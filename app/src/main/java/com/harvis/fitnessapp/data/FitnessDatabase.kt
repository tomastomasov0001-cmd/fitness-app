package com.harvis.fitnessapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WorkoutVariant::class,
        Exercise::class,
        VariantExercise::class,
        WorkoutLog::class,
        ExerciseSet::class
    ],
    version = 4,
    exportSchema = false
)
abstract class FitnessDatabase : RoomDatabase() {

    abstract fun fitnessDao(): FitnessDao

    companion object {
        @Volatile
        private var INSTANCE: FitnessDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Pridat nove sloupce do exercises
                database.execSQL("ALTER TABLE exercises ADD COLUMN hasReps INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE exercises ADD COLUMN hasWeight INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE exercises ADD COLUMN hasTime INTEGER NOT NULL DEFAULT 0")
                // Pridat sloupec timeSeconds do exercise_sets
                database.execSQL("ALTER TABLE exercise_sets ADD COLUMN timeSeconds INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Pridat sloupec hasSets do exercises
                database.execSQL("ALTER TABLE exercises ADD COLUMN hasSets INTEGER NOT NULL DEFAULT 1")
            }
        }

        // Prima migrace z verze 1 na 3 (pro uzivatele, kteri preskocili verzi 2)
        private val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Pridat nove sloupce do exercises
                database.execSQL("ALTER TABLE exercises ADD COLUMN hasSets INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE exercises ADD COLUMN hasReps INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE exercises ADD COLUMN hasWeight INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE exercises ADD COLUMN hasTime INTEGER NOT NULL DEFAULT 0")
                // Pridat sloupec timeSeconds do exercise_sets
                database.execSQL("ALTER TABLE exercise_sets ADD COLUMN timeSeconds INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Pridat sloupec defaultSets do exercises
                database.execSQL("ALTER TABLE exercises ADD COLUMN defaultSets INTEGER NOT NULL DEFAULT 3")
            }
        }

        // Prima migrace z verze 1 na 4
        private val MIGRATION_1_4 = object : Migration(1, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE exercises ADD COLUMN hasSets INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE exercises ADD COLUMN hasReps INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE exercises ADD COLUMN hasWeight INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE exercises ADD COLUMN hasTime INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE exercises ADD COLUMN defaultSets INTEGER NOT NULL DEFAULT 3")
                database.execSQL("ALTER TABLE exercise_sets ADD COLUMN timeSeconds INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): FitnessDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FitnessDatabase::class.java,
                    "fitness_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3, MIGRATION_3_4, MIGRATION_1_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
