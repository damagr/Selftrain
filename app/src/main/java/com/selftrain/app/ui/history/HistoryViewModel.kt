package com.selftrain.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selftrain.app.data.db.SetWithExercise
import com.selftrain.app.data.model.Exercise
import com.selftrain.app.data.model.Workout
import com.selftrain.app.data.model.WorkoutSet
import com.selftrain.app.data.repository.ExerciseRepository
import com.selftrain.app.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

enum class HistoryView { SUMMARY, WORKOUT_LIST, WORKOUT_DETAIL }

// ponytail: filtros del historial — por rutina y/o ejercicio
data class HistoryFilter(val routineId: Long? = null, val exerciseId: Long? = null)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val workoutRepo: WorkoutRepository,
    private val exerciseRepo: ExerciseRepository,
    private val routineRepo: com.selftrain.app.data.repository.RoutineRepository
) : ViewModel() {

    val workouts = workoutRepo.workouts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val allExercises = exerciseRepo.exercises.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val routines = routineRepo.routines.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filter state
    private val _filter = MutableStateFlow(HistoryFilter())
    val filter: StateFlow<HistoryFilter> = _filter.asStateFlow()

    fun setFilter(newFilter: HistoryFilter) {
        _filter.value = newFilter
    }

    private val _exerciseIdsWithHistory = MutableStateFlow<List<Long>>(emptyList())
    val exercisesWithHistory: StateFlow<List<Exercise>> = combine(
        allExercises, _exerciseIdsWithHistory
    ) { all, ids -> all.filter { it.id in ids } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Drill-down state
    private val _view = MutableStateFlow(HistoryView.SUMMARY)
    val view: StateFlow<HistoryView> = _view

    private val _selectedWorkout = MutableStateFlow<Workout?>(null)
    val selectedWorkout: StateFlow<Workout?> = _selectedWorkout

    private val _workoutDetailSets = MutableStateFlow<List<SetWithExercise>>(emptyList())
    val workoutDetailSets: StateFlow<List<SetWithExercise>> = _workoutDetailSets

    val completedWorkouts: StateFlow<List<Workout>> = combine(workouts, _filter) { list, f ->
        // ponytail: los ids del ejercicio se resuelven DENTRO del combine (transform suspend) —
        // antes el filtro se aplicaba síncrono y los ids llegaban async → la lista parpadeaba vacía
        val exIds = f.exerciseId?.let { exId ->
            workoutRepo.getSetHistory(exId).map { it.set.workoutId }.toSet()
        } ?: emptySet()
        list.filter { it.completed }
            .filter { w ->
                (f.routineId == null || w.routineId == f.routineId) &&
                    (f.exerciseId == null || w.id in exIds)
            }
            .sortedByDescending { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calendar state
    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    val workoutsByDay: StateFlow<Map<LocalDate, List<Workout>>> = completedWorkouts.map { list ->
        list.groupBy { w ->
            Instant.ofEpochMilli(w.date).atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun nextMonth() { _currentMonth.value = _currentMonth.value.plusMonths(1) }
    fun previousMonth() { _currentMonth.value = _currentMonth.value.minusMonths(1) }
    fun goToToday() { _currentMonth.value = YearMonth.now() }

    // Exercise progression
    private val _selectedExercise = MutableStateFlow<Exercise?>(null)
    val selectedExercise: StateFlow<Exercise?> = _selectedExercise

    private val _history = MutableStateFlow<List<SetWithExercise>>(emptyList())
    val history: StateFlow<List<SetWithExercise>> = _history

    private val _max1RM = MutableStateFlow<Float>(0f)
    val max1RM: StateFlow<Float> = _max1RM

    init {
        viewModelScope.launch {
            _exerciseIdsWithHistory.value = workoutRepo.getExerciseIdsWithHistory()
        }
    }

    fun selectExercise(exercise: Exercise) {
        _selectedExercise.value = exercise
        _view.value = HistoryView.SUMMARY
        viewModelScope.launch {
            _history.value = workoutRepo.getSetHistory(exercise.id)
            val rm = workoutRepo.getMax1RM(exercise.id) ?: 0.0
            _max1RM.value = rm.toFloat()
        }
    }

    fun clearSelection() { _selectedExercise.value = null; _view.value = HistoryView.SUMMARY }

    fun showWorkoutList() { _view.value = HistoryView.WORKOUT_LIST }

    fun selectWorkout(workout: Workout) {
        _selectedWorkout.value = workout
        _view.value = HistoryView.WORKOUT_DETAIL
        viewModelScope.launch {
            _workoutDetailSets.value = workoutRepo.getSetsWithExercise(workout.id)
        }
    }

    fun backFromDetail() {
        if (_view.value == HistoryView.WORKOUT_DETAIL) {
            _view.value = HistoryView.WORKOUT_LIST
        } else {
            _view.value = HistoryView.SUMMARY
        }
    }

    fun deleteWorkout(workout: Workout) {
        viewModelScope.launch {
            workoutRepo.deleteWorkoutById(workout.id)
            _selectedWorkout.value = null
            _view.value = HistoryView.WORKOUT_LIST
        }
    }

    // ponytail: edit history — update/delete/add sets from workout detail
    fun updateSet(set: WorkoutSet, setType: String, reps: Int, weightKg: Float, rir: Int, notes: String) {
        viewModelScope.launch {
            workoutRepo.updateSet(set.copy(setType = setType, reps = reps, weightKg = weightKg, rir = rir, notes = notes))
            val workout = _selectedWorkout.value ?: return@launch
            _workoutDetailSets.value = workoutRepo.getSetsWithExercise(workout.id)
        }
    }

    fun deleteSetFromHistory(set: WorkoutSet) {
        viewModelScope.launch {
            workoutRepo.deleteSet(set)
            val workout = _selectedWorkout.value ?: return@launch
            _workoutDetailSets.value = workoutRepo.getSetsWithExercise(workout.id)
        }
    }

    fun addSetToHistory(exerciseId: Long, setType: String, reps: Int, weightKg: Float, rir: Int) {
        viewModelScope.launch {
            val workout = _selectedWorkout.value ?: return@launch
            val set = WorkoutSet(
                workoutId = workout.id,
                exerciseId = exerciseId,
                setType = setType,
                reps = reps,
                weightKg = weightKg,
                rir = rir,
                explosive = false
            )
            workoutRepo.insertSet(set)
            _workoutDetailSets.value = workoutRepo.getSetsWithExercise(workout.id)
        }
    }

    fun getExercisesInWorkout(): List<Long> {
        return _workoutDetailSets.value.map { it.set.exerciseId }.distinct()
    }

    // ponytail: editar la fecha de un entreno del historial (conserva la hora del día original)
    fun updateWorkoutDate(workout: Workout, newDateMillis: Long) {
        viewModelScope.launch {
            workoutRepo.update(workout.copy(date = newDateMillis))
            _selectedWorkout.value = workout.copy(date = newDateMillis)
        }
    }

    // ponytail: add a missed exercise to an already-completed workout
    fun addExerciseToWorkout(exerciseId: Long) {
        viewModelScope.launch {
            val workout = _selectedWorkout.value ?: return@launch
            val set = WorkoutSet(
                workoutId = workout.id,
                exerciseId = exerciseId,
                setType = "bilbo",
                reps = 0,
                weightKg = 0f,
                rir = 0,
                explosive = false
            )
            workoutRepo.insertSet(set)
            _workoutDetailSets.value = workoutRepo.getSetsWithExercise(workout.id)
        }
    }

    fun getAllExercises(): StateFlow<List<Exercise>> = allExercises
}
