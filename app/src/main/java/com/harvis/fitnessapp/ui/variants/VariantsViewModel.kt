package com.harvis.fitnessapp.ui.variants

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.harvis.fitnessapp.FitnessApp
import com.harvis.fitnessapp.R
import com.harvis.fitnessapp.data.BackupData
import com.harvis.fitnessapp.data.BackupHelper
import com.harvis.fitnessapp.data.Exercise
import com.harvis.fitnessapp.data.VariantExercise
import com.harvis.fitnessapp.data.VariantWithCount
import com.harvis.fitnessapp.data.VariantWithExercises
import com.harvis.fitnessapp.data.WorkoutVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VariantsViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = (application as FitnessApp).database.fitnessDao()

    val allVariants: LiveData<List<WorkoutVariant>> = dao.getAllVariants()
    val allVariantsWithCount: LiveData<List<VariantWithCount>> = dao.getAllVariantsWithCount()
    val allExercises: LiveData<List<Exercise>> = dao.getAllExercises()

    private val _exportResult = MutableLiveData<String?>()
    val exportResult: LiveData<String?> = _exportResult

    private val _importResult = MutableLiveData<ImportResult?>()
    val importResult: LiveData<ImportResult?> = _importResult

    data class ImportResult(val success: Boolean, val message: String)

    fun getVariantWithExercises(variantId: Long): LiveData<VariantWithExercises?> {
        return dao.getVariantWithExercises(variantId)
    }

    fun getExercisesForVariant(variantId: Long): LiveData<List<Exercise>> {
        return dao.getExercisesForVariant(variantId)
    }

    fun insertVariant(variant: WorkoutVariant) {
        viewModelScope.launch {
            dao.insertVariant(variant)
        }
    }

    fun updateVariant(variant: WorkoutVariant) {
        viewModelScope.launch {
            dao.updateVariant(variant)
        }
    }

    fun deleteVariant(variant: WorkoutVariant) {
        viewModelScope.launch {
            dao.deleteVariant(variant)
        }
    }

    fun deleteVariantById(variantId: Long) {
        viewModelScope.launch {
            dao.deleteVariantById(variantId)
        }
    }

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

    fun addExerciseToVariant(variantId: Long, exerciseId: Long, order: Int) {
        viewModelScope.launch {
            dao.insertVariantExercise(VariantExercise(variantId, exerciseId, order))
        }
    }

    fun removeExerciseFromVariant(variantId: Long, exerciseId: Long) {
        viewModelScope.launch {
            dao.removeExerciseFromVariant(variantId, exerciseId)
        }
    }

    fun insertExerciseAndAddToVariant(exercise: Exercise, variantId: Long) {
        viewModelScope.launch {
            val exerciseId = dao.insertExercise(exercise)
            val count = dao.getExerciseCountForVariant(variantId)
            dao.insertVariantExercise(VariantExercise(variantId, exerciseId, count))
        }
    }

    // === EXPORT / IMPORT ===

    fun exportData() {
        viewModelScope.launch {
            try {
                val variants = dao.getAllVariantsSync()
                val exercises = dao.getAllExercisesSync()
                val variantExercises = dao.getAllVariantExercisesSync()

                val backup = BackupHelper.createBackup(variants, exercises, variantExercises)
                val json = BackupHelper.toJson(backup)

                _exportResult.value = json
            } catch (e: Exception) {
                _exportResult.value = null
            }
        }
    }

    fun clearExportResult() {
        _exportResult.value = null
    }

    fun importData(json: String) {
        viewModelScope.launch {
            try {
                val backup = BackupHelper.fromJson(json)
                if (backup == null) {
                    _importResult.value = ImportResult(false, getApplication<Application>().getString(R.string.invalid_file_format))
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    // Mapovani starych ID na nova ID
                    val variantIdMap = mutableMapOf<Long, Long>()
                    val exerciseIdMap = mutableMapOf<Long, Long>()

                    // Import cviku
                    for (exerciseBackup in backup.exercises) {
                        // Zkontrolovat zda cvik uz existuje
                        val existing = dao.getExerciseByName(exerciseBackup.name)
                        if (existing != null) {
                            exerciseIdMap[exerciseBackup.id] = existing.id
                        } else {
                            val newExercise = Exercise(
                                name = exerciseBackup.name,
                                description = exerciseBackup.description,
                                hasSets = exerciseBackup.hasSets,
                                hasReps = exerciseBackup.hasReps,
                                hasWeight = exerciseBackup.hasWeight,
                                hasTime = exerciseBackup.hasTime,
                                defaultSets = exerciseBackup.defaultSets
                            )
                            val newId = dao.insertExercise(newExercise)
                            exerciseIdMap[exerciseBackup.id] = newId
                        }
                    }

                    // Import variant
                    for (variantBackup in backup.variants) {
                        val existing = dao.getVariantByName(variantBackup.name)
                        if (existing != null) {
                            variantIdMap[variantBackup.id] = existing.id
                        } else {
                            val newVariant = WorkoutVariant(
                                name = variantBackup.name,
                                description = variantBackup.description
                            )
                            val newId = dao.insertVariant(newVariant)
                            variantIdMap[variantBackup.id] = newId
                        }
                    }

                    // Import propojeni
                    for (ve in backup.variantExercises) {
                        val newVariantId = variantIdMap[ve.variantId] ?: continue
                        val newExerciseId = exerciseIdMap[ve.exerciseId] ?: continue

                        dao.insertVariantExercise(
                            VariantExercise(newVariantId, newExerciseId, ve.orderIndex)
                        )
                    }
                }

                _importResult.value = ImportResult(
                    true,
                    getApplication<Application>().getString(R.string.import_success, backup.variants.size, backup.exercises.size)
                )
            } catch (e: Exception) {
                _importResult.value = ImportResult(false, getApplication<Application>().getString(R.string.import_error, e.message ?: ""))
            }
        }
    }

    fun clearImportResult() {
        _importResult.value = null
    }
}
