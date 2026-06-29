package com.entrenaguay.app.ui.train

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entrenaguay.app.data.db.SetWithExercise
import com.entrenaguay.app.data.model.*
import com.entrenaguay.app.data.repository.ExerciseRepository
import com.entrenaguay.app.data.repository.RoutineRepository
import com.entrenaguay.app.data.repository.WorkoutRepository
import com.entrenaguay.app.util.BilboProgression
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseWithSets(
    val exercise: Exercise,
    val sets: List<WorkoutSet> = emptyList(),
    val previousHistory: List<SetWithExercise> = emptyList(),
    val lastSessionSets: List<SetWithExercise> = emptyList()
)

data class PerExerciseSuggestion(
    val bilboReps: Int = 0,
    val bilboWeight: Float = 0f,
    val workWeight: Float = 0f,
    val workReps: Int = 0,
    val hasHistory: Boolean = false
)

data class TrainState(
    val routine: Routine? = null,
    val exercises: List<ExerciseWithSets> = emptyList(),
    val currentExerciseIndex: Int = 0,
    val workoutId: Long = 0,
    val isFinishing: Boolean = false,
    val suggestions: List<PerExerciseSuggestion> = emptyList()
)

@HiltViewModel
class TrainViewModel @Inject constructor(
    private val routineRepo: RoutineRepository,
    private val workoutRepo: WorkoutRepository,
    private val exerciseRepo: ExerciseRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TrainState())
    val state: StateFlow<TrainState> = _state.asStateFlow()

    fun startWorkout(routineId: Long) {
        viewModelScope.launch {
            val routine = routineRepo.getById(routineId) ?: return@launch
            val reList = routineRepo.getWithExercises(routineId)

            // Get last completed workout for this routine (for PR display)
            val lastCompleted = workoutRepo.getLastCompleted(routineId)

            val exercises = reList.map { re ->
                val ex = exerciseRepo.getById(re.exerciseId) ?: return@launch
                val allHistory = workoutRepo.getSetHistory(ex.id)
                val lastSession = if (lastCompleted != null) {
                    workoutRepo.getSetsForExercise(lastCompleted.id, ex.id)
                        .map { set ->
                            SetWithExercise(set, ex.name, ex.muscleGroup)
                        }
                } else emptyList()

                ExerciseWithSets(
                    exercise = ex,
                    previousHistory = allHistory,
                    lastSessionSets = lastSession
                )
            }

            val suggestions = exercises.map { ex ->
                val history = ex.previousHistory
                if (history.isEmpty()) {
                    PerExerciseSuggestion(hasHistory = false)
                } else {
                    val lastBilbo = history.filter { it.set.setType == "bilbo" }.lastOrNull()
                    val bilboReps = if (lastBilbo != null) BilboProgression.suggestBilboReps(lastBilbo.set.reps) else 15
                    val bilboWeight = if (lastBilbo != null) {
                        if (BilboProgression.shouldIncreaseBilboWeight(lastBilbo.set.reps))
                            BilboProgression.increasedBilboWeight(lastBilbo.set.weightKg)
                        else lastBilbo.set.weightKg
                    } else {
                        history.filter { it.set.setType == "work" }.lastOrNull()?.set?.weightKg?.div(1.40f) ?: 0f
                    }
                    PerExerciseSuggestion(
                        bilboReps = bilboReps,
                        bilboWeight = bilboWeight,
                        workWeight = bilboWeight * 1.40f,
                        workReps = 10,
                        hasHistory = true
                    )
                }
            }

            val workout = Workout(routineId = routineId)
            val workoutId = workoutRepo.insert(workout)

            _state.value = TrainState(
                routine = routine,
                exercises = exercises,
                workoutId = workoutId,
                suggestions = suggestions
            )
        }
    }

    fun logSet(exerciseId: Long, setType: String, reps: Int, weightKg: Float, rir: Int, explosive: Boolean) {
        viewModelScope.launch {
            val set = WorkoutSet(
                workoutId = _state.value.workoutId,
                exerciseId = exerciseId,
                setType = setType,
                reps = reps,
                weightKg = weightKg,
                rir = rir,
                explosive = explosive
            )
            workoutRepo.insertSet(set)
            reloadSets(exerciseId)
        }
    }

    fun deleteLastSet(exerciseId: Long) {
        viewModelScope.launch {
            val sets = _state.value.exercises
                .find { it.exercise.id == exerciseId }?.sets ?: return@launch
            val last = sets.lastOrNull() ?: return@launch
            workoutRepo.deleteSet(last)
            reloadSets(exerciseId)
        }
    }

    private suspend fun reloadSets(exerciseId: Long) {
        val sets = workoutRepo.getSetsForExercise(_state.value.workoutId, exerciseId)
        val current = _state.value
        val updated = current.exercises.map {
            if (it.exercise.id == exerciseId) it.copy(sets = sets) else it
        }
        _state.value = current.copy(exercises = updated)
    }

    fun setCurrentExercise(index: Int) {
        _state.value = _state.value.copy(currentExerciseIndex = index.coerceIn(0, (_state.value.exercises.size - 1).coerceAtLeast(0)))
    }

    fun finishWorkout(onFinish: () -> Unit) {
        viewModelScope.launch {
            val workout = workoutRepo.getById(_state.value.workoutId)
            if (workout != null) {
                workoutRepo.update(workout.copy(completed = true))
            }
            onFinish()
        }
    }
}
