package com.harvis.fitnessapp.ui.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.harvis.fitnessapp.FitnessApp
import com.harvis.fitnessapp.data.WorkoutLog
import com.harvis.fitnessapp.data.WorkoutVariant
import kotlinx.coroutines.launch
import java.util.*

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = (application as FitnessApp).database.fitnessDao()

    val allVariants: LiveData<List<WorkoutVariant>> = dao.getAllVariants()

    private val _workoutsForDate = MutableLiveData<List<WorkoutLog>>()
    val workoutsForDate: LiveData<List<WorkoutLog>> = _workoutsForDate

    init {
        loadWorkoutsForDate(System.currentTimeMillis())
    }

    fun loadWorkoutsForDate(dateMillis: Long) {
        viewModelScope.launch {
            // Normalizovat datum na zacatek dne
            val calendar = Calendar.getInstance().apply {
                timeInMillis = dateMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calendar.timeInMillis
            val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1

            val logs = dao.getWorkoutLogsBetweenSync(startOfDay, endOfDay)
            _workoutsForDate.value = logs.take(4) // Max 4 treninky na den
        }
    }

    fun assignWorkoutToDate(variantId: Long, dateMillis: Long) {
        viewModelScope.launch {
            // Normalizovat datum na zacatek dne
            val calendar = Calendar.getInstance().apply {
                timeInMillis = dateMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val normalizedDate = calendar.timeInMillis

            // Zkontrolovat, zda uz neni 4 treninku
            val existingLogs = dao.getWorkoutLogsBetweenSync(normalizedDate, normalizedDate + 24 * 60 * 60 * 1000 - 1)
            if (existingLogs.size >= 4) {
                return@launch // Max 4 treninky na den
            }

            val log = WorkoutLog(
                variantId = variantId,
                date = normalizedDate,
                completed = false
            )
            dao.insertWorkoutLog(log)
            loadWorkoutsForDate(normalizedDate)
        }
    }

    fun getVariantNameWithNumber(variantId: Long): LiveData<String> {
        val result = MutableLiveData<String>()
        viewModelScope.launch {
            val variants = dao.getAllVariantsSync()
            val index = variants.indexOfFirst { it.id == variantId }
            val variant = variants.find { it.id == variantId }
            if (variant != null && index >= 0) {
                result.value = "${index + 1}. ${variant.name}"
            } else {
                result.value = variant?.name ?: "Neznámý"
            }
        }
        return result
    }

    fun getWorkoutLogsForMonth(startOfMonth: Long, endOfMonth: Long): LiveData<List<WorkoutLog>> {
        return dao.getWorkoutLogsBetween(startOfMonth, endOfMonth)
    }

    fun deleteWorkoutLog(workoutLogId: Long, dateMillis: Long) {
        viewModelScope.launch {
            dao.deleteWorkoutLogById(workoutLogId)
            loadWorkoutsForDate(dateMillis)
        }
    }
}
