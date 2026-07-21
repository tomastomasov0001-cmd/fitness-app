package com.harvis.fitnessapp.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * Trida pro export/import dat treninku
 */
data class BackupData(
    val version: Int = 1,
    val exportDate: Long = System.currentTimeMillis(),
    val variants: List<WorkoutVariantBackup>,
    val exercises: List<ExerciseBackup>,
    val variantExercises: List<VariantExerciseBackup>
)

data class WorkoutVariantBackup(
    val id: Long,
    val name: String,
    val description: String
)

data class ExerciseBackup(
    val id: Long,
    val name: String,
    val description: String,
    val hasSets: Boolean,
    val hasReps: Boolean,
    val hasWeight: Boolean,
    val hasTime: Boolean,
    val defaultSets: Int
)

data class VariantExerciseBackup(
    val variantId: Long,
    val exerciseId: Long,
    val orderIndex: Int
)

object BackupHelper {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun toJson(data: BackupData): String {
        return gson.toJson(data)
    }

    fun fromJson(json: String): BackupData? {
        return try {
            gson.fromJson(json, BackupData::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun createBackup(
        variants: List<WorkoutVariant>,
        exercises: List<Exercise>,
        variantExercises: List<VariantExercise>
    ): BackupData {
        return BackupData(
            variants = variants.map {
                WorkoutVariantBackup(it.id, it.name, it.description)
            },
            exercises = exercises.map {
                ExerciseBackup(
                    it.id, it.name, it.description,
                    it.hasSets, it.hasReps, it.hasWeight, it.hasTime, it.defaultSets
                )
            },
            variantExercises = variantExercises.map {
                VariantExerciseBackup(it.variantId, it.exerciseId, it.orderIndex)
            }
        )
    }
}
