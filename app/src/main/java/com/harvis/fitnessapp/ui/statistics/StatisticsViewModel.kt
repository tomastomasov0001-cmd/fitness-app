package com.harvis.fitnessapp.ui.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.harvis.fitnessapp.FitnessApp
import com.harvis.fitnessapp.data.*
import kotlinx.coroutines.launch
import java.util.*

data class DashboardStats(
    val totalWorkouts: Int = 0,
    val trainingDaysThisMonth: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0
)

data class WeeklyStats(
    val workoutsThisWeek: Int = 0,
    val totalVolume: Float = 0f,  // vaha x opakovani
    val totalSets: Int = 0
)

data class MonthlyStats(
    val workoutsThisMonth: Int = 0,
    val workoutsLastMonth: Int = 0,
    val change: Int = 0  // rozdil oproti minulemu mesici
)

data class ExerciseRecord(
    val exercise: Exercise,
    val maxWeight: Float?,
    val maxReps: Int?
)

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = (application as FitnessApp).database.fitnessDao()

    private val _dashboardStats = MutableLiveData<DashboardStats>()
    val dashboardStats: LiveData<DashboardStats> = _dashboardStats

    private val _weeklyStats = MutableLiveData<WeeklyStats>()
    val weeklyStats: LiveData<WeeklyStats> = _weeklyStats

    private val _monthlyStats = MutableLiveData<MonthlyStats>()
    val monthlyStats: LiveData<MonthlyStats> = _monthlyStats

    private val _variantDistribution = MutableLiveData<List<VariantWorkoutCount>>()
    val variantDistribution: LiveData<List<VariantWorkoutCount>> = _variantDistribution

    private val _workoutFrequency = MutableLiveData<List<DateWorkoutCount>>()
    val workoutFrequency: LiveData<List<DateWorkoutCount>> = _workoutFrequency

    private val _heatmapData = MutableLiveData<List<DateWorkoutCount>>()
    val heatmapData: LiveData<List<DateWorkoutCount>> = _heatmapData

    private val _exerciseRecords = MutableLiveData<List<ExerciseRecord>>()
    val exerciseRecords: LiveData<List<ExerciseRecord>> = _exerciseRecords

    private val _exercisesWithData = MutableLiveData<List<Exercise>>()
    val exercisesWithData: LiveData<List<Exercise>> = _exercisesWithData

    init {
        loadAllStatistics()
    }

    fun loadAllStatistics() {
        viewModelScope.launch {
            try {
                loadDashboardStats()
                loadWeeklyStats()
                loadMonthlyStats()
                loadVariantDistribution()
                loadWorkoutFrequency()
                loadHeatmapData()
                loadExerciseRecords()
                loadExercisesWithData()
            } catch (e: Exception) {
                android.util.Log.e("StatisticsViewModel", "Error loading stats: ${e.message}", e)
            }
        }
    }

    private suspend fun loadDashboardStats() {
        val totalWorkouts = dao.getTotalCompletedWorkouts()

        // Tento mesic
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endOfMonth = calendar.timeInMillis

        val trainingDaysThisMonth = dao.getTrainingDaysInMonth(startOfMonth, endOfMonth)

        // Vypocet serie (po sobe jdoucich dni)
        val allWorkouts = dao.getAllCompletedWorkoutsSync()
        val (currentStreak, bestStreak) = calculateStreaks(allWorkouts)

        _dashboardStats.value = DashboardStats(
            totalWorkouts = totalWorkouts,
            trainingDaysThisMonth = trainingDaysThisMonth,
            currentStreak = currentStreak,
            bestStreak = bestStreak
        )
    }

    private fun calculateStreaks(workouts: List<WorkoutLog>): Pair<Int, Int> {
        if (workouts.isEmpty()) return Pair(0, 0)

        // Ziskat unikatni data treninku
        val workoutDates = workouts.map { normalizeDate(it.date) }.distinct().sorted()
        if (workoutDates.isEmpty()) return Pair(0, 0)

        var currentStreak = 0
        var bestStreak = 0
        var tempStreak = 1

        val today = normalizeDate(System.currentTimeMillis())
        val yesterday = today - 24 * 60 * 60 * 1000

        // Kontrola aktualni serie (vcera nebo dnes)
        val lastWorkoutDate = workoutDates.last()
        val isCurrentStreakActive = lastWorkoutDate == today || lastWorkoutDate == yesterday

        // Vypocet nejdelsi serie
        for (i in 1 until workoutDates.size) {
            val diff = workoutDates[i] - workoutDates[i - 1]
            if (diff == 24 * 60 * 60 * 1000L) {
                tempStreak++
            } else {
                if (tempStreak > bestStreak) bestStreak = tempStreak
                tempStreak = 1
            }
        }
        if (tempStreak > bestStreak) bestStreak = tempStreak

        // Aktualni serie
        if (isCurrentStreakActive) {
            tempStreak = 1
            for (i in workoutDates.size - 1 downTo 1) {
                val diff = workoutDates[i] - workoutDates[i - 1]
                if (diff == 24 * 60 * 60 * 1000L) {
                    tempStreak++
                } else {
                    break
                }
            }
            currentStreak = tempStreak
        }

        return Pair(currentStreak, bestStreak)
    }

    private fun normalizeDate(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private suspend fun loadWeeklyStats() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfWeek = calendar.timeInMillis

        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endOfWeek = calendar.timeInMillis

        val workouts = dao.getWorkoutLogsBetweenSync(startOfWeek, endOfWeek)
            .filter { it.completed }

        var totalVolume = 0f
        var totalSets = 0

        for (workout in workouts) {
            val sets = dao.getSetsForWorkoutSync(workout.id)
            for (set in sets) {
                totalVolume += set.weight * set.reps
                totalSets++
            }
        }

        _weeklyStats.value = WeeklyStats(
            workoutsThisWeek = workouts.size,
            totalVolume = totalVolume,
            totalSets = totalSets
        )
    }

    private suspend fun loadMonthlyStats() {
        val calendar = Calendar.getInstance()

        // Tento mesic
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfThisMonth = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endOfThisMonth = calendar.timeInMillis

        // Minuly mesic
        calendar.add(Calendar.MILLISECOND, 1)
        calendar.add(Calendar.MONTH, -2)
        val startOfLastMonth = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endOfLastMonth = calendar.timeInMillis

        val workoutsThisMonth = dao.getWorkoutLogsBetweenSync(startOfThisMonth, endOfThisMonth)
            .filter { it.completed }.size
        val workoutsLastMonth = dao.getWorkoutLogsBetweenSync(startOfLastMonth, endOfLastMonth)
            .filter { it.completed }.size

        _monthlyStats.value = MonthlyStats(
            workoutsThisMonth = workoutsThisMonth,
            workoutsLastMonth = workoutsLastMonth,
            change = workoutsThisMonth - workoutsLastMonth
        )
    }

    private suspend fun loadVariantDistribution() {
        _variantDistribution.value = dao.getWorkoutCountsByVariant()
    }

    private suspend fun loadWorkoutFrequency() {
        // Posledni 4 tydny
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.WEEK_OF_YEAR, -4)
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis

        val endDate = System.currentTimeMillis()

        _workoutFrequency.value = dao.getWorkoutCountsByDate(startDate, endDate)
    }

    private suspend fun loadHeatmapData() {
        _heatmapData.value = dao.getAllWorkoutDates()
    }

    private suspend fun loadExerciseRecords() {
        val exercises = dao.getExercisesWithData()
        val records = exercises.map { exercise ->
            ExerciseRecord(
                exercise = exercise,
                maxWeight = dao.getMaxWeightForExercise(exercise.id),
                maxReps = dao.getMaxRepsForExercise(exercise.id)
            )
        }
        _exerciseRecords.value = records
    }

    private suspend fun loadExercisesWithData() {
        _exercisesWithData.value = dao.getExercisesWithData()
    }
}
