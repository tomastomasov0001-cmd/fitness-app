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

    // Zalozni kopie puvodnich dat z databaze (pro pripad poskozeni TextWatchery)
    private var originalSetsBackup: List<ExerciseSet> = emptyList()

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

            // Nacist ulozene serie z databaze PRVNE
            val savedSets = dao.getSetsForWorkoutSync(workoutLogId)
            val setsByExercise = savedSets.groupBy { it.exerciseId }

            // Ziskat unikatni ID cviku z ulozenych serii
            val savedExerciseIds = savedSets.map { it.exerciseId }.toSet()

            // Nacist cviky z aktualni varianty
            val variantExercises = dao.getExercisesForVariantSync(variant.id)
            val variantExerciseIds = variantExercises.map { it.id }.toSet()

            // Najit cviky, ktere maji ulozene serie ale nejsou v aktualni variante
            // (byly odebrane po dokonceni treningu)
            val missingExerciseIds = savedExerciseIds - variantExerciseIds
            val missingExercises = missingExerciseIds.mapNotNull { id ->
                dao.getExerciseById(id)
            }

            // Spojit cviky: nejprve ty z varianty, pak ty co byly odebrane
            val allExercises = variantExercises + missingExercises

            val activeExercises = allExercises.map { exercise ->
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

            // Ulozit zalohu puvodnich dat pro pripad poskozeni
            originalSetsBackup = savedSets

            _currentVariant.value = variant
            _currentExerciseIndex.value = 0
            // Nastavit UI stavy PRED exercises
            _isViewOnly.value = true
            _isEditingCompleted.value = false
            _isWorkoutActive.value = true
            // Exercises jako posledni
            _exercises.value = activeExercises
        }
    }

    /**
     * Nacte dokonceny trenink PRIMO do editacniho rezimu.
     * Pouziva se pri kliknuti na "Upravit" z kalendare.
     */
    fun loadCompletedWorkoutForEdit(variantId: Long, workoutLogId: Long) {
        viewModelScope.launch {
            android.util.Log.e("WORKOUT_DEBUG", "loadCompletedWorkoutForEdit: variantId=$variantId, workoutLogId=$workoutLogId")

            val variant = dao.getVariantById(variantId) ?: return@launch
            this@WorkoutViewModel.workoutLogId = workoutLogId

            // Nacist ulozene serie z databaze
            val savedSets = dao.getSetsForWorkoutSync(workoutLogId)

            android.util.Log.e("WORKOUT_DEBUG", "loadCompletedWorkoutForEdit: savedSets count=${savedSets.size}")
            savedSets.forEach { set ->
                android.util.Log.e("WORKOUT_DEBUG", "  DB Set: exerciseId=${set.exerciseId}, setNum=${set.setNumber}, reps=${set.reps}, weight=${set.weight}")
            }
            val setsByExercise = savedSets.groupBy { it.exerciseId }

            // Ziskat unikatni ID cviku z ulozenych serii
            val savedExerciseIds = savedSets.map { it.exerciseId }.toSet()

            // Nacist cviky z aktualni varianty
            val variantExercises = dao.getExercisesForVariantSync(variant.id)
            val variantExerciseIds = variantExercises.map { it.id }.toSet()

            // Najit cviky, ktere maji ulozene serie ale nejsou v aktualni variante
            val missingExerciseIds = savedExerciseIds - variantExerciseIds
            val missingExercises = missingExerciseIds.mapNotNull { id ->
                dao.getExerciseById(id)
            }

            // Spojit cviky
            val allExercises = variantExercises + missingExercises

            val activeExercises = allExercises.map { exercise ->
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
                        mutableListOf(SetData(completed = true))
                    }
                )
            }

            // Ulozit zalohu puvodnich dat
            originalSetsBackup = savedSets

            _currentVariant.value = variant
            _currentExerciseIndex.value = 0
            // DULEZITE: Nastavit UI stavy PRED exercises,
            // aby se refresh spustil jen jednou se spravnymi hodnotami
            _isViewOnly.value = false
            _isEditingCompleted.value = true
            _isWorkoutActive.value = true
            // Exercises jako posledni - tohle triggeruje hlavni refresh
            _exercises.value = activeExercises
        }
    }

    fun enableEditing() {
        _isViewOnly.value = false
        _isEditingCompleted.value = true
    }

    private val _saveCompleted = MutableLiveData<Boolean>()
    val saveCompleted: LiveData<Boolean> = _saveCompleted

    private val _finishCompleted = MutableLiveData<Boolean>()
    val finishCompleted: LiveData<Boolean> = _finishCompleted

    fun saveCompletedWorkout() {
        viewModelScope.launch {
            val exerciseList = _exercises.value ?: return@launch

            android.util.Log.e("WORKOUT_DEBUG", "saveCompletedWorkout: exerciseListSize=${exerciseList.size}, workoutLogId=$workoutLogId")

            // DULEZITE: Vytvorit OKAMZITOU KOPII dat pred jakoukoli async operaci
            // aby se data nemohla zmenit TextWatchery behem ukladani
            val setsToSave = mutableListOf<ExerciseSet>()
            exerciseList.forEach { activeExercise ->
                android.util.Log.e("WORKOUT_DEBUG", "  Exercise: ${activeExercise.exercise.name}, setsCount=${activeExercise.sets.size}")
                activeExercise.sets.forEachIndexed { index, setData ->
                    // Kopirovat hodnoty HNED - ne pozdeji
                    val reps = setData.reps
                    val weight = setData.weight
                    val timeSeconds = setData.timeSeconds
                    val completed = setData.completed

                    android.util.Log.e("WORKOUT_DEBUG", "    Set $index: reps=$reps, weight=$weight, time=$timeSeconds, completed=$completed")

                    // Ulozit serii pokud je completed NEBO ma nejake hodnoty
                    if (completed || reps > 0 || weight > 0 || timeSeconds > 0) {
                        setsToSave.add(ExerciseSet(
                            workoutLogId = workoutLogId,
                            exerciseId = activeExercise.exercise.id,
                            setNumber = index + 1,
                            reps = reps,
                            weight = weight,
                            timeSeconds = timeSeconds,
                            completed = completed
                        ))
                    }
                }
            }

            android.util.Log.e("WORKOUT_DEBUG", "setsToSave count: ${setsToSave.size}")

            // BEZPECNOSTNI KONTROLA: Detekce poskozeni dat
            // Pokud puvodni data mela hodnoty ale nova maji vse na 0, pouzij zalohu
            val originalHadValues = originalSetsBackup.any { it.reps > 0 || it.weight > 0 || it.timeSeconds > 0 }
            val newHasValues = setsToSave.any { it.reps > 0 || it.weight > 0 || it.timeSeconds > 0 }

            val dataCorrupted = originalHadValues && !newHasValues && originalSetsBackup.isNotEmpty()

            if (dataCorrupted) {
                // Data byla poskozena, obnovit ze zalohy
                android.util.Log.w("WorkoutViewModel", "saveCompletedWorkout: Data corruption detected, restoring from backup")
                // Nemazat a neprepisovat - puvodni data zustana v databazi
            } else if (setsToSave.isEmpty() && exerciseList.isNotEmpty()) {
                // Zadne serie k ulozeni ale cviky existuji - podezrele
                android.util.Log.w("WorkoutViewModel", "saveCompletedWorkout: No sets to save, keeping original data")
            } else {
                // Vse v poradku, ulozit nova data
                dao.deleteSetsForWorkout(workoutLogId)
                setsToSave.forEach { set ->
                    dao.insertExerciseSet(set)
                }
            }

            android.util.Log.e("WORKOUT_DEBUG", "saveCompletedWorkout: DB operations completed successfully")

            // Reset stavu
            _isWorkoutActive.value = false
            _currentVariant.value = null
            _exercises.value = emptyList()
            _currentExerciseIndex.value = 0
            _isViewOnly.value = false
            _isEditingCompleted.value = false
            workoutLogId = 0
            originalSetsBackup = emptyList()

            // Signal completion
            _saveCompleted.value = true
        }
    }

    fun resetSaveCompleted() {
        _saveCompleted.value = false
    }

    fun resetFinishCompleted() {
        _finishCompleted.value = false
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

            // DULEZITE: Vytvorit OKAMZITOU KOPII dat pred jakoukoli async operaci
            val setsToInsert = mutableListOf<ExerciseSet>()
            exerciseList.forEach { activeExercise ->
                activeExercise.sets.forEachIndexed { index, setData ->
                    // Kopirovat hodnoty HNED
                    val reps = setData.reps
                    val weight = setData.weight
                    val timeSeconds = setData.timeSeconds
                    val completed = setData.completed

                    // Ulozit serii pokud je completed NEBO ma nejake hodnoty
                    if (completed || reps > 0 || weight > 0 || timeSeconds > 0) {
                        setsToInsert.add(ExerciseSet(
                            workoutLogId = workoutLogId,
                            exerciseId = activeExercise.exercise.id,
                            setNumber = index + 1,
                            reps = reps,
                            weight = weight,
                            timeSeconds = timeSeconds,
                            completed = completed
                        ))
                    }
                }
            }

            // Ted ulozit do DB (muze byt async)
            setsToInsert.forEach { exerciseSet ->
                dao.insertExerciseSet(exerciseSet)
            }

            // Oznacit trenink jako dokonceny
            val log = dao.getWorkoutLogById(workoutLogId)
            log?.let {
                dao.updateWorkoutLog(it.copy(completed = true))
            }

            android.util.Log.e("WORKOUT_DEBUG", "finishWorkout: DB operations completed successfully")

            // Reset stavu
            _isWorkoutActive.value = false
            _currentVariant.value = null
            _exercises.value = emptyList()
            _currentExerciseIndex.value = 0
            _isViewOnly.value = false
            _isEditingCompleted.value = false
            workoutLogId = 0

            // Signal completion
            _finishCompleted.value = true
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
            originalSetsBackup = emptyList()
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
        originalSetsBackup = emptyList()
    }
}
