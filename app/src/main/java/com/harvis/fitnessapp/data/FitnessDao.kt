package com.harvis.fitnessapp.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface FitnessDao {

    // === VARIANTY ===
    @Query("SELECT * FROM workout_variants ORDER BY name ASC")
    fun getAllVariants(): LiveData<List<WorkoutVariant>>

    @Query("""
        SELECT v.id, v.name, v.description,
               (SELECT COUNT(*) FROM variant_exercises ve WHERE ve.variantId = v.id) as exerciseCount
        FROM workout_variants v
        ORDER BY v.name ASC
    """)
    fun getAllVariantsWithCount(): LiveData<List<VariantWithCount>>

    @Query("SELECT * FROM workout_variants WHERE id = :id")
    suspend fun getVariantById(id: Long): WorkoutVariant?

    @Insert
    suspend fun insertVariant(variant: WorkoutVariant): Long

    @Update
    suspend fun updateVariant(variant: WorkoutVariant)

    @Delete
    suspend fun deleteVariant(variant: WorkoutVariant)

    @Query("DELETE FROM workout_variants WHERE id = :id")
    suspend fun deleteVariantById(id: Long)

    // === CVIKY ===
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): LiveData<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: Long): Exercise?

    @Insert
    suspend fun insertExercise(exercise: Exercise): Long

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Delete
    suspend fun deleteExercise(exercise: Exercise)

    // === PROPOJENI CVIK-VARIANTA ===
    @Query("""
        SELECT e.* FROM exercises e
        INNER JOIN variant_exercises ve ON e.id = ve.exerciseId
        WHERE ve.variantId = :variantId
        ORDER BY ve.orderIndex ASC
    """)
    fun getExercisesForVariant(variantId: Long): LiveData<List<Exercise>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariantExercise(variantExercise: VariantExercise)

    @Query("DELETE FROM variant_exercises WHERE variantId = :variantId AND exerciseId = :exerciseId")
    suspend fun removeExerciseFromVariant(variantId: Long, exerciseId: Long)

    @Transaction
    @Query("SELECT * FROM workout_variants WHERE id = :variantId")
    fun getVariantWithExercises(variantId: Long): LiveData<VariantWithExercises?>

    @Query("SELECT COUNT(*) FROM variant_exercises WHERE variantId = :variantId")
    suspend fun getExerciseCountForVariant(variantId: Long): Int

    @Query("""
        SELECT e.* FROM exercises e
        INNER JOIN variant_exercises ve ON e.id = ve.exerciseId
        WHERE ve.variantId = :variantId
        ORDER BY ve.orderIndex ASC
    """)
    suspend fun getExercisesForVariantSync(variantId: Long): List<Exercise>

    @Query("SELECT * FROM workout_variants")
    suspend fun getAllVariantsSync(): List<WorkoutVariant>

    @Query("SELECT * FROM exercises")
    suspend fun getAllExercisesSync(): List<Exercise>

    @Query("SELECT * FROM variant_exercises")
    suspend fun getAllVariantExercisesSync(): List<VariantExercise>

    @Query("SELECT * FROM workout_variants WHERE name = :name LIMIT 1")
    suspend fun getVariantByName(name: String): WorkoutVariant?

    @Query("SELECT * FROM exercises WHERE name = :name LIMIT 1")
    suspend fun getExerciseByName(name: String): Exercise?

    // === WORKOUT LOGY ===
    @Query("SELECT * FROM workout_logs WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getWorkoutLogsBetween(startDate: Long, endDate: Long): LiveData<List<WorkoutLog>>

    @Query("SELECT * FROM workout_logs WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getWorkoutLogsBetweenSync(startDate: Long, endDate: Long): List<WorkoutLog>

    @Query("SELECT * FROM workout_logs WHERE date = :date")
    fun getWorkoutLogsForDate(date: Long): LiveData<List<WorkoutLog>>

    @Query("SELECT * FROM workout_logs WHERE id = :id")
    suspend fun getWorkoutLogById(id: Long): WorkoutLog?

    @Insert
    suspend fun insertWorkoutLog(log: WorkoutLog): Long

    @Update
    suspend fun updateWorkoutLog(log: WorkoutLog)

    @Delete
    suspend fun deleteWorkoutLog(log: WorkoutLog)

    @Query("DELETE FROM workout_logs WHERE id = :id")
    suspend fun deleteWorkoutLogById(id: Long)

    @Transaction
    @Query("SELECT * FROM workout_logs ORDER BY date DESC")
    fun getAllWorkoutLogsWithDetails(): LiveData<List<WorkoutLogWithDetails>>

    // === SERIE CVIKU ===
    @Query("SELECT * FROM exercise_sets WHERE workoutLogId = :workoutLogId ORDER BY exerciseId, setNumber")
    fun getSetsForWorkout(workoutLogId: Long): LiveData<List<ExerciseSet>>

    @Query("SELECT * FROM exercise_sets WHERE workoutLogId = :workoutLogId ORDER BY exerciseId, setNumber")
    suspend fun getSetsForWorkoutSync(workoutLogId: Long): List<ExerciseSet>

    @Query("SELECT * FROM exercise_sets WHERE workoutLogId = :workoutLogId AND exerciseId = :exerciseId ORDER BY setNumber")
    fun getSetsForExercise(workoutLogId: Long, exerciseId: Long): LiveData<List<ExerciseSet>>

    @Insert
    suspend fun insertExerciseSet(set: ExerciseSet): Long

    @Update
    suspend fun updateExerciseSet(set: ExerciseSet)

    @Delete
    suspend fun deleteExerciseSet(set: ExerciseSet)

    @Query("DELETE FROM exercise_sets WHERE workoutLogId = :workoutLogId")
    suspend fun deleteSetsForWorkout(workoutLogId: Long)
}
