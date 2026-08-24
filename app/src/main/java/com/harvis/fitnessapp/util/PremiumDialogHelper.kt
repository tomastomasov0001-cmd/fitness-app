package com.harvis.fitnessapp.util

import android.app.Activity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.harvis.fitnessapp.FitnessApp
import com.harvis.fitnessapp.R

/**
 * Helper pro zobrazení dialogů souvisejících s Premium verzí.
 */
object PremiumDialogHelper {

    fun showUpgradeDialog(
        activity: Activity,
        title: String,
        message: String
    ) {
        val dialogView = LayoutInflater.from(activity)
            .inflate(R.layout.dialog_upgrade_premium, null)

        val descriptionView = dialogView.findViewById<TextView>(R.id.premiumDescription)
        val priceView = dialogView.findViewById<TextView>(R.id.premiumPrice)

        descriptionView.text = message

        val billingManager = (activity.application as FitnessApp).billingManager
        val price = billingManager.getFormattedPrice()
        priceView.text = price ?: ""

        MaterialAlertDialogBuilder(activity)
            .setView(dialogView)
            .setPositiveButton(R.string.upgrade) { _, _ ->
                billingManager.launchPurchaseFlow(activity)
            }
            .setNeutralButton(R.string.have_promo_code) { _, _ ->
                showPromoCodeDialog(activity)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    fun showPromoCodeDialog(activity: Activity) {
        val inputLayout = TextInputLayout(activity).apply {
            hint = activity.getString(R.string.promo_code_hint)
            setPadding(48, 16, 48, 0)
        }
        val input = TextInputEditText(activity)
        inputLayout.addView(input)

        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.enter_promo_code)
            .setView(inputLayout)
            .setPositiveButton(R.string.activate) { _, _ ->
                val code = input.text.toString()
                val result = PremiumManager.redeemPromoCode(activity, code)

                val messageResId = when (result) {
                    PremiumManager.PromoResult.SUCCESS -> R.string.promo_code_success
                    PremiumManager.PromoResult.SUCCESS_PERMANENT -> R.string.promo_code_success_permanent
                    PremiumManager.PromoResult.INVALID_CODE -> R.string.promo_code_invalid
                    PremiumManager.PromoResult.ALREADY_USED -> R.string.promo_code_already_used
                    PremiumManager.PromoResult.ALREADY_ACTIVE -> R.string.promo_code_already_active
                }

                val isSuccess = result == PremiumManager.PromoResult.SUCCESS ||
                               result == PremiumManager.PromoResult.SUCCESS_PERMANENT
                val duration = if (isSuccess) Toast.LENGTH_LONG else Toast.LENGTH_SHORT

                Toast.makeText(activity, messageResId, duration).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    fun showVariantLimitDialog(activity: Activity) {
        showUpgradeDialog(
            activity = activity,
            title = activity.getString(R.string.variant_limit_title),
            message = activity.getString(R.string.variant_limit_message)
        )
    }

    fun showExerciseLimitDialog(activity: Activity) {
        showUpgradeDialog(
            activity = activity,
            title = activity.getString(R.string.exercise_limit_title),
            message = activity.getString(R.string.exercise_limit_message)
        )
    }

    fun showHistoryLockedDialog(activity: Activity) {
        showUpgradeDialog(
            activity = activity,
            title = activity.getString(R.string.premium_required),
            message = activity.getString(R.string.history_premium_required)
        )
    }

    fun showStatisticsLockedDialog(activity: Activity) {
        showUpgradeDialog(
            activity = activity,
            title = activity.getString(R.string.premium_required),
            message = activity.getString(R.string.statistics_premium_required)
        )
    }
}
