package com.selftrain.app.ui.routines

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.selftrain.app.data.model.Routine
import com.selftrain.app.data.model.Exercise
import com.selftrain.app.data.model.Workout
import com.selftrain.app.data.repository.ExerciseRepository
import com.selftrain.app.data.repository.RoutineRepository
import com.selftrain.app.data.repository.WorkoutRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.selftrain.app.util.RoutineShareCodec
import com.selftrain.app.util.SharedDay
import com.selftrain.app.util.SharedExercise
import com.selftrain.app.util.SharedRoutine
import com.selftrain.app.util.findMatchingGifUrl
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

@HiltViewModel
class RoutinesViewModel @Inject constructor(
    app: Application,
    private val routineRepo: RoutineRepository,
    private val exerciseRepo: ExerciseRepository,
    private val workoutRepo: WorkoutRepository
) : AndroidViewModel(app) {

    val routines = routineRepo.routines.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Crash recovery dialog state — survives recomposition
    var showRecoveryDialog by mutableStateOf(false)
    var unfinishedWorkout by mutableStateOf<Workout?>(null)

    init {
        viewModelScope.launch { exerciseRepo.seedIfEmpty() }
        viewModelScope.launch {
            val uw = workoutRepo.getUnfinishedWorkout()
            if (uw != null) {
                unfinishedWorkout = uw
                showRecoveryDialog = true
            }
        }
    }

    fun createRoutine(name: String, method: String = "bilbo", notes: String = "", onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val nextOrder = routineRepo.getAllList().size
            val id = routineRepo.insert(Routine(name = name, method = method, notes = notes, order = nextOrder))
            onCreated(id)
        }
    }

    fun createChildRoutine(parentId: Long, name: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val children = routineRepo.getAllList().filter { it.parentId == parentId }
            val nextOrder = children.size
            val id = routineRepo.insert(Routine(name = name, method = children.firstOrNull()?.method ?: "bilbo", parentId = parentId, order = nextOrder))
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

    // --- QR share ---

    fun buildSharePayload(routineId: Long, onDone: (SharedRoutine?) -> Unit) {
        viewModelScope.launch {
            val routine = routineRepo.getById(routineId) ?: return@launch onDone(null)
            val children = routineRepo.getAllList().filter { it.parentId == routineId }.sortedBy { it.order }
            val days = if (children.isEmpty()) {
                listOf(SharedDay(name = routine.name, exercises = exercisesOf(routineId)))
            } else {
                children.map { SharedDay(name = it.name, exercises = exercisesOf(it.id)) }
            }
            onDone(SharedRoutine(name = routine.name, method = routine.method, notes = routine.notes, days = days))
        }
    }

    private suspend fun exercisesOf(routineId: Long): List<SharedExercise> {
        val links = routineRepo.getWithExercises(routineId) // ordenado por `order`
        if (links.isEmpty()) return emptyList()
        val byId = exerciseRepo.getByIds(links.map { it.exerciseId }).associateBy { it.id }
        return links.mapNotNull { link ->
            byId[link.exerciseId]?.let { ex ->
                SharedExercise(ex.name, ex.muscleGroup, ex.category, ex.isBilboEligible, ex.equipment)
            }
        }
    }

    /** null = ok, String = error para toast */
    fun importSharedRoutine(shared: SharedRoutine, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val existing = exerciseRepo.getAllList().associateBy { it.name }
                val resolved = mutableMapOf<String, Long>()
                var nextOrder = routineRepo.getAllList().size
                val exercises = shared.days.firstOrNull()?.exercises ?: emptyList()
                if (shared.days.size <= 1) {
                    // Rutina suelta
                    val routineId = routineRepo.insert(Routine(name = shared.name, method = shared.method, notes = shared.notes, order = nextOrder++))
                    exercises.forEachIndexed { i, se ->
                        routineRepo.addExercise(routineId, resolveExerciseId(se, existing, resolved), i)
                    }
                } else {
                    // Programa: padre + hijos
                    val parentId = routineRepo.insert(Routine(name = shared.name, method = shared.method, notes = shared.notes, order = nextOrder++))
                    for (day in shared.days) {
                        val childId = routineRepo.insert(Routine(name = day.name, method = shared.method, order = nextOrder++, parentId = parentId))
                        day.exercises.forEachIndexed { i, se ->
                            routineRepo.addExercise(childId, resolveExerciseId(se, existing, resolved), i)
                        }
                    }
                }
                onDone(null)
            } catch (e: Exception) {
                onDone("Error al importar: ${e.message}")
            }
        }
    }

    private suspend fun resolveExerciseId(
        se: SharedExercise,
        existing: Map<String, Exercise>,
        resolved: MutableMap<String, Long>
    ): Long {
        resolved[se.name]?.let { return it }
        existing[se.name]?.let {
            resolved[se.name] = it.id
            return it.id
        }
        // Ejercicio nuevo: se crea en la biblioteca con su gif (mismo lookup que seedIfEmpty)
        val id = exerciseRepo.addExercise(Exercise(
            name = se.name,
            muscleGroup = se.muscleGroup,
            category = se.category,
            isBilboEligible = se.isBilboEligible,
            equipment = se.equipment,
            gifUrl = findMatchingGifUrl(se.name)
        ))
        resolved[se.name] = id
        return id
    }

    // --- estado de escaneo/import (vive aquí para el flujo escanear -> confirmar) ---
    var pendingImport by mutableStateOf<SharedRoutine?>(null)
    var importMessage by mutableStateOf<String?>(null)

    fun onQrScanned(payload: String) {
        val shared = RoutineShareCodec.decode(payload)
        if (shared == null) importMessage = "QR no válido"
        else pendingImport = shared
    }

    fun confirmImport() {
        val shared = pendingImport ?: return
        importSharedRoutine(shared) { error ->
            importMessage = error ?: "Rutina importada"
            pendingImport = null
        }
    }

    fun cancelImport() { pendingImport = null }

    fun clearImportMessage() { importMessage = null }

    // ponytail: crash recovery — check for unfinished workout on app launch
    suspend fun getUnfinishedWorkout() = workoutRepo.getUnfinishedWorkout()

    fun discardUnfinishedWorkout(workoutId: Long) {
        viewModelScope.launch {
            workoutRepo.deleteWorkoutById(workoutId)
        }
    }
}
