package com.selftrain.app.ui.train

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selftrain.app.data.db.SetWithExercise
import com.selftrain.app.data.model.*
import com.selftrain.app.data.repository.ExerciseRepository
import com.selftrain.app.data.repository.RoutineRepository
import com.selftrain.app.data.repository.WorkoutRepository
import com.selftrain.app.util.BilboProgression
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
    val hasHistory: Boolean = false,
    val workProgression: BilboProgression.WorkProgression = BilboProgression.WorkProgression.MAINTAIN
)

data class TrainState(
    val routine: Routine? = null,
    val exercises: List<ExerciseWithSets> = emptyList(),
    val currentExerciseIndex: Int = 0,
    val workoutId: Long = 0,
    val isFinishing: Boolean = false,
    val suggestions: List<PerExerciseSuggestion> = emptyList(),
    val workoutSummary: WorkoutSummary? = null,
    val confirmEmpty: Boolean = false,
    val confirmFinish: Boolean = false
)

data class MuscleGroupVolume(
    val muscleGroup: String,
    val totalKg: Float,
    val setCount: Int
)

data class PersonalRecord(
    val exerciseName: String,
    val muscleGroup: String,
    val newRecord: Double,
    val previousBest: Double
)

data class WeeklyComparison(
    val muscleGroup: String,
    val thisWorkoutKg: Float,
    val lastWeekKg: Float
)

data class WorkoutSummary(
    val durationMinutes: Int,
    val volumeByMuscle: List<MuscleGroupVolume>,
    val personalRecords: List<PersonalRecord>,
    val weeklyComparison: List<WeeklyComparison>
)

@HiltViewModel
class TrainViewModel @Inject constructor(
    private val routineRepo: RoutineRepository,
    private val workoutRepo: WorkoutRepository,
    private val exerciseRepo: ExerciseRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TrainState())
    val state: StateFlow<TrainState> = _state.asStateFlow()

    fun startWorkout(routineId: Long, existingWorkoutId: Long? = null) {
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
                            BilboProgression.increasedBilboWeight(lastBilbo.set.weightKg, lastBilbo.set.rir)
                        else lastBilbo.set.weightKg
                    } else {
                        history.filter { it.set.setType == "work" }.lastOrNull()?.set?.weightKg?.div(1.40f) ?: 0f
                    }
                    // ponytail: pre-fill work weight with max from last session, not bilbo-derived
                    val lastWorkSets = ex.lastSessionSets.filter { it.set.setType == "work" }
                    val workWeightFromLastSession = lastWorkSets.maxOfOrNull { it.set.weightKg }
                    val baseWorkWeight = workWeightFromLastSession ?: (bilboWeight * 1.40f)

                    // Progression: <8 reps → decrease, >12 reps → increase
                    val lastWorkReps = lastWorkSets.map { it.set.reps }
                    val progression = BilboProgression.workProgression(lastWorkReps)
                    val adjustedWorkWeight = when (progression) {
                        BilboProgression.WorkProgression.INCREASE -> baseWorkWeight * 1.05f
                        BilboProgression.WorkProgression.DECREASE -> baseWorkWeight * 0.90f
                        BilboProgression.WorkProgression.MAINTAIN -> baseWorkWeight
                    }
                    PerExerciseSuggestion(
                        bilboReps = bilboReps,
                        bilboWeight = bilboWeight,
                        workWeight = adjustedWorkWeight,
                        workReps = 10,
                        hasHistory = true,
                        workProgression = progression
                    )
                }
            }

            // Resume existing workout or create new one
            val workoutId: Long
            if (existingWorkoutId != null) {
                // Resume: load existing sets
                val existingWorkout = workoutRepo.getById(existingWorkoutId) ?: return@launch
                val existingSets = workoutRepo.getSetsWithExercise(existingWorkoutId)
                val loadedExercises = exercises.map { ex ->
                    val exSets = existingSets.filter { it.set.exerciseId == ex.exercise.id }
                        .map { it.set }
                    ex.copy(sets = exSets)
                }
                val startIdx = existingWorkout.lastExerciseIndex.coerceIn(0, (loadedExercises.size - 1).coerceAtLeast(0))
                _state.value = TrainState(
                    routine = routine,
                    exercises = loadedExercises,
                    workoutId = existingWorkoutId,
                    suggestions = suggestions,
                    currentExerciseIndex = startIdx
                )
                return@launch
            } else {
                val workout = Workout(routineId = routineId)
                workoutId = workoutRepo.insert(workout)
            }

            _state.value = TrainState(
                routine = routine,
                exercises = exercises,
                workoutId = workoutId,
                suggestions = suggestions
            )
        }
    }

    private fun saveExerciseIndex(index: Int) {
        viewModelScope.launch {
            val workout = workoutRepo.getById(_state.value.workoutId) ?: return@launch
            workoutRepo.update(workout.copy(lastExerciseIndex = index))
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
        val clamped = index.coerceIn(0, (_state.value.exercises.size - 1).coerceAtLeast(0))
        _state.value = _state.value.copy(currentExerciseIndex = clamped)
        saveExerciseIndex(clamped)
    }

    fun showConfirmFinish() {
        _state.value = _state.value.copy(confirmFinish = true)
    }

    fun cancelConfirmFinish() {
        _state.value = _state.value.copy(confirmFinish = false)
    }

    fun finishWorkout() {
        viewModelScope.launch {
            val current = _state.value
            // Check if any sets were logged
            val hasAnySets = current.exercises.any { it.sets.isNotEmpty() }
            if (!hasAnySets) {
                // ponytail: empty workout → ask confirmation, don't register
                _state.value = current.copy(confirmEmpty = true)
                return@launch
            }

            val workout = workoutRepo.getById(current.workoutId) ?: return@launch
            val endDate = System.currentTimeMillis()
            val durationMinutes = ((endDate - workout.date) / 60000).toInt().coerceAtLeast(0)
            val updatedWorkout = workout.copy(completed = true, endDate = endDate, durationMinutes = durationMinutes)
            workoutRepo.update(updatedWorkout)

            val summary = computeWorkoutSummary(current.workoutId, durationMinutes, workout.date)
            _state.value = current.copy(isFinishing = true, workoutSummary = summary)
        }
    }

    fun discardEmptyWorkout() {
        viewModelScope.launch {
            val workoutId = _state.value.workoutId
            val workout = workoutRepo.getById(workoutId)
            if (workout != null) {
                workoutRepo.delete(workout)
            }
            _state.value = _state.value.copy(confirmEmpty = false)
        }
    }

    // ponytail: discard without confirmEmpty flag, used by back-handler
    fun discardWorkout() {
        viewModelScope.launch {
            val workoutId = _state.value.workoutId
            val workout = workoutRepo.getById(workoutId)
            if (workout != null) {
                workoutRepo.delete(workout)
            }
        }
    }

    fun cancelEmptyFinish() {
        _state.value = _state.value.copy(confirmEmpty = false)
    }

    fun updateDuration(minutes: Int) {
        viewModelScope.launch {
            val summary = _state.value.workoutSummary?.copy(durationMinutes = minutes)
            _state.value = _state.value.copy(workoutSummary = summary)
            val workout = workoutRepo.getById(_state.value.workoutId)
            if (workout != null) {
                workoutRepo.update(workout.copy(durationMinutes = minutes))
            }
        }
    }

    fun dismissSummary() {
        _state.value = _state.value.copy(isFinishing = false, workoutSummary = null)
    }

    private suspend fun computeWorkoutSummary(workoutId: Long, durationMinutes: Int, workoutDate: Long): WorkoutSummary {
        // 1. Volume per muscle group
        val allSets = workoutRepo.getSetsWithExercise(workoutId)
        val volumeByMuscle = allSets
            .groupBy { it.muscleGroup }
            .map { (group, sets) ->
                val totalKg = sets.sumOf { (it.set.reps * it.set.weightKg).toDouble() }.toFloat()
                MuscleGroupVolume(muscleGroup = group, totalKg = totalKg, setCount = sets.size)
            }
            .sortedByDescending { it.totalKg }

        // 2. Personal records
        val exercisesInWorkout = allSets
            .groupBy { it.set.exerciseId }
            .map { (exId, sets) -> exId to sets }
        val personalRecords = exercisesInWorkout.mapNotNull { (exId, sets) ->
            val exerciseData = _state.value.exercises.find { it.exercise.id == exId } ?: return@mapNotNull null
            val currentMax = sets.maxOfOrNull {
                BilboProgression.estimated1RM(it.set.weightKg, it.set.reps).toDouble()
            } ?: return@mapNotNull null

            // Get history excluding the current workout
            val previousHistory = exerciseData.previousHistory
                .filter { it.set.workoutId != workoutId }
            if (previousHistory.isEmpty()) return@mapNotNull null

            val previousMax = previousHistory.maxOfOrNull {
                BilboProgression.estimated1RM(it.set.weightKg, it.set.reps).toDouble()
            } ?: return@mapNotNull null

            if (currentMax > previousMax) {
                PersonalRecord(
                    exerciseName = exerciseData.exercise.name,
                    muscleGroup = exerciseData.exercise.muscleGroup,
                    newRecord = currentMax,
                    previousBest = previousMax
                )
            } else null
        }

        // 3. Weekly comparison
        val weekAgo = workoutDate - 7L * 24 * 60 * 60 * 1000
        val pastWorkouts = workoutRepo.getCompletedWorkoutsBetween(weekAgo, workoutDate - 1)
        val lastWeekVolumeByMuscle = mutableMapOf<String, Float>()
        for (pw in pastWorkouts) {
            val pwSets = workoutRepo.getSetsWithExercise(pw.id)
            pwSets.groupBy { it.muscleGroup }.forEach { (group, sets) ->
                val kg = sets.sumOf { (it.set.reps * it.set.weightKg).toDouble() }.toFloat()
                lastWeekVolumeByMuscle[group] = (lastWeekVolumeByMuscle[group] ?: 0f) + kg
            }
        }
        val weeklyComparison = volumeByMuscle.map { mgv ->
            WeeklyComparison(
                muscleGroup = mgv.muscleGroup,
                thisWorkoutKg = mgv.totalKg,
                lastWeekKg = lastWeekVolumeByMuscle[mgv.muscleGroup] ?: 0f
            )
        }

        return WorkoutSummary(
            durationMinutes = durationMinutes,
            volumeByMuscle = volumeByMuscle,
            personalRecords = personalRecords,
            weeklyComparison = weeklyComparison
        )
    }
}
