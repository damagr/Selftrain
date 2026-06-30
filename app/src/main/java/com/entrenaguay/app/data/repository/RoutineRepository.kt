package com.entrenaguay.app.data.repository

import com.entrenaguay.app.data.db.RoutineDao
import com.entrenaguay.app.data.model.Routine
import com.entrenaguay.app.data.model.RoutineExercise
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutineRepository @Inject constructor(
    private val dao: RoutineDao
) {
    val routines: Flow<List<Routine>> = dao.getAll()

    suspend fun getById(id: Long) = dao.getById(id)
    suspend fun insert(routine: Routine) = dao.insert(routine)
    suspend fun update(routine: Routine) = dao.update(routine)
    suspend fun delete(routine: Routine) = dao.delete(routine)
    suspend fun getWithExercises(routineId: Long) = dao.getRoutineExercises(routineId)

    suspend fun addExercise(routineId: Long, exerciseId: Long, order: Int) {
        dao.addExercise(RoutineExercise(routineId = routineId, exerciseId = exerciseId, order = order))
    }

    suspend fun clearExercises(routineId: Long) = dao.clearExercises(routineId)
    suspend fun removeExercise(id: Long) = dao.removeExercise(id)
    suspend fun updateOrder(id: Long, order: Int) = dao.updateOrder(id, order)
    suspend fun getAllList() = dao.getAllList()
}
