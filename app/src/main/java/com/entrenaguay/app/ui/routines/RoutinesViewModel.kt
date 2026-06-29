package com.entrenaguay.app.ui.routines

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.entrenaguay.app.data.model.Routine
import com.entrenaguay.app.data.repository.ExerciseRepository
import com.entrenaguay.app.data.repository.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoutinesViewModel @Inject constructor(
    private val routineRepo: RoutineRepository,
    private val exerciseRepo: ExerciseRepository
) : AndroidViewModel(Application()) {

    val routines = routineRepo.routines.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createRoutine(name: String, method: String = "bilbo", notes: String = "", onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = routineRepo.insert(Routine(name = name, method = method, notes = notes))
            onCreated(id)
        }
    }

    fun deleteRoutine(routine: Routine) {
        viewModelScope.launch { routineRepo.delete(routine) }
    }
}
