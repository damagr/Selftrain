package com.entrenaguay.app.ui.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entrenaguay.app.data.db.RoutineWithExercises
import com.entrenaguay.app.data.model.Exercise
import com.entrenaguay.app.data.model.Routine
import com.entrenaguay.app.data.repository.ExerciseRepository
import com.entrenaguay.app.data.repository.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoutineEditViewModel @Inject constructor(
    private val routineRepo: RoutineRepository,
    private val exerciseRepo: ExerciseRepository
) : ViewModel() {

    private val _routine = MutableStateFlow<Routine?>(null)
    val routine: StateFlow<Routine?> = _routine

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val routineExercises: StateFlow<List<Exercise>> = _exercises

    val allExercises = exerciseRepo.exercises.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadRoutine(routineId: Long) {
        viewModelScope.launch {
            val r = routineRepo.getById(routineId) ?: return@launch
            _routine.value = r
            val reList = routineRepo.getWithExercises(routineId)
            val ids = reList.map { it.exerciseId }
            _exercises.value = if (ids.isNotEmpty()) exerciseRepo.getByIds(ids) else emptyList()
        }
    }

    fun updateName(name: String) {
        _routine.value = _routine.value?.copy(name = name)
    }

    fun addExercise(exercise: Exercise) {
        val current = _exercises.value.toMutableList()
        if (current.none { it.id == exercise.id }) {
            current.add(exercise)
            _exercises.value = current
        }
    }

    fun removeExercise(exercise: Exercise) {
        _exercises.value = _exercises.value.filter { it.id != exercise.id }
    }

    fun moveExercise(fromIndex: Int, toIndex: Int) {
        val list = _exercises.value.toMutableList()
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        _exercises.value = list
    }

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            val r = _routine.value ?: return@launch
            routineRepo.update(r)
            routineRepo.clearExercises(r.id)
            _exercises.value.forEachIndexed { index, ex ->
                routineRepo.addExercise(r.id, ex.id, index)
            }
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
