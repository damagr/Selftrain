package com.entrenaguay.app.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entrenaguay.app.data.model.Exercise
import com.entrenaguay.app.data.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseLibraryViewModel @Inject constructor(
    private val exerciseRepo: ExerciseRepository
) : ViewModel() {

    val exercises = exerciseRepo.exercises.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        viewModelScope.launch { exerciseRepo.seedIfEmpty() }
    }

    fun addExercise(name: String, muscleGroup: String, category: String, isBilboEligible: Boolean) {
        viewModelScope.launch {
            exerciseRepo.addExercise(
                Exercise(
                    name = name.trim(),
                    muscleGroup = muscleGroup,
                    category = category,
                    isBilboEligible = isBilboEligible
                )
            )
        }
    }

    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch {
            try {
                exerciseRepo.deleteExercise(exercise)
            } catch (e: IllegalStateException) {
                val (routines, sets) = exerciseRepo.getUsageCount(exercise.id)
                _errorMessage.value = "No se puede eliminar: usado en $routines rutinas y $sets entrenos"
            }
        }
    }

    fun clearError() { _errorMessage.value = null }
}
