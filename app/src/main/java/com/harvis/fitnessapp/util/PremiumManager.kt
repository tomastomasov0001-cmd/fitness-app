package com.harvis.fitnessapp.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Singleton pro správu premium stavu aplikace.
 * Free verze má omezení na počet variant a cviků.
 *
 * Premium může být aktivováno:
 * 1. Nákupem (trvalé)
 * 2. Promo kódem (7 dní, kód lze použít pouze jednou)
 */
object PremiumManager {

    private const val PREFS_NAME = "premium_prefs"
    private const val KEY_IS_PREMIUM_PURCHASED = "is_premium_purchased"  // Trvalý nákup
    private const val KEY_PROMO_ACTIVATION_TIME = "promo_activation_time"  // Kdy byl promo kód aktivován
    private const val KEY_USED_PROMO_CODES = "used_promo_codes"  // Seznam použitých kódů
    private const val KEY_ACTIVE_PROMO_CODE = "active_promo_code"  // Aktuálně aktivní kód

    // Limity pro free verzi
    const val FREE_MAX_VARIANTS = 1
    const val FREE_MAX_EXERCISES = 2

    // Doba platnosti promo kódu v milisekundách (7 dní)
    private const val PROMO_DURATION_MS = 7L * 24 * 60 * 60 * 1000

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Zkontroluje, zda má uživatel premium verzi (zakoupenou nebo z promo kódu).
     */
    fun isPremium(context: Context): Boolean {
        // Nejprve zkontroluj trvalý nákup
        if (isPremiumPurchased(context)) {
            return true
        }
        // Pak zkontroluj platnost promo kódu
        return isPromoActive(context)
    }

    /**
     * Vrátí true, pokud má uživatel zakoupený trvalý premium.
     */
    fun isPremiumPurchased(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_PREMIUM_PURCHASED, false)
    }

    /**
     * Vrátí true, pokud je promo kód stále aktivní (méně než 7 dní od aktivace).
     */
    fun isPromoActive(context: Context): Boolean {
        val activationTime = getPrefs(context).getLong(KEY_PROMO_ACTIVATION_TIME, 0)
        if (activationTime == 0L) return false

        val now = System.currentTimeMillis()
        val expirationTime = activationTime + PROMO_DURATION_MS
        return now < expirationTime
    }

    /**
     * Vrátí počet zbývajících dní promo období, nebo -1 pokud není aktivní.
     */
    fun getPromoRemainingDays(context: Context): Int {
        val activationTime = getPrefs(context).getLong(KEY_PROMO_ACTIVATION_TIME, 0)
        if (activationTime == 0L) return -1

        val now = System.currentTimeMillis()
        val expirationTime = activationTime + PROMO_DURATION_MS
        val remainingMs = expirationTime - now

        if (remainingMs <= 0) return 0

        // Zaokrouhlení nahoru (i 1ms = 1 den)
        return ((remainingMs + (24 * 60 * 60 * 1000 - 1)) / (24 * 60 * 60 * 1000)).toInt()
    }

    /**
     * Vrátí typ premium (pro zobrazení v UI).
     */
    fun getPremiumType(context: Context): PremiumType {
        if (isPremiumPurchased(context)) return PremiumType.PURCHASED
        if (isPromoActive(context)) return PremiumType.PROMO
        return PremiumType.NONE
    }

    /**
     * Nastaví trvalý premium stav (volá se po úspěšném nákupu).
     */
    fun setPremiumPurchased(context: Context, isPurchased: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_PREMIUM_PURCHASED, isPurchased).apply()
    }

    /**
     * Zkontroluje, zda lze přidat další variantu.
     */
    fun canAddVariant(context: Context, currentCount: Int): Boolean {
        return isPremium(context) || currentCount < FREE_MAX_VARIANTS
    }

    /**
     * Zkontroluje, zda lze přidat další cvik do varianty.
     */
    fun canAddExercise(context: Context, currentCount: Int): Boolean {
        return isPremium(context) || currentCount < FREE_MAX_EXERCISES
    }

    /**
     * Zkontroluje, zda má uživatel přístup k historii.
     */
    fun canAccessHistory(context: Context): Boolean {
        return isPremium(context)
    }

    /**
     * Zkontroluje, zda má uživatel přístup ke statistikám.
     */
    fun canAccessStatistics(context: Context): Boolean {
        return isPremium(context)
    }

    // ========== PROMO KÓDY ==========

    // Běžné promo kódy - platnost 7 dní
    private val validPromoCodes = setOf(
        "FITNESS2024",
        "BETAUSER",
        "REVIEWER",
        "TESTWEEK"
    )

    // Speciální kódy pro testery - TRVALÝ premium
    private val permanentPromoCodes = setOf(
        "TESTER2026"
    )

    // Prefix pro unikátní VIP kódy (formát: VIP-XXXX kde XXXX je 4-místné číslo)
    private const val VIP_CODE_PREFIX = "VIP-"

    /**
     * Ověří, zda je VIP kód platný.
     * Formát: VIP-XXXX kde poslední číslice je kontrolní součet.
     * Např. VIP-1234 je platný pokud (1+2+3) % 10 == 4
     */
    private fun isValidVipCode(code: String): Boolean {
        if (!code.startsWith(VIP_CODE_PREFIX)) return false
        val numberPart = code.removePrefix(VIP_CODE_PREFIX)
        if (numberPart.length != 4 || !numberPart.all { it.isDigit() }) return false

        val digits = numberPart.map { it.digitToInt() }
        val checksum = (digits[0] + digits[1] + digits[2]) % 10
        return checksum == digits[3]
    }

    /**
     * Generuje platný VIP kód.
     * Volej tuto funkci pro získání nového kódu pro přítele.
     */
    fun generateVipCode(): String {
        val random = java.util.Random()
        val d1 = random.nextInt(10)
        val d2 = random.nextInt(10)
        val d3 = random.nextInt(10)
        val checksum = (d1 + d2 + d3) % 10
        return "$VIP_CODE_PREFIX$d1$d2$d3$checksum"
    }

    /**
     * Zkusí uplatnit promo kód.
     * Vrací PromoResult s informací o úspěchu/chybě.
     */
    fun redeemPromoCode(context: Context, code: String): PromoResult {
        val normalizedCode = code.trim().uppercase()

        // Zkontroluj speciální kódy pro testery (trvalý premium)
        if (permanentPromoCodes.contains(normalizedCode)) {
            // Zkontroluj, zda kód nebyl již použit
            val usedCodes = getUsedPromoCodes(context)
            if (usedCodes.contains(normalizedCode)) {
                return PromoResult.ALREADY_USED
            }

            // Aktivuj TRVALÝ premium
            getPrefs(context).edit()
                .putBoolean(KEY_IS_PREMIUM_PURCHASED, true)
                .putStringSet(KEY_USED_PROMO_CODES, usedCodes + normalizedCode)
                .apply()

            return PromoResult.SUCCESS_PERMANENT
        }

        // Zkontroluj VIP kódy (formát VIP-XXXX, trvalý premium)
        if (isValidVipCode(normalizedCode)) {
            val usedCodes = getUsedPromoCodes(context)
            if (usedCodes.contains(normalizedCode)) {
                return PromoResult.ALREADY_USED
            }

            // Aktivuj TRVALÝ premium
            getPrefs(context).edit()
                .putBoolean(KEY_IS_PREMIUM_PURCHASED, true)
                .putStringSet(KEY_USED_PROMO_CODES, usedCodes + normalizedCode)
                .apply()

            return PromoResult.SUCCESS_PERMANENT
        }

        // Zkontroluj, zda je kód platný (běžné kódy)
        if (!validPromoCodes.contains(normalizedCode)) {
            return PromoResult.INVALID_CODE
        }

        // Zkontroluj, zda kód nebyl již použit
        val usedCodes = getUsedPromoCodes(context)
        if (usedCodes.contains(normalizedCode)) {
            return PromoResult.ALREADY_USED
        }

        // Zkontroluj, zda uživatel již nemá aktivní promo
        if (isPromoActive(context)) {
            return PromoResult.ALREADY_ACTIVE
        }

        // Aktivuj promo kód (7 dní)
        val now = System.currentTimeMillis()
        getPrefs(context).edit()
            .putLong(KEY_PROMO_ACTIVATION_TIME, now)
            .putString(KEY_ACTIVE_PROMO_CODE, normalizedCode)
            .putStringSet(KEY_USED_PROMO_CODES, usedCodes + normalizedCode)
            .apply()

        return PromoResult.SUCCESS
    }

    /**
     * Vrátí seznam všech použitých promo kódů.
     */
    private fun getUsedPromoCodes(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_USED_PROMO_CODES, emptySet()) ?: emptySet()
    }

    /**
     * Vrátí aktuálně aktivní promo kód, nebo null.
     */
    fun getActivePromoCode(context: Context): String? {
        if (!isPromoActive(context)) return null
        return getPrefs(context).getString(KEY_ACTIVE_PROMO_CODE, null)
    }

    // ========== DEBUG FUNKCE ==========

    fun debugTogglePremium(context: Context): Boolean {
        val newState = !isPremiumPurchased(context)
        setPremiumPurchased(context, newState)
        return newState
    }

    fun debugResetPremium(context: Context) {
        setPremiumPurchased(context, false)
        // Také resetovat promo
        getPrefs(context).edit()
            .putLong(KEY_PROMO_ACTIVATION_TIME, 0)
            .putString(KEY_ACTIVE_PROMO_CODE, null)
            .apply()
    }

    fun debugResetAllPromoCodes(context: Context) {
        getPrefs(context).edit()
            .putStringSet(KEY_USED_PROMO_CODES, emptySet())
            .putLong(KEY_PROMO_ACTIVATION_TIME, 0)
            .putString(KEY_ACTIVE_PROMO_CODE, null)
            .apply()
    }

    // ========== ENUM TYPY ==========

    enum class PremiumType {
        NONE,       // Žádný premium
        PROMO,      // Promo kód (7 dní)
        PURCHASED   // Zakoupený (trvalý)
    }

    enum class PromoResult {
        SUCCESS,            // Kód úspěšně aktivován (7 dní)
        SUCCESS_PERMANENT,  // Kód úspěšně aktivován (trvalý premium)
        INVALID_CODE,       // Neplatný kód
        ALREADY_USED,       // Kód byl již použit
        ALREADY_ACTIVE      // Již máte aktivní promo
    }
}
