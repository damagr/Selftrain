package com.selftrain.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.selftrain.app.data.model.*

@Database(
    entities = [Exercise::class, Routine::class, RoutineExercise::class, Workout::class, WorkoutSet::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun routineDao(): RoutineDao
    abstract fun workoutDao(): WorkoutDao
}
