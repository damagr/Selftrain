package com.entrenaguay.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.entrenaguay.app.data.model.*

@Database(
    entities = [Exercise::class, Routine::class, RoutineExercise::class, Workout::class, WorkoutSet::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun routineDao(): RoutineDao
    abstract fun workoutDao(): WorkoutDao
}
