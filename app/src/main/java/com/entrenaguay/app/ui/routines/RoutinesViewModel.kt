package com.entrenaguay.app.ui.routines

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.entrenaguay.app.data.model.Routine
import com.entrenaguay.app.data.repository.ExerciseRepository
import com.entrenaguay.app.data.repository.RoutineRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PredefinedProgram(
    val program: String,
    val method: String,
    val routines: List<PredefinedRoutineData>
)
data class PredefinedRoutineData(
    val name: String,
    val exercises: List<String>
)

data class RoutineGroup(
    val routine: Routine,           // parent (or standalone if no children)
    val children: List<Routine>,     // empty = standalone
    val expanded: Boolean = false
)

@HiltViewModel
class RoutinesViewModel @Inject constructor(
    app: Application,
    private val routineRepo: RoutineRepository,
    private val exerciseRepo: ExerciseRepository
) : AndroidViewModel(app) {

    val routines = routineRepo.routines.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { exerciseRepo.seedIfEmpty() }
    }

    fun createRoutine(name: String, method: String = "bilbo", notes: String = "", onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val nextOrder = routineRepo.getAllList().size
            val id = routineRepo.insert(Routine(name = name, method = method, notes = notes, order = nextOrder))
            onCreated(id)
        }
    }

    fun deleteRoutine(routine: Routine) {
        viewModelScope.launch {
            // ponytail: also delete children if this is a parent
            val all = routineRepo.getAllList()
            all.filter { it.parentId == routine.id }.forEach { routineRepo.delete(it) }
            routineRepo.delete(routine)
        }
    }

    fun moveRoutine(index: Int, direction: Int, allRoutines: List<Routine>) {
        viewModelScope.launch {
            val targetIndex = index + direction
            if (targetIndex < 0 || targetIndex >= allRoutines.size) return@launch
            val a = allRoutines[index]
            val b = allRoutines[targetIndex]
            routineRepo.updateOrder(a.id, b.order)
            routineRepo.updateOrder(b.id, a.order)
        }
    }

    fun loadPredefinedPrograms(): List<PredefinedProgram> {
        val json = getApplication<Application>().assets.open("predefined_routines.json")
            .bufferedReader().readText()
        return Gson().fromJson(json, object : TypeToken<List<PredefinedProgram>>() {}.type)
    }

    fun createRoutinesFromProgram(program: PredefinedProgram, onDone: () -> Unit) {
        viewModelScope.launch {
            val allExercises = exerciseRepo.getAllList()
            val exerciseMap = allExercises.associateBy { it.name }
            var nextOrder = routineRepo.getAllList().size
            // Create parent
            val parentId = routineRepo.insert(Routine(name = program.program, method = program.method, order = nextOrder++))
            // Create children
            for (routineData in program.routines) {
                val childId = routineRepo.insert(Routine(name = routineData.name, method = program.method, order = nextOrder++, parentId = parentId))
                var exOrder = 0
                for (exerciseName in routineData.exercises) {
                    val exercise = exerciseMap[exerciseName]
                    if (exercise != null) {
                        routineRepo.addExercise(childId, exercise.id, exOrder++)
                    }
                }
            }
            onDone()
        }
    }
}
