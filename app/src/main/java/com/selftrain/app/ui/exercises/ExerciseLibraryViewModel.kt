package com.selftrain.app.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selftrain.app.data.model.Exercise
import com.selftrain.app.data.repository.ExerciseRepository
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

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            exerciseRepo.seedIfEmpty()
            // Wait for Room to emit the initial query result
            exerciseRepo.exercises.first()
            _isLoading.value = false
        }
    }

    fun addExercise(name: String, muscleGroup: String, category: String, isBilboEligible: Boolean, equipment: String) {
        viewModelScope.launch {
            exerciseRepo.addExercise(
                Exercise(
                    name = name.trim(),
                    muscleGroup = muscleGroup,
                    category = category,
                    isBilboEligible = isBilboEligible,
                    equipment = equipment
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
