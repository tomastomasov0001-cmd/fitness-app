package com.harvis.fitnessapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.harvis.fitnessapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        // Pri kliknuti na polozku v dolni navigaci vzdy prejit na hlavni fragment
        binding.bottomNavigation.setOnItemSelectedListener { item ->
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
