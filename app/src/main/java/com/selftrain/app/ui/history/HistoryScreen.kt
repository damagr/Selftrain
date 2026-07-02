package com.selftrain.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.selftrain.app.data.model.Exercise
import com.selftrain.app.data.model.Workout
import com.selftrain.app.data.db.SetWithExercise
import com.selftrain.app.data.model.WorkoutSet
import com.selftrain.app.util.Labels
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
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm2 by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal),
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
                    if (view == HistoryView.WORKOUT_DETAIL) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, "Eliminar entrenamiento",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
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
                val grouped = remember(workoutDetailSets) {
                    workoutDetailSets.groupBy { it.exerciseName }
                }
                LazyColumn(Modifier.padding(padding)) {
                    grouped.forEach { (exerciseName, sets) ->
                        val mg = sets.firstOrNull()?.muscleGroup ?: ""
                        val firstExerciseId = sets.firstOrNull()?.set?.exerciseId ?: 0L
                        item(key = "ex-$exerciseName") {
                            Card(
                                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.FitnessCenter, null,
                                            Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                        Spacer(Modifier.width(8.dp))
                                        Text(exerciseName, style = MaterialTheme.typography.titleMedium)
                                    }
                                    Text("${Labels.muscleGroup(mg)} · ${sets.size} series",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        items(sets, key = { "set-${it.set.id}" }) { s ->
                            var showEditDialog by remember { mutableStateOf(false) }

                            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)
                                .clickable { showEditDialog = true }
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "${if (s.set.setType == "bilbo") "Bilbo" else "Trabajo"}: ${s.set.reps} reps \u00D7 ${String.format("%.1f", s.set.weightKg)} kg" +
                                                if (s.set.rir > 0) " (RIR ${s.set.rir})" else "",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    IconButton(onClick = {
                                        viewModel.deleteSetFromHistory(s.set)
                                    }) {
                                        Icon(Icons.Default.Delete, "Borrar",
                                            Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.error)
        }
    }
}

// ponytail: inline edit dialogs for history workout detail

@Composable
fun EditSetDialog(
    set: WorkoutSet,
    onDismiss: () -> Unit,
    onSave: (reps: Int, weight: Float, rir: Int) -> Unit
) {
    var reps by remember { mutableStateOf(set.reps.toString()) }
    var weight by remember { mutableStateOf(String.format("%.1f", set.weightKg)) }
    var rir by remember { mutableStateOf(if (set.rir > 0) set.rir.toString() else "") }
    var setType by remember { mutableStateOf(set.setType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar serie") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = setType == "bilbo",
                        onClick = { setType = "bilbo" },
                        label = { Text("Bilbo") }
                    )
                    FilterChip(
                        selected = setType == "work",
                        onClick = { setType = "work" },
                        label = { Text("Trabajo") }
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it.filter { c -> c.isDigit() } },
                        label = { Text("Reps") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Peso (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = rir,
                    onValueChange = { rir = it.filter { c -> c.isDigit() } },
                    label = { Text("RIR") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val r = reps.toIntOrNull() ?: return@TextButton
                val w = weight.toFloatOrNull() ?: return@TextButton
                val ri = rir.toIntOrNull() ?: 0
                onSave(r, w, ri)
            }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun AddSetDialog(
    onDismiss: () -> Unit,
    onAdd: (setType: String, reps: Int, weight: Float, rir: Int) -> Unit
) {
    var reps by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var rir by remember { mutableStateOf("") }
    var setType by remember { mutableStateOf("work") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir serie") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = setType == "bilbo",
                        onClick = { setType = "bilbo" },
                        label = { Text("Bilbo") }
                    )
                    FilterChip(
                        selected = setType == "work",
                        onClick = { setType = "work" },
                        label = { Text("Trabajo") }
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it.filter { c -> c.isDigit() } },
                        label = { Text("Reps") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Peso (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = rir,
                    onValueChange = { rir = it.filter { c -> c.isDigit() } },
                    label = { Text("RIR") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val r = reps.toIntOrNull() ?: return@TextButton
                val w = weight.toFloatOrNull() ?: return@TextButton
                val ri = rir.toIntOrNull() ?: 0
                onAdd(setType, r, w, ri)
            }) { Text("Añadir") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

                            // Edit dialog
                            if (showEditDialog) {
                                EditSetDialog(
                                    set = s.set,
                                    onDismiss = { showEditDialog = false },
                                    onSave = { reps, weight, rir ->
                                        viewModel.updateSet(s.set, reps, weight, rir)
                                        showEditDialog = false
                                    }
                                )
                            }
                        }

                        // Add set button
                        item(key = "add-$exerciseName") {
                            var showAddDialog by remember { mutableStateOf(false) }
                            TextButton(
                                onClick = { showAddDialog = true },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                            ) {
                                Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Añadir serie")
                            }
                            if (showAddDialog) {
                                AddSetDialog(
                                    onDismiss = { showAddDialog = false },
                                    onAdd = { setType, reps, weight, rir ->
                                        viewModel.addSetToHistory(firstExerciseId, setType, reps, weight, rir)
                                        showAddDialog = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete workout — double confirmation
    if (showDeleteConfirm && selectedWorkout != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Eliminar entrenamiento?") },
            text = { Text("Se borrarán todas las series registradas en este entrenamiento.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    showDeleteConfirm2 = true
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
            }
        )
    }
    if (showDeleteConfirm2 && selectedWorkout != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm2 = false },
            title = { Text("¿Estás seguro?") },
            text = { Text("Esta acción es irreversible. No podrás recuperar este entrenamiento.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm2 = false
                    viewModel.deleteWorkout(selectedWorkout!!)
                }) { Text("Sí, eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm2 = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun ExerciseProgressionView(
    exercise: Exercise,
    history: List<com.selftrain.app.data.db.SetWithExercise>,
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

// ponytail: inline edit dialogs for history workout detail

@Composable
fun EditSetDialog(
    set: WorkoutSet,
    onDismiss: () -> Unit,
    onSave: (reps: Int, weight: Float, rir: Int) -> Unit
) {
    var reps by remember { mutableStateOf(set.reps.toString()) }
    var weight by remember { mutableStateOf(String.format("%.1f", set.weightKg)) }
    var rir by remember { mutableStateOf(if (set.rir > 0) set.rir.toString() else "") }
    var setType by remember { mutableStateOf(set.setType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar serie") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = setType == "bilbo",
                        onClick = { setType = "bilbo" },
                        label = { Text("Bilbo") }
                    )
                    FilterChip(
                        selected = setType == "work",
                        onClick = { setType = "work" },
                        label = { Text("Trabajo") }
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it.filter { c -> c.isDigit() } },
                        label = { Text("Reps") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Peso (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = rir,
                    onValueChange = { rir = it.filter { c -> c.isDigit() } },
                    label = { Text("RIR") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val r = reps.toIntOrNull() ?: return@TextButton
                val w = weight.toFloatOrNull() ?: return@TextButton
                val ri = rir.toIntOrNull() ?: 0
                onSave(r, w, ri)
            }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun AddSetDialog(
    onDismiss: () -> Unit,
    onAdd: (setType: String, reps: Int, weight: Float, rir: Int) -> Unit
) {
    var reps by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var rir by remember { mutableStateOf("") }
    var setType by remember { mutableStateOf("work") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir serie") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = setType == "bilbo",
                        onClick = { setType = "bilbo" },
                        label = { Text("Bilbo") }
                    )
                    FilterChip(
                        selected = setType == "work",
                        onClick = { setType = "work" },
                        label = { Text("Trabajo") }
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it.filter { c -> c.isDigit() } },
                        label = { Text("Reps") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Peso (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = rir,
                    onValueChange = { rir = it.filter { c -> c.isDigit() } },
                    label = { Text("RIR") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val r = reps.toIntOrNull() ?: return@TextButton
                val w = weight.toFloatOrNull() ?: return@TextButton
                val ri = rir.toIntOrNull() ?: 0
                onAdd(setType, r, w, ri)
            }) { Text("Añadir") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
