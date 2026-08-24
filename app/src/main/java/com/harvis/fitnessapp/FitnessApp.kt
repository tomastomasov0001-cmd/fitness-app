package com.harvis.fitnessapp

import android.app.Application
import android.widget.Toast
import com.harvis.fitnessapp.billing.BillingManager
import com.harvis.fitnessapp.data.FitnessDatabase
import com.harvis.fitnessapp.util.PremiumManager

class FitnessApp : Application() {

    val database: FitnessDatabase by lazy {
        FitnessDatabase.getDatabase(this)
    }

    val billingManager: BillingManager by lazy {
        BillingManager(this)
    }

    override fun onCreate() {
        super.onCreate()

        try {
            billingManager.initialize()
        } catch (e: Exception) {
            android.util.Log.e("FitnessApp", "Billing init failed: ${e.message}")
        }
    }
}
