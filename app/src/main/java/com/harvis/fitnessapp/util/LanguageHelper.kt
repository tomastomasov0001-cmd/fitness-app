package com.harvis.fitnessapp.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LanguageHelper {

    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LANGUAGE = "selected_language"
    const val SYSTEM_DEFAULT = "system"

    data class Language(
        val code: String,
        val displayName: String,
        val nativeName: String
    )

    val availableLanguages = listOf(
        Language(SYSTEM_DEFAULT, "System", "System"),
        Language("en", "English", "English"),
        Language("cs", "Czech", "Čeština"),
        Language("sk", "Slovak", "Slovenčina"),
        Language("pl", "Polish", "Polski"),
        Language("de", "German", "Deutsch"),
        Language("es", "Spanish", "Español"),
        Language("fr", "French", "Français"),
        Language("zh", "Chinese", "中文")
    )

    fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, SYSTEM_DEFAULT) ?: SYSTEM_DEFAULT
    }

    fun saveLanguage(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }

    fun applyLanguage(context: Context, languageCode: String = getSavedLanguage(context)) {
        if (languageCode == SYSTEM_DEFAULT) {
            // Use system default
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        } else {
            // Set specific locale
            val localeList = LocaleListCompat.forLanguageTags(languageCode)
            AppCompatDelegate.setApplicationLocales(localeList)
        }
    }

    fun getCurrentLanguageCode(context: Context): String {
        val saved = getSavedLanguage(context)
        if (saved != SYSTEM_DEFAULT) {
            return saved
        }
        // Return system language
        return Locale.getDefault().language
    }

    fun getLanguageDisplayName(code: String): String {
        return availableLanguages.find { it.code == code }?.nativeName ?: code
    }

    fun wrapContext(context: Context): Context {
        val languageCode = getSavedLanguage(context)
        if (languageCode == SYSTEM_DEFAULT) {
            return context
        }

        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }
}
