package com.selftrain.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selftrain.app.data.db.SetWithExercise
import com.selftrain.app.data.model.Exercise
import com.selftrain.app.data.model.Workout
import com.selftrain.app.data.repository.ExerciseRepository
import com.selftrain.app.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HistoryView { SUMMARY, WORKOUT_LIST, WORKOUT_DETAIL }

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val workoutRepo: WorkoutRepository,
    private val exerciseRepo: ExerciseRepository
) : ViewModel() {

    val workouts = workoutRepo.workouts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val allExercises = exerciseRepo.exercises.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    val completedWorkouts: StateFlow<List<Workout>> = workouts.map { list ->
        list.filter { it.completed }.sortedByDescending { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
}
