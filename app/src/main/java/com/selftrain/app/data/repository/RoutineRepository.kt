package com.selftrain.app.data.repository

import com.selftrain.app.data.db.RoutineDao
import androidx.room.withTransaction
import com.selftrain.app.data.model.Routine
import com.selftrain.app.data.model.RoutineExercise
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutineRepository @Inject constructor(
    private val dao: RoutineDao,
    private val db: com.selftrain.app.data.db.AppDatabase
) {
    val routines: Flow<List<Routine>> = dao.getAll()

    suspend fun getById(id: Long) = dao.getById(id)
    suspend fun insert(routine: Routine) = dao.insert(routine)
    suspend fun update(routine: Routine) = dao.update(routine)
    suspend fun delete(routine: Routine) = dao.delete(routine)
    suspend fun getWithExercises(routineId: Long) = dao.getRoutineExercises(routineId)

    suspend fun addExercise(routineId: Long, exerciseId: Long, order: Int, isBilbo: Boolean = false, restSeconds: Int = 0) {
        dao.addExercise(RoutineExercise(routineId = routineId, exerciseId = exerciseId, order = order, isBilbo = isBilbo, restSeconds = restSeconds))
    }

    suspend fun setBilbo(id: Long, isBilbo: Boolean) = dao.setBilbo(id, isBilbo)
    suspend fun setRest(id: Long, restSeconds: Int) = dao.setRest(id, restSeconds)

    // ponytail: guardado atómico de la lista de ejercicios — antes update + clear + N inserts
    // sin transacción: un crash a mitad dejaba la rutina SIN ejercicios
    suspend fun saveRoutineExercises(routine: Routine, items: List<RoutineExercise>) {
        db.withTransaction {
            dao.update(routine)
            dao.clearExercises(routine.id)
            items.forEach { dao.addExercise(it) }
        }
    }

    suspend fun clearExercises(routineId: Long) = dao.clearExercises(routineId)
    suspend fun removeExercise(id: Long) = dao.removeExercise(id)
    suspend fun updateOrder(id: Long, order: Int) = dao.updateOrder(id, order)
    suspend fun getAllList() = dao.getAllList()
}
