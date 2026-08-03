package com.selftrain.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.selftrain.app.data.db.AppDatabase
import com.selftrain.app.data.db.ExerciseDao
import com.selftrain.app.data.db.RoutineDao
import com.selftrain.app.data.db.WorkoutDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE exercises ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE exercises ADD COLUMN equipment TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE workouts ADD COLUMN endDate INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE workouts ADD COLUMN durationMinutes INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE routines ADD COLUMN `order` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE routines ADD COLUMN parentId INTEGER DEFAULT NULL")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE workouts ADD COLUMN lastExerciseIndex INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE exercises ADD COLUMN gifUrl TEXT DEFAULT NULL")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE routine_exercises ADD COLUMN isBilbo INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Recrear workouts sin FK a routines: borrar una rutina no debe borrar el historial
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS workouts_new (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                routineId INTEGER NOT NULL,
                date INTEGER NOT NULL,
                notes TEXT NOT NULL,
                completed INTEGER NOT NULL,
                endDate INTEGER NOT NULL,
                durationMinutes INTEGER NOT NULL,
                lastExerciseIndex INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO workouts_new (id, routineId, date, notes, completed, endDate, durationMinutes, lastExerciseIndex)
            SELECT id, routineId, date, notes, completed, endDate, durationMinutes, lastExerciseIndex FROM workouts
        """.trimIndent())
        db.execSQL("DROP TABLE workouts")
        db.execSQL("ALTER TABLE workouts_new RENAME TO workouts")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_workouts_routineId ON workouts (routineId)")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "selftrain.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
            .build()

    @Provides
    fun provideExerciseDao(db: AppDatabase): ExerciseDao = db.exerciseDao()

    @Provides
    fun provideRoutineDao(db: AppDatabase): RoutineDao = db.routineDao()

    @Provides
    fun provideWorkoutDao(db: AppDatabase): WorkoutDao = db.workoutDao()
}
