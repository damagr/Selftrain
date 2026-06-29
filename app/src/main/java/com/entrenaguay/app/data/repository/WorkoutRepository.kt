package com.entrenaguay.app.data.repository

import com.entrenaguay.app.data.db.WorkoutDao
import com.entrenaguay.app.data.db.SetWithExercise
import com.entrenaguay.app.data.model.Workout
import com.entrenaguay.app.data.model.WorkoutSet
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val dao: WorkoutDao
) {
    val workouts: Flow<List<Workout>> = dao.getAll()

    suspend fun getById(id: Long) = dao.getById(id)
    suspend fun getByRoutine(routineId: Long) = dao.getByRoutine(routineId)
    suspend fun getLastCompleted(routineId: Long) = dao.getLastCompleted(routineId)
    suspend fun insert(workout: Workout) = dao.insert(workout)
    suspend fun update(workout: Workout) = dao.update(workout)
    suspend fun delete(workout: Workout) = dao.delete(workout)

    suspend fun insertSet(set: WorkoutSet) = dao.insertSet(set)
    suspend fun getSetsForExercise(workoutId: Long, exerciseId: Long) = dao.getSetsForExercise(workoutId, exerciseId)
    suspend fun getSetsWithExercise(workoutId: Long) = dao.getSetsWithExercise(workoutId)
    suspend fun getSetHistory(exerciseId: Long) = dao.getSetHistoryForExercise(exerciseId)
    suspend fun getMax1RM(exerciseId: Long) = dao.getMaxEstimated1RM(exerciseId)
    suspend fun deleteSet(set: WorkoutSet) = dao.deleteSet(set)
    suspend fun getExerciseIdsWithHistory() = dao.getExerciseIdsWithHistory()
}
