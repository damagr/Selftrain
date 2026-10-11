package com.selftrain.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selftrain.app.data.db.ExercisePrRow
import com.selftrain.app.data.db.SetVolumeRow
import com.selftrain.app.data.model.Exercise
import com.selftrain.app.data.model.Workout
import com.selftrain.app.data.repository.ExerciseRepository
import com.selftrain.app.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

// ponytail: stats — volumen semanal (suma de reps×kg de los entrenos completados de esa semana)
data class WeekVolume(
    val weekStart: LocalDate,   // lunes de esa semana
    val volumeKg: Float,
    val workoutCount: Int
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val workoutRepo: WorkoutRepository,
    private val exerciseRepo: ExerciseRepository
) : ViewModel() {

    val completedWorkouts: StateFlow<List<Workout>> = workoutRepo.workouts.map { list ->
        list.filter { it.completed }.sortedByDescending { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exercises: StateFlow<List<Exercise>> = exerciseRepo.exercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // PRs globales: mejor 1RM de cada ejercicio (Epley), ordenado de mayor a menor
    val personalRecords: StateFlow<List<ExercisePrRow>> = workoutRepo.getMax1RMPerExercise()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Volumen y frecuencia semanal (últimas 12 semanas con datos)
    val weeklyVolume: StateFlow<List<WeekVolume>> = workoutRepo.getCompletedSetVolume().map { rows ->
        groupByWeek(rows).takeLast(12)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Volumen total acumulado (todas las series de entrenos completados)
    val totalVolume: StateFlow<Float> = workoutRepo.getCompletedSetVolume().map { rows ->
        rows.sumOf { (it.weightKg * it.reps).toDouble() }.toFloat()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    // ponytail: último peso corporal registrado (para fuerza relativa 1RM/kg corporal)
    val latestBodyWeight: StateFlow<Float> = completedWorkouts.map { list ->
        list.firstOrNull { it.bodyWeight > 0f }?.bodyWeight ?: 0f
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    // Racha: semanas consecutivas con al menos un entreno, contando desde la semana actual
    // (si aún no has entrenado esta semana, empieza a contar desde la pasada)
    val weeklyStreak: StateFlow<Int> = completedWorkouts.map { list ->
        computeStreak(list)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private fun groupByWeek(rows: List<SetVolumeRow>): List<WeekVolume> {
        val zone = ZoneId.systemDefault()
        return rows.groupBy { row ->
            Instant.ofEpochMilli(row.date).atZone(zone).toLocalDate()
                .with(DayOfWeek.MONDAY)
        }.map { (weekStart, weekRows) ->
            WeekVolume(
                weekStart = weekStart,
                volumeKg = weekRows.sumOf { (it.weightKg * it.reps).toDouble() }.toFloat(),
                workoutCount = weekRows.map { it.workoutId }.distinct().size
            )
        }.sortedBy { it.weekStart }
    }

    private fun computeStreak(completed: List<Workout>): Int {
        if (completed.isEmpty()) return 0
        val zone = ZoneId.systemDefault()
        val thisWeek = LocalDate.now().with(DayOfWeek.MONDAY)
        // ponytail: ignora semanas futuras (posibles vía edición de fecha) — antes rompían la racha
        val weeks = completed.map {
            Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate().with(DayOfWeek.MONDAY)
        }.filter { !it.isAfter(thisWeek) }.distinct().sortedDescending()
        if (weeks.isEmpty()) return 0
        var cursor = if (weeks.first() == thisWeek) weeks.first() else thisWeek.minusWeeks(1)
        var streak = 0
        for (week in weeks) {
            if (week == cursor) {
                streak++
                cursor = cursor.minusWeeks(1)
            } else if (week < cursor) break
        }
        return streak
    }
}
