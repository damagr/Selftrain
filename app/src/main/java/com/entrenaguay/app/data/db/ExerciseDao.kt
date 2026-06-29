package com.entrenaguay.app.data.db

import androidx.room.*
import com.entrenaguay.app.data.model.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY muscleGroup, name")
    fun getAll(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE category = :category ORDER BY name")
    fun getByCategory(category: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: Long): Exercise?

    @Query("SELECT * FROM exercises WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<Exercise>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<Exercise>)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: Exercise): Long

    @Delete
    suspend fun delete(exercise: Exercise)

    @Query("SELECT COUNT(*) FROM routine_exercises WHERE exerciseId = :exerciseId")
    suspend fun countUsageInRoutines(exerciseId: Long): Int

    @Query("SELECT COUNT(*) FROM workout_sets WHERE exerciseId = :exerciseId")
    suspend fun countUsageInSets(exerciseId: Long): Int

    @Query("SELECT * FROM exercises")
    suspend fun getAllList(): List<Exercise>
}
