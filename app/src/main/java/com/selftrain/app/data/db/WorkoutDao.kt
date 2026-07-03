package com.selftrain.app.data.db

import androidx.room.*
import com.selftrain.app.data.model.Workout
import com.selftrain.app.data.model.WorkoutSet
import kotlinx.coroutines.flow.Flow

data class SetWithExercise(
    @androidx.room.Embedded
    val set: WorkoutSet,
    val exerciseName: String,
    val muscleGroup: String
)

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts ORDER BY date DESC")
    fun getAll(): Flow<List<Workout>>

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getById(id: Long): Workout?

    @Query("SELECT * FROM workouts WHERE routineId = :routineId AND completed = 1 ORDER BY date DESC LIMIT 1")
    suspend fun getLastCompleted(routineId: Long): Workout?

    @Insert
    suspend fun insert(workout: Workout): Long

    @Update
    suspend fun update(workout: Workout)

    @Delete
    suspend fun delete(workout: Workout)

    // Sets
    @Insert
    suspend fun insertSet(workoutSet: WorkoutSet): Long

    @Query("SELECT * FROM workout_sets WHERE workoutId = :workoutId AND exerciseId = :exerciseId ORDER BY id")
    suspend fun getSetsForExercise(workoutId: Long, exerciseId: Long): List<WorkoutSet>

    @Query("""
        SELECT ws.*, e.name as exerciseName, e.muscleGroup
        FROM workout_sets ws
        JOIN exercises e ON ws.exerciseId = e.id
        WHERE ws.workoutId = :workoutId
        ORDER BY ws.id
    """)
    suspend fun getSetsWithExercise(workoutId: Long): List<SetWithExercise>

    // History queries for progression charts
    @Query("""
        SELECT ws.*, e.name as exerciseName, e.muscleGroup
        FROM workout_sets ws
        JOIN workouts w ON ws.workoutId = w.id
        JOIN exercises e ON ws.exerciseId = e.id
        WHERE ws.exerciseId = :exerciseId AND w.completed = 1
        ORDER BY w.date ASC, ws.id ASC
    """)
    suspend fun getSetHistoryForExercise(exerciseId: Long): List<SetWithExercise>

    @Query("""
        SELECT MAX(ws.weightKg * (1.0 + ws.reps / 30.0)) 
        FROM workout_sets ws 
        JOIN workouts w ON ws.workoutId = w.id 
        WHERE ws.exerciseId = :exerciseId AND w.completed = 1
    """)
    suspend fun getMaxEstimated1RM(exerciseId: Long): Double?

    @Delete
    suspend fun deleteSet(workoutSet: WorkoutSet)

    @Update
    suspend fun updateSet(workoutSet: WorkoutSet)

    @Query("""
        SELECT DISTINCT ws.exerciseId
        FROM workout_sets ws
        JOIN workouts w ON ws.workoutId = w.id
        WHERE w.completed = 1
    """)
    suspend fun getExerciseIdsWithHistory(): List<Long>

    @Query("SELECT * FROM workouts")
    suspend fun getAllList(): List<Workout>

    @Query("SELECT * FROM workout_sets")
    suspend fun getAllSets(): List<WorkoutSet>

    @Query("SELECT * FROM workouts WHERE completed = 1 AND date BETWEEN :from AND :to ORDER BY date DESC")
    suspend fun getCompletedWorkoutsBetween(from: Long, to: Long): List<Workout>

    // ponytail: crash recovery — find orphaned unfinished workouts
    @Query("SELECT * FROM workouts WHERE completed = 0 ORDER BY date DESC LIMIT 1")
    suspend fun getUnfinishedWorkout(): Workout?

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteById(id: Long)
}
