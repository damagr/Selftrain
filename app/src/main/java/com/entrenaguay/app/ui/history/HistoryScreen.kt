package com.entrenaguay.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.entrenaguay.app.data.model.Exercise
import com.entrenaguay.app.data.model.Workout
import com.entrenaguay.app.util.Labels
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onSettings: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val view by viewModel.view.collectAsState()
    val workouts by viewModel.workouts.collectAsState()
    val exercises by viewModel.exercisesWithHistory.collectAsState()
    val selectedExercise by viewModel.selectedExercise.collectAsState()
    val history by viewModel.history.collectAsState()
    val max1RM by viewModel.max1RM.collectAsState()
    val completedWorkouts by viewModel.completedWorkouts.collectAsState()
    val selectedWorkout by viewModel.selectedWorkout.collectAsState()
    val workoutDetailSets by viewModel.workoutDetailSets.collectAsState()

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(when (view) {
                        HistoryView.SUMMARY -> if (selectedExercise != null) "Progreso: ${selectedExercise!!.name}" else "Historial"
                        HistoryView.WORKOUT_LIST -> "Entrenos realizados"
                        HistoryView.WORKOUT_DETAIL -> selectedWorkout?.let { w ->
                            dateFormat.format(Date(w.date))
                        } ?: "Detalle"
                    })
                },
                navigationIcon = {
                    if (selectedExercise != null && view == HistoryView.SUMMARY) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.ArrowBack, "Volver")
                        }
                    } else if (view != HistoryView.SUMMARY) {
                        IconButton(onClick = { viewModel.backFromDetail() }) {
                            Icon(Icons.Default.ArrowBack, "Volver")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, "Ajustes")
                    }
                }
            )
        }
    ) { padding ->
        when (view) {
            HistoryView.SUMMARY -> {
                if (selectedExercise != null) {
                    ExerciseProgressionView(
                        exercise = selectedExercise!!,
                        history = history,
                        max1RM = max1RM,
                        modifier = Modifier.padding(padding)
                    )
                } else {
                    Column(Modifier.padding(padding)) {
                        Card(
                            Modifier.fillMaxWidth().padding(16.dp).clickable { viewModel.showWorkoutList() }
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Resumen", style = MaterialTheme.typography.titleMedium)
                                Text("${completedWorkouts.size} entrenos completados",
                                    style = MaterialTheme.typography.bodyLarge)
                                Text("Toca para ver la lista", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Text("Progreso por ejercicio",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp))

                        if (exercises.isEmpty()) {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Completa tu primer entreno para ver el progreso",
                                    style = MaterialTheme.typography.bodyLarge)
                            }
                        } else {
                            LazyColumn {
                                items(exercises) { ex ->
                                    ListItem(
                                        headlineContent = { Text(ex.name) },
                                        supportingContent = { Text(Labels.muscleGroup(ex.muscleGroup)) },
                                        modifier = Modifier.clickable { viewModel.selectExercise(ex) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HistoryView.WORKOUT_LIST -> {
                LazyColumn(Modifier.padding(padding)) {
                    if (completedWorkouts.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No hay entrenos completados aun",
                                    style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                    items(completedWorkouts) { workout ->
                        Card(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable { viewModel.selectWorkout(workout) }
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(dateFormat.format(Date(workout.date)),
                                    style = MaterialTheme.typography.titleMedium)
                                if (workout.notes.isNotBlank()) {
                                    Text(workout.notes, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            HistoryView.WORKOUT_DETAIL -> {
                LazyColumn(Modifier.padding(padding)) {
                    items(workoutDetailSets) { s ->
                        Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(s.exerciseName, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${if (s.set.setType == "bilbo") "Bilbo" else "Trabajo"}: ${s.set.reps} reps \u00D7 ${String.format("%.1f", s.set.weightKg)} kg" +
                                        if (s.set.rir > 0) " (RIR ${s.set.rir})" else "",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseProgressionView(
    exercise: Exercise,
    history: List<com.entrenaguay.app.data.db.SetWithExercise>,
    max1RM: Float,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier.padding(16.dp)) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, null, tint = Color(0xFFFFC107))
                        Spacer(Modifier.width(8.dp))
                        Text("1RM estimado max: ${String.format("%.1f", max1RM)} kg",
                            style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (history.isEmpty()) {
            item {
                Text("Sin datos aun. Haz tu primer entreno!",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp))
            }
        } else {
            item {
                Text("Historial de series", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
            }

            history.reversed().forEach { s ->
                item {
                    Card(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Row(Modifier.padding(12.dp)) {
                            val label = if (s.set.setType == "bilbo") "Bilbo" else "Trabajo"
                            Text("$label: ${s.set.reps} reps \u00D7 ${String.format("%.1f", s.set.weightKg)} kg" +
                                if (s.set.rir > 0) " (RIR ${s.set.rir})" else "",
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
