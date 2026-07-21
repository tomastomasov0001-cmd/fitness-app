package com.harvis.fitnessapp.ui.workout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.harvis.fitnessapp.FitnessApp
import com.harvis.fitnessapp.data.Exercise
import com.harvis.fitnessapp.data.ExerciseSet
import com.harvis.fitnessapp.data.WorkoutLog
import com.harvis.fitnessapp.data.WorkoutVariant
import kotlinx.coroutines.launch
import java.util.*

data class ActiveExercise(
    val exercise: Exercise,
    val sets: MutableList<SetData> = mutableListOf()
)

data class SetData(
    var reps: Int = 0,
    var weight: Float = 0f,
    var timeSeconds: Int = 0,  // cas v sekundach
    var completed: Boolean = false
)

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = (application as FitnessApp).database.fitnessDao()

    val allVariants: LiveData<List<WorkoutVariant>> = dao.getAllVariants()

    private val _isWorkoutActive = MutableLiveData(false)
    val isWorkoutActive: LiveData<Boolean> = _isWorkoutActive

    private val _currentVariant = MutableLiveData<WorkoutVariant?>()
    val currentVariant: LiveData<WorkoutVariant?> = _currentVariant

    private val _exercises = MutableLiveData<List<ActiveExercise>>()
    val exercises: LiveData<List<ActiveExercise>> = _exercises

    private val _currentExerciseIndex = MutableLiveData(0)
    val currentExerciseIndex: LiveData<Int> = _currentExerciseIndex

    private val _isViewOnly = MutableLiveData(false)
    val isViewOnly: LiveData<Boolean> = _isViewOnly

    private val _isEditingCompleted = MutableLiveData(false)
    val isEditingCompleted: LiveData<Boolean> = _isEditingCompleted

    private var workoutLogId: Long = 0

    fun startWorkout(variant: WorkoutVariant) {
        viewModelScope.launch {
            // Normalizovat datum na zacatek dne
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayNormalized = calendar.timeInMillis

            // Vytvorit log treninku
            val log = WorkoutLog(
                variantId = variant.id,
                date = todayNormalized
            )
            workoutLogId = dao.insertWorkoutLog(log)

            // Nacist cviky
            val exerciseList = dao.getExercisesForVariantSync(variant.id)
            val activeExercises = exerciseList.map { exercise ->
                ActiveExercise(
                    exercise = exercise,
                    sets = if (exercise.hasSets) {
                        MutableList(exercise.defaultSets) { SetData() }
                    } else {
                        mutableListOf(SetData()) // 1 zaznam pro cviky bez serii
                    }
                )
            }

            _currentVariant.value = variant
            _exercises.value = activeExercises
            _currentExerciseIndex.value = 0
            _isViewOnly.value = false
            _isEditingCompleted.value = false
            _isWorkoutActive.value = true
        }
    }

    fun startWorkoutById(variantId: Long, existingLogId: Long = 0) {
        viewModelScope.launch {
            val variant = dao.getVariantById(variantId) ?: return@launch

            // Pouzit existujici log nebo vytvorit novy
            if (existingLogId > 0) {
                workoutLogId = existingLogId
            } else {
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val todayNormalized = calendar.timeInMillis
                val log = WorkoutLog(
                    variantId = variant.id,
                    date = todayNormalized
                )
                workoutLogId = dao.insertWorkoutLog(log)
            }

            // Nacist cviky
            val exerciseList = dao.getExercisesForVariantSync(variant.id)
            val activeExercises = exerciseList.map { exercise ->
                ActiveExercise(
                    exercise = exercise,
                    sets = if (exercise.hasSets) {
                        MutableList(exercise.defaultSets) { SetData() }
                    } else {
                        mutableListOf(SetData())
                    }
                )
            }

            _currentVariant.value = variant
            _exercises.value = activeExercises
            _currentExerciseIndex.value = 0
            _isViewOnly.value = false
            _isEditingCompleted.value = false
            _isWorkoutActive.value = true
        }
    }

    fun loadCompletedWorkout(variantId: Long, workoutLogId: Long) {
        viewModelScope.launch {
            val variant = dao.getVariantById(variantId) ?: return@launch
            this@WorkoutViewModel.workoutLogId = workoutLogId

            // Nacist cviky varianty
            val exerciseList = dao.getExercisesForVariantSync(variant.id)

            // Nacist ulozene serie z databaze
            val savedSets = dao.getSetsForWorkoutSync(workoutLogId)

            // Seskupit serie podle cviku
            val setsByExercise = savedSets.groupBy { it.exerciseId }

            val activeExercises = exerciseList.map { exercise ->
                val exerciseSets = setsByExercise[exercise.id] ?: emptyList()
                ActiveExercise(
                    exercise = exercise,
                    sets = if (exerciseSets.isNotEmpty()) {
                        exerciseSets.sortedBy { it.setNumber }.map { set ->
                            SetData(
                                reps = set.reps,
                                weight = set.weight,
                                timeSeconds = set.timeSeconds,
                                completed = set.completed
                            )
                        }.toMutableList()
                    } else {
                        mutableListOf(SetData(completed = true)) // Prazdna serie
                    }
                )
            }

            _currentVariant.value = variant
            _exercises.value = activeExercises
            _currentExerciseIndex.value = 0
            _isViewOnly.value = true
            _isEditingCompleted.value = false
            _isWorkoutActive.value = true
        }
    }

    fun enableEditing() {
        _isViewOnly.value = false
        _isEditingCompleted.value = true
    }

    fun saveCompletedWorkout() {
        viewModelScope.launch {
            // Smazat stare serie
            dao.deleteSetsForWorkout(workoutLogId)

            // Ulozit aktualni serie
            val exerciseList = _exercises.value ?: return@launch

            exerciseList.forEach { activeExercise ->
                activeExercise.sets.forEachIndexed { index, setData ->
                    if (setData.reps > 0 || setData.weight > 0 || setData.timeSeconds > 0) {
                        val exerciseSet = ExerciseSet(
                            workoutLogId = workoutLogId,
                            exerciseId = activeExercise.exercise.id,
                            setNumber = index + 1,
                            reps = setData.reps,
                            weight = setData.weight,
                            timeSeconds = setData.timeSeconds,
                            completed = true
                        )
                        dao.insertExerciseSet(exerciseSet)
                    }
                }
            }

            // Reset stavu
            _isWorkoutActive.value = false
            _currentVariant.value = null
            _exercises.value = emptyList()
            _currentExerciseIndex.value = 0
            _isViewOnly.value = false
            _isEditingCompleted.value = false
            workoutLogId = 0
        }
    }

    fun addSet(exerciseIndex: Int) {
        val currentExercises = _exercises.value?.toMutableList() ?: return
        if (exerciseIndex < currentExercises.size) {
            currentExercises[exerciseIndex].sets.add(SetData())
            _exercises.value = currentExercises
        }
    }

    fun removeSet(exerciseIndex: Int, setIndex: Int) {
        val currentExercises = _exercises.value?.toMutableList() ?: return
        if (exerciseIndex < currentExercises.size) {
            val exercise = currentExercises[exerciseIndex]
            if (setIndex < exercise.sets.size && exercise.sets.size > 1) {
                exercise.sets.removeAt(setIndex)
                _exercises.value = currentExercises
            }
        }
    }

    fun updateSet(exerciseIndex: Int, setIndex: Int, reps: Int, weight: Float) {
        val currentExercises = _exercises.value ?: return
        if (exerciseIndex < currentExercises.size) {
            val exercise = currentExercises[exerciseIndex]
            if (setIndex < exercise.sets.size) {
                exercise.sets[setIndex].reps = reps
                exercise.sets[setIndex].weight = weight
                // Neaktualizovat LiveData - data jsou mutable a aktualizuji se primo
            }
        }
    }

    fun updateSetTime(exerciseIndex: Int, setIndex: Int, timeSeconds: Int) {
        val currentExercises = _exercises.value ?: return
        if (exerciseIndex < currentExercises.size) {
            val exercise = currentExercises[exerciseIndex]
            if (setIndex < exercise.sets.size) {
                exercise.sets[setIndex].timeSeconds = timeSeconds
            }
        }
    }

    fun completeSet(exerciseIndex: Int, setIndex: Int) {
        val currentExercises = _exercises.value ?: return
        if (exerciseIndex < currentExercises.size) {
            val exercise = currentExercises[exerciseIndex]
            if (setIndex < exercise.sets.size) {
                exercise.sets[setIndex].completed = true
                // Neaktualizovat LiveData - adapter si sam aktualizuje UI
            }
        }
    }

    fun nextExercise() {
        val current = _currentExerciseIndex.value ?: 0
        val total = _exercises.value?.size ?: 0
        if (current < total - 1) {
            _currentExerciseIndex.value = current + 1
        }
    }

    fun previousExercise() {
        val current = _currentExerciseIndex.value ?: 0
        if (current > 0) {
            _currentExerciseIndex.value = current - 1
        }
    }

    fun finishWorkout() {
        viewModelScope.launch {
            // Ulozit vsechny serie do databaze
            val exerciseList = _exercises.value ?: return@launch

            exerciseList.forEach { activeExercise ->
                activeExercise.sets.forEachIndexed { index, setData ->
                    if (setData.reps > 0 || setData.weight > 0 || setData.timeSeconds > 0) {
                        val exerciseSet = ExerciseSet(
                            workoutLogId = workoutLogId,
                            exerciseId = activeExercise.exercise.id,
                            setNumber = index + 1,
                            reps = setData.reps,
                            weight = setData.weight,
                            timeSeconds = setData.timeSeconds,
                            completed = setData.completed
                        )
                        dao.insertExerciseSet(exerciseSet)
                    }
                }
            }

            // Oznacit trenink jako dokonceny
            val log = dao.getWorkoutLogById(workoutLogId)
            log?.let {
                dao.updateWorkoutLog(it.copy(completed = true))
            }

            // Reset stavu
            _isWorkoutActive.value = false
            _currentVariant.value = null
            _exercises.value = emptyList()
            _currentExerciseIndex.value = 0
            _isViewOnly.value = false
            _isEditingCompleted.value = false
            workoutLogId = 0
        }
    }

    fun cancelWorkout() {
        viewModelScope.launch {
            // Smazat nedokonceny log
            if (workoutLogId > 0) {
                val log = dao.getWorkoutLogById(workoutLogId)
                log?.let { dao.deleteWorkoutLog(it) }
            }

            _isWorkoutActive.value = false
            _currentVariant.value = null
            _exercises.value = emptyList()
            _currentExerciseIndex.value = 0
            _isViewOnly.value = false
            _isEditingCompleted.value = false
            workoutLogId = 0
        }
    }

    fun closeViewMode() {
        _isWorkoutActive.value = false
        _currentVariant.value = null
        _exercises.value = emptyList()
        _currentExerciseIndex.value = 0
        _isViewOnly.value = false
        _isEditingCompleted.value = false
        workoutLogId = 0
    }
}
