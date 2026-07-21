package com.harvis.fitnessapp.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.harvis.fitnessapp.FitnessApp
import com.harvis.fitnessapp.data.WorkoutLogWithDetails

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = (application as FitnessApp).database.fitnessDao()

    val allWorkoutLogs: LiveData<List<WorkoutLogWithDetails>> = dao.getAllWorkoutLogsWithDetails()
}
