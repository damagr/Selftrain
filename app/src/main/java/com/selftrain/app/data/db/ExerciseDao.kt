package com.selftrain.app.data.db

import androidx.room.*
import com.selftrain.app.data.model.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE isDeleted = 0 ORDER BY muscleGroup, name")
    fun getAll(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE category = :category AND isDeleted = 0 ORDER BY name")
    fun getByCategory(category: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: Long): Exercise?

    @Query("SELECT * FROM exercises WHERE id IN (:ids) AND isDeleted = 0")
    suspend fun getByIds(ids: List<Long>): List<Exercise>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<Exercise>)

    @Query("SELECT COUNT(*) FROM exercises WHERE isDeleted = 0")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: Exercise): Long

    @Query("UPDATE exercises SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("SELECT COUNT(*) FROM routine_exercises WHERE exerciseId = :exerciseId")
    suspend fun countUsageInRoutines(exerciseId: Long): Int

    @Query("SELECT COUNT(*) FROM workout_sets WHERE exerciseId = :exerciseId")
    suspend fun countUsageInSets(exerciseId: Long): Int

    @Query("SELECT * FROM exercises WHERE isDeleted = 0")
    suspend fun getAllList(): List<Exercise>

    @Query("SELECT * FROM exercises")
    suspend fun getAllListRaw(): List<Exercise>

    @Query("SELECT * FROM exercises WHERE isDeleted = 1 ORDER BY name")
    suspend fun getDeleted(): List<Exercise>

    @Query("UPDATE exercises SET isDeleted = 0 WHERE id = :id")
    suspend fun restore(id: Long)
}
