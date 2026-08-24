package com.harvis.fitnessapp.data

import androidx.room.*

/**
 * Varianta treninku (napr. "Nohy", "Hrudnik", "Push day")
 */
@Entity(tableName = "workout_variants")
data class WorkoutVariant(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Cvik (napr. "Drepy", "Bench press")
 */
@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val imagePath: String? = null,  // cesta k obrazku
    val videoPath: String? = null,  // cesta k videu
    val hasSets: Boolean = true,    // ma serie (vice opakovani cviku)
    val hasReps: Boolean = true,    // ma opakovani (napr. drepy)
    val hasWeight: Boolean = true,  // ma vahu (napr. bench press)
    val hasTime: Boolean = false,   // ma cas (napr. beh, plank)
    val defaultSets: Int = 3,       // vychozi pocet serii
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Propojeni cviku s variantou treninku
 */
@Entity(
    tableName = "variant_exercises",
    primaryKeys = ["variantId", "exerciseId"],
    foreignKeys = [
        ForeignKey(
            entity = WorkoutVariant::class,
            parentColumns = ["id"],
            childColumns = ["variantId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class VariantExercise(
    val variantId: Long,
    val exerciseId: Long,
    val orderIndex: Int = 0  // poradi cviku ve variante
)

/**
 * Zaznam treninku v kalendari
 */
@Entity(tableName = "workout_logs")
data class WorkoutLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val variantId: Long,
    val date: Long,  // datum treninku (timestamp)
    val notes: String = "",
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Zaznam serie cviku (opakovani, vaha)
 */
@Entity(
    tableName = "exercise_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutLog::class,
            parentColumns = ["id"],
            childColumns = ["workoutLogId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExerciseSet(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutLogId: Long,
    val exerciseId: Long,
    val setNumber: Int,      // cislo serie (1, 2, 3...)
    val reps: Int = 0,       // pocet opakovani
    val weight: Float = 0f,  // vaha v kg
    val timeSeconds: Int = 0, // cas v sekundach (pro beh, plank, atd.)
    val completed: Boolean = false
)

/**
 * Varianta s cviky - pro zobrazeni
 */
data class VariantWithExercises(
    @Embedded val variant: WorkoutVariant,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            VariantExercise::class,
            parentColumn = "variantId",
            entityColumn = "exerciseId"
        )
    )
    val exercises: List<Exercise>
)

/**
 * Log treninku s detaily
 */
data class WorkoutLogWithDetails(
    @Embedded val log: WorkoutLog,
    @Relation(
        parentColumn = "variantId",
        entityColumn = "id"
    )
    val variant: WorkoutVariant
)

/**
 * Varianta s poctem cviku - pro seznam
 */
data class VariantWithCount(
    val id: Long,
    val name: String,
    val description: String,
    val exerciseCount: Int,
    val createdAt: Long = 0  // Pridano pro zachovani pri editaci
)

// === STATISTIKY ===

/**
 * Pocet treninku podle varianty - pro kolacovy graf
 */
data class VariantWorkoutCount(
    val variantName: String,
    val count: Int
)

/**
 * Serie cviku s datem treninku - pro grafy progresu
 */
data class ExerciseSetWithDate(
    val id: Long,
    val workoutLogId: Long,
    val exerciseId: Long,
    val setNumber: Int,
    val reps: Int,
    val weight: Float,
    val timeSeconds: Int,
    val completed: Boolean,
    val workoutDate: Long
)

/**
 * Pocet treninku na datum - pro graf frekvence a heatmapu
 */
data class DateWorkoutCount(
    val date: Long,
    val count: Int
)
