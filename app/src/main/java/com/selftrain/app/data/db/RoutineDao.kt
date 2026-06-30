package com.selftrain.app.data.db

import androidx.room.*
import com.selftrain.app.data.model.Routine
import com.selftrain.app.data.model.RoutineExercise
import kotlinx.coroutines.flow.Flow

data class RoutineWithExercises(
    val routine: Routine,
    val exercises: List<RoutineExerciseWithName>
)

data class RoutineExerciseWithName(
    val routineExercise: RoutineExercise,
    val exerciseName: String,
    val muscleGroup: String,
    val category: String,
    val isBilboEligible: Boolean
)

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY `order`, id DESC")
    fun getAll(): Flow<List<Routine>>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getById(id: Long): Routine?

    @Insert
    suspend fun insert(routine: Routine): Long

    @Update
    suspend fun update(routine: Routine)

    @Delete
    suspend fun delete(routine: Routine)

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY `order`")
    suspend fun getRoutineExercises(routineId: Long): List<RoutineExercise>

    @Insert
    suspend fun addExercise(routineExercise: RoutineExercise)

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun clearExercises(routineId: Long)

    @Query("DELETE FROM routine_exercises WHERE id = :id")
    suspend fun removeExercise(id: Long)

    @Query("SELECT * FROM routines")
    suspend fun getAllList(): List<Routine>

    @Query("SELECT * FROM routine_exercises")
    suspend fun getAllRoutineExercises(): List<RoutineExercise>

    @Query("UPDATE routines SET `order` = :order WHERE id = :id")
    suspend fun updateOrder(id: Long, order: Int)
}
