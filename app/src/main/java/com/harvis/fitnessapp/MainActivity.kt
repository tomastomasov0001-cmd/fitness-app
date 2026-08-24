package com.harvis.fitnessapp

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.harvis.fitnessapp.databinding.ActivityMainBinding
import com.harvis.fitnessapp.util.LanguageHelper
import com.harvis.fitnessapp.util.PremiumDialogHelper
import com.harvis.fitnessapp.util.PremiumManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageHelper.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply saved language
        LanguageHelper.applyLanguage(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        // Pri kliknuti na polozku v dolni navigaci vzdy prejit na hlavni fragment
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            // Premium check for History tab
            if (item.itemId == R.id.historyFragment && !PremiumManager.canAccessHistory(this)) {
                PremiumDialogHelper.showHistoryLockedDialog(this)
                return@setOnItemSelectedListener false
            }

            // Premium check for Statistics tab
            if (item.itemId == R.id.statisticsFragment && !PremiumManager.canAccessStatistics(this)) {
                PremiumDialogHelper.showStatisticsLockedDialog(this)
                return@setOnItemSelectedListener false
            }

            val navOptions = NavOptions.Builder()
                .setPopUpTo(navController.graph.startDestinationId, false)
                .setLaunchSingleTop(true)
                .build()

            navController.navigate(item.itemId, null, navOptions)
            true
        }

        // Reselect - kdyz kliknu na aktualni tab, vrati se na zacatek
        binding.bottomNavigation.setOnItemReselectedListener { item ->
            navController.popBackStack(item.itemId, false)
        }
    }
}
