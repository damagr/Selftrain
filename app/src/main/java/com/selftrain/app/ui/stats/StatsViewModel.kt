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

// ponytail: stats — reps de trabajo por semana: conseguidas vs objetivo (rango 8-12 por serie).
// Las series Bilbo se excluyen: su objetivo es la progresión a 50 reps, no el rango 8-12.
data class WeekReps(
    val weekStart: LocalDate,   // lunes de esa semana
    val achievedReps: Int,
    val targetMinReps: Int,     // series × 8
    val targetMaxReps: Int,     // series × 12
    val workoutCount: Int
)

// ponytail: ventana de la página p sobre el histórico (página 0 = las más recientes).
// pageOfWeeks(all, 0, 8) → las últimas 8; pageOfWeeks(all, 1, 8) → las 8 anteriores...
internal fun pageOfWeeks(all: List<WeekReps>, page: Int, pageSize: Int): List<WeekReps> {
    val p = page.coerceAtLeast(0)
    val end = (all.size - p * pageSize).coerceAtLeast(0)
    val start = (end - pageSize).coerceAtLeast(0)
    return if (end <= 0) emptyList() else all.subList(start, end)
}

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

    // Reps de trabajo semanales (histórico completo, sin recorte): conseguidas vs objetivo.
    // La paginación por páginas de 8 semanas se hace en la UI con pageOfWeeks().
    val weeklyReps: StateFlow<List<WeekReps>> = workoutRepo.getCompletedSetVolume().map { rows ->
        groupRepsByWeek(rows)
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

    private fun groupRepsByWeek(rows: List<SetVolumeRow>): List<WeekReps> {
        val zone = ZoneId.systemDefault()
        val targetMinPerSet = 8
        val targetMaxPerSet = 12
        return rows.filter { it.setType == "work" }
            .groupBy { row ->
                Instant.ofEpochMilli(row.date).atZone(zone).toLocalDate()
                    .with(DayOfWeek.MONDAY)
            }
            .map { (weekStart, weekRows) ->
                WeekReps(
                    weekStart = weekStart,
                    achievedReps = weekRows.sumOf { it.reps },
                    targetMinReps = weekRows.size * targetMinPerSet,
                    targetMaxReps = weekRows.size * targetMaxPerSet,
                    workoutCount = weekRows.map { it.workoutId }.distinct().size
                )
            }
            .sortedBy { it.weekStart }
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
