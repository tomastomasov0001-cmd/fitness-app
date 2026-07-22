package com.harvis.fitnessapp.ui.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.harvis.fitnessapp.FitnessApp
import com.harvis.fitnessapp.data.Exercise
import com.harvis.fitnessapp.data.ExerciseSetWithDate
import kotlinx.coroutines.launch

data class ProgressDataPoint(
    val date: Long,
    val maxWeight: Float,
    val maxReps: Int,
    val totalVolume: Float  // vaha x opakovani
)

class ExerciseStatsViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = (application as FitnessApp).database.fitnessDao()

    private val _exercise = MutableLiveData<Exercise?>()
    val exercise: LiveData<Exercise?> = _exercise

    private val _progressData = MutableLiveData<List<ProgressDataPoint>>()
    val progressData: LiveData<List<ProgressDataPoint>> = _progressData

    private val _allTimeBest = MutableLiveData<Pair<Float?, Int?>>()
    val allTimeBest: LiveData<Pair<Float?, Int?>> = _allTimeBest

    private val _lastWorkoutData = MutableLiveData<List<ExerciseSetWithDate>>()
    val lastWorkoutData: LiveData<List<ExerciseSetWithDate>> = _lastWorkoutData

    fun loadExerciseStats(exerciseId: Long) {
        viewModelScope.launch {
            // Nacist cvik
            val exercise = dao.getExerciseById(exerciseId)
            _exercise.value = exercise

            // Nacist vsechny serie pro tento cvik
            val allSets = dao.getAllSetsForExercise(exerciseId)

            // Seskupit podle data a spocitat max hodnoty
            val setsByDate = allSets.groupBy { it.workoutDate }
            val progressPoints = setsByDate.map { (date, sets) ->
                ProgressDataPoint(
                    date = date,
                    maxWeight = sets.maxOfOrNull { it.weight } ?: 0f,
                    maxReps = sets.maxOfOrNull { it.reps } ?: 0,
                    totalVolume = sets.sumOf { (it.weight * it.reps).toDouble() }.toFloat()
                )
            }.sortedBy { it.date }

            _progressData.value = progressPoints

            // All-time best
            val maxWeight = dao.getMaxWeightForExercise(exerciseId)
            val maxReps = dao.getMaxRepsForExercise(exerciseId)
            _allTimeBest.value = Pair(maxWeight, maxReps)

            // Posledni trenink
            val lastWorkout = setsByDate.maxByOrNull { it.key }
            _lastWorkoutData.value = lastWorkout?.value ?: emptyList()
        }
    }
}
