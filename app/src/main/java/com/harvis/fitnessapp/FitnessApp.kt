package com.harvis.fitnessapp

import android.app.Application
import com.harvis.fitnessapp.data.FitnessDatabase

class FitnessApp : Application() {

    val database: FitnessDatabase by lazy {
        FitnessDatabase.getDatabase(this)
    }
}
