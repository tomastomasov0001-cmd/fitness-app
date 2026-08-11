package com.harvis.fitnessapp.ui.exercises

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.harvis.fitnessapp.FitnessApp
import com.harvis.fitnessapp.data.Exercise
import kotlinx.coroutines.launch

class ExercisesViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = (application as FitnessApp).database.fitnessDao()

    val allExercises: LiveData<List<Exercise>> = dao.getAllExercises()

    fun insertExercise(exercise: Exercise) {
        viewModelScope.launch {
            dao.insertExercise(exercise)
        }
    }

    fun updateExercise(exercise: Exercise) {
        viewModelScope.launch {
            dao.updateExercise(exercise)
        }
    }

    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch {
            dao.deleteExercise(exercise)
        }
    }

    suspend fun getVariantCountForExercise(exerciseId: Long): Int {
        return dao.getVariantCountForExercise(exerciseId)
    }
}
