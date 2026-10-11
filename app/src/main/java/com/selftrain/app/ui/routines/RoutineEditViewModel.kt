package com.selftrain.app.ui.routines

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selftrain.app.data.model.Exercise
import com.selftrain.app.data.model.Routine
import com.selftrain.app.data.model.RoutineExercise
import com.selftrain.app.data.repository.ExerciseRepository
import com.selftrain.app.data.repository.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoutineExerciseItem(
    val exercise: Exercise,
    val isBilbo: Boolean = false,
    val routineExerciseId: Long = 0,  // ponytail: id en routine_exercises (para setRest)
    val restSeconds: Int = 0          // ponytail: 0 = default por categoría
)

@HiltViewModel
class RoutineEditViewModel @Inject constructor(
    private val routineRepo: RoutineRepository,
    private val exerciseRepo: ExerciseRepository
) : ViewModel() {

    private val _routine = MutableStateFlow<Routine?>(null)
    val routine: StateFlow<Routine?> = _routine

    private val _exercises = MutableStateFlow<List<RoutineExerciseItem>>(emptyList())
    val routineExercises: StateFlow<List<RoutineExerciseItem>> = _exercises

    val allExercises = exerciseRepo.exercises.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Mensajes de validación mostrados como toast en la UI
    var message by mutableStateOf<String?>(null)
    fun clearMessage() { message = null }

    fun loadRoutine(routineId: Long) {
        viewModelScope.launch {
            val r = routineRepo.getById(routineId) ?: return@launch
            _routine.value = r
            val reList = routineRepo.getWithExercises(routineId)
            val ids = reList.map { it.exerciseId }
            val byId = if (ids.isNotEmpty()) exerciseRepo.getByIds(ids).associateBy { it.id } else emptyMap()
            val items = reList.mapNotNull { re ->
                byId[re.exerciseId]?.let { RoutineExerciseItem(it, re.isBilbo, re.id, re.restSeconds) }
            }.toMutableList()
            // Backfill: rutinas Bilbo antiguas sin marca → primera elegible (mínimo 1 Serie Bilbo)
            if (r.method.equals("bilbo", true) && items.none { it.isBilbo }) {
                val first = items.firstOrNull { it.exercise.isBilboEligible }
                if (first != null) {
                    val idx = items.indexOfFirst { it.exercise.id == first.exercise.id }
                    items[idx] = items[idx].copy(isBilbo = true)
                    reList.firstOrNull { it.exerciseId == first.exercise.id }?.let {
                        routineRepo.setBilbo(it.id, true)
                    }
                }
            }
            _exercises.value = items
        }
    }

    fun updateName(name: String) {
        _routine.value = _routine.value?.copy(name = name)
    }

    fun addExercise(exercise: Exercise) {
        val current = _exercises.value.toMutableList()
        if (current.none { it.exercise.id == exercise.id }) {
            current.add(RoutineExerciseItem(exercise))
            _exercises.value = current
        }
    }

    fun removeExercise(item: RoutineExerciseItem) {
        _exercises.value = _exercises.value.filter { it.exercise.id != item.exercise.id }
    }

    fun replaceExercise(index: Int, newExercise: Exercise) {
        val list = _exercises.value.toMutableList()
        if (index !in list.indices) return
        val previous = list[index]
        // Conserva la marca solo si el nuevo es elegible y no choca con el mismo grupo muscular
        val keepBilbo = previous.isBilbo && newExercise.isBilboEligible &&
            list.none {
                it.isBilbo && it.exercise.muscleGroup == newExercise.muscleGroup && it.exercise.id != newExercise.id
            }
        // ponytail: conserva el descanso configurado del slot (antes se perdía silenciosamente)
        list[index] = RoutineExerciseItem(newExercise, keepBilbo, previous.routineExerciseId, previous.restSeconds)
        _exercises.value = list
    }

    fun moveExercise(fromIndex: Int, toIndex: Int) {
        val list = _exercises.value.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        _exercises.value = list
    }

    // ponytail: solo estado local — persiste en Guardar (save lleva restSeconds).
    // Antes escribía en BD al instante: salir sin Guardar perdía los demás cambios
    // pero el descanso quedaba guardado (inconsistente con el modelo save/cancel).
    fun setRest(item: RoutineExerciseItem, seconds: Int) {
        val clamped = seconds.coerceIn(0, 300)   // 0 = auto; máx 5 min
        _exercises.value = _exercises.value.map {
            if (it.exercise.id == item.exercise.id) it.copy(restSeconds = clamped) else it
        }
    }

    /** Máximo 1 Serie Bilbo por grupo muscular dentro de la misma sesión. */
    fun toggleBilbo(item: RoutineExerciseItem) {
        val list = _exercises.value.toMutableList()
        val idx = list.indexOfFirst { it.exercise.id == item.exercise.id }
        if (idx < 0) return
        if (list[idx].isBilbo) {
            list[idx] = list[idx].copy(isBilbo = false)
        } else {
            val conflict = list.any {
                it.isBilbo && it.exercise.muscleGroup == item.exercise.muscleGroup && it.exercise.id != item.exercise.id
            }
            if (conflict) {
                message = "Solo una Serie Bilbo por grupo muscular y sesión"
                return
            }
            list[idx] = list[idx].copy(isBilbo = true)
        }
        _exercises.value = list
    }

    fun save(onSaved: () -> Unit) {
        val r = _routine.value ?: return
        val items = _exercises.value
        if (r.method.equals("bilbo", true) && items.none { it.isBilbo }) {
            message = "Marca al menos un ejercicio como Serie Bilbo"
            return
        }
        viewModelScope.launch {
            // ponytail: transaccional en el repo (antes update+clear+N inserts sin transacción)
            routineRepo.saveRoutineExercises(
                r,
                items.mapIndexed { index, item ->
                    RoutineExercise(
                        routineId = r.id,
                        exerciseId = item.exercise.id,
                        order = index,
                        isBilbo = item.isBilbo,
                        restSeconds = item.restSeconds
                    )
                }
            )
            onSaved()
        }
    }

    fun deleteRoutine(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val r = _routine.value ?: return@launch
            routineRepo.delete(r)
            onDeleted()
        }
    }
}
