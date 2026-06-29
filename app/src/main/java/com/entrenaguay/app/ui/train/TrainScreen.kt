package com.entrenaguay.app.ui.train

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.entrenaguay.app.data.model.WorkoutSet
import com.entrenaguay.app.util.BilboProgression
import com.entrenaguay.app.util.Labels

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainScreen(
    routineId: Long,
    onFinish: () -> Unit,
    viewModel: TrainViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(routineId) { viewModel.startWorkout(routineId) }

    val totalExercises = state.exercises.size
    val currentIndex = state.currentExerciseIndex
    val currentEx = state.exercises.getOrNull(currentIndex)
    val currentSuggestion = state.suggestions.getOrNull(currentIndex) ?: PerExerciseSuggestion()

    if (totalExercises == 0) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var showJumpDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.routine?.name ?: "Entrenando...") },
                actions = {
                    TextButton(onClick = { viewModel.finishWorkout(onFinish) }) {
                        Icon(Icons.Default.Done, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Finalizar")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // Navigation header
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(12.dp)) {
                    // Exercise nav bar
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.setCurrentExercise(currentIndex - 1) },
                            enabled = currentIndex > 0
                        ) {
                            Icon(Icons.Default.ChevronLeft, "Anterior")
                        }

                        // Exercise name + position
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                currentEx?.exercise?.name ?: "",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "${currentIndex + 1} de $totalExercises",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { viewModel.setCurrentExercise(currentIndex + 1) },
                            enabled = currentIndex < totalExercises - 1
                        ) {
                            Icon(Icons.Default.ChevronRight, "Siguiente")
                        }
                    }

                    // Jump button
                    OutlinedButton(
                        onClick = { showJumpDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.List, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Saltar a otro ejercicio")
                    }
                }
            }

            // Previous best session (PRs)
            currentEx?.let { ex ->
                if (ex.lastSessionSets.isNotEmpty()) {
                    Card(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Tu mejor sesión anterior",
                                style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            ex.lastSessionSets.forEach { s ->
                                val label = if (s.set.setType == "bilbo") "Bilbo" else "Trabajo"
                                Text(
                                    "$label: ${s.set.reps} reps \u00D7 ${String.format("%.1f", s.set.weightKg)} kg" +
                                        if (s.set.rir > 0) " (RIR ${s.set.rir})" else "",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            // Current exercise sets + input
            currentEx?.let { ex ->
                val appliesBilbo = BilboProgression.appliesTo(
                    ex.exercise.isBilboEligible,
                    state.routine?.method ?: ""
                )

                ExerciseSetContent(
                    exerciseName = ex.exercise.name,
                    muscleGroup = ex.exercise.muscleGroup,
                    appliesBilbo = appliesBilbo,
                    sets = ex.sets,
                    suggestion = currentSuggestion,
                    onLogBilboSet = { reps, weight, rir ->
                        viewModel.logSet(ex.exercise.id, "bilbo", reps, weight, rir, true)
                    },
                    onLogWorkSet = { reps, weight ->
                        viewModel.logSet(ex.exercise.id, "work", reps, weight, 0, false)
                    },
                    onDeleteLastSet = { viewModel.deleteLastSet(ex.exercise.id) }
                )
            }
        }
    }

    // Jump dialog
    if (showJumpDialog) {
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("Saltar a ejercicio") },
            text = {
                Column {
                    state.exercises.forEachIndexed { index, ex ->
                        val isCurrent = index == currentIndex
                        ListItem(
                            headlineContent = { Text(ex.exercise.name) },
                            supportingContent = {
                                val setsCount = ex.sets.size
                                Text(if (setsCount > 0) "$setsCount series registradas" else "Sin series")
                            },
                            leadingContent = {
                                if (isCurrent) Icon(Icons.Default.Check, "Actual")
                            },
                            modifier = Modifier.clickable {
                                viewModel.setCurrentExercise(index)
                                showJumpDialog = false
                            },
                            colors = if (isCurrent) ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ) else ListItemDefaults.colors()
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) { Text("Cerrar") }
            }
        )
    }
}

@Composable
fun ExerciseSetContent(
    exerciseName: String,
    muscleGroup: String,
    appliesBilbo: Boolean,
    sets: List<WorkoutSet>,
    suggestion: PerExerciseSuggestion,
    onLogBilboSet: (reps: Int, weight: Float, rir: Int) -> Unit,
    onLogWorkSet: (reps: Int, weight: Float) -> Unit,
    onDeleteLastSet: () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    LazyColumn(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Exercise info
        item {
            if (appliesBilbo) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Whatshot, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text("Serie Bilbo + series de trabajo",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Logged sets
        if (sets.isNotEmpty()) {
            item {
                Card {
                    Column(Modifier.padding(12.dp)) {
                        Text("Series de hoy:", style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.height(4.dp))
                        sets.forEachIndexed { i, set ->
                            val label = if (appliesBilbo && i == 0) "Bilbo" else "Set ${if (appliesBilbo) i else i + 1}"
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 1.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("$label: ${set.reps} reps \u00D7 ${String.format("%.1f", set.weightKg)} kg",
                                    style = MaterialTheme.typography.bodyMedium)
                                if (set.setType == "bilbo") {
                                    Text("RIR ${set.rir}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Input section
        item {
            Card {
                Column(Modifier.padding(12.dp)) {
                    if (appliesBilbo) {
                        val bilboDone = sets.any { it.setType == "bilbo" }
                        if (!bilboDone) {
                            BilboSetInput(
                                suggestion = suggestion,
                                onLog = onLogBilboSet
                            )
                        } else {
                            WorkSetInput(
                                suggestion = suggestion,
                                onLog = onLogWorkSet
                            )
                        }
                    } else {
                        WorkSetInput(
                            suggestion = suggestion,
                            onLog = onLogWorkSet
                        )
                    }
                }
            }
        }

        // Rest timer
        if (sets.isNotEmpty()) {
            item { RestTimer() }
        }

        // Delete last
        if (sets.isNotEmpty()) {
            item {
                TextButton(
                    onClick = onDeleteLastSet,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                    Text("Deshacer última serie")
                }
            }
        }

        // Bottom spacer
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun BilboSetInput(
    suggestion: PerExerciseSuggestion,
    onLog: (reps: Int, weight: Float, rir: Int) -> Unit
) {
    var reps by remember(suggestion) {
        mutableStateOf(if (suggestion.hasHistory) suggestion.bilboReps.toString() else "")
    }
    var weight by remember(suggestion) {
        mutableStateOf(if (suggestion.hasHistory && suggestion.bilboWeight > 0) String.format("%.1f", suggestion.bilboWeight) else "")
    }
    var rir by remember { mutableStateOf("2") }

    Column {
        Text("Registrar serie Bilbo", style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary)
        Text("Explosivo en la subida, controlado en la bajada",
            style = MaterialTheme.typography.bodySmall)

        if (!suggestion.hasHistory) {
            Spacer(Modifier.height(4.dp))
            Text("Primer entreno: introduce los valores manualmente",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = reps,
                onValueChange = { reps = it.filter { c -> c.isDigit() } },
                label = { Text("Reps") },
                placeholder = if (!suggestion.hasHistory) {{ Text("Ej: 15") }} else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Peso (kg)") },
                placeholder = if (!suggestion.hasHistory) {{ Text("Ej: 40") }} else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = rir,
                onValueChange = { rir = it.filter { c -> c.isDigit() } },
                label = { Text("RIR") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(0.6f),
                singleLine = true
            )
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                val r = reps.toIntOrNull() ?: return@Button
                val w = weight.toFloatOrNull() ?: return@Button
                val ri = rir.toIntOrNull() ?: 2
                onLog(r, w, ri)
                reps = ""
                weight = ""
                rir = "2"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Whatshot, null)
            Spacer(Modifier.width(8.dp))
            Text("Registrar serie Bilbo")
        }
    }
}

@Composable
fun WorkSetInput(
    suggestion: PerExerciseSuggestion,
    onLog: (reps: Int, weight: Float) -> Unit
) {
    var reps by remember(suggestion) {
        mutableStateOf(if (suggestion.hasHistory) suggestion.workReps.toString() else "")
    }
    var weight by remember(suggestion) {
        mutableStateOf(if (suggestion.hasHistory && suggestion.workWeight > 0) String.format("%.1f", suggestion.workWeight) else "")
    }

    Column {
        Text("Serie de trabajo", style = MaterialTheme.typography.titleSmall)

        if (!suggestion.hasHistory) {
            Spacer(Modifier.height(4.dp))
            Text("Primer entreno: introduce los valores manualmente",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = reps,
                onValueChange = { reps = it.filter { c -> c.isDigit() } },
                label = { Text("Reps") },
                placeholder = if (!suggestion.hasHistory) {{ Text("Ej: 10") }} else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Peso (kg)") },
                placeholder = if (!suggestion.hasHistory) {{ Text("Ej: 60") }} else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                val r = reps.toIntOrNull() ?: return@Button
                val w = weight.toFloatOrNull() ?: return@Button
                onLog(r, w)
                reps = ""
                weight = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrar serie")
        }
    }
}

@Composable
fun RestTimer() {
    var seconds by remember { mutableIntStateOf(90) }
    var isRunning by remember { mutableStateOf(false) }
    var showTimer by remember { mutableStateOf(false) }

    if (!showTimer) {
        TextButton(onClick = { showTimer = true; isRunning = true }) {
            Icon(Icons.Default.Timer, null, Modifier.size(16.dp))
            Text("Iniciar descanso (90s)")
        }
    } else {
        LaunchedEffect(isRunning) {
            while (isRunning && seconds > 0) {
                kotlinx.coroutines.delay(1000L)
                seconds--
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Timer, "Descanso")
                Text(
                    "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.headlineSmall
                )
                Row {
                    TextButton(onClick = { isRunning = !isRunning }) {
                        Text(if (isRunning) "Pausa" else "Reanudar")
                    }
                    TextButton(onClick = { seconds = 90; isRunning = true }) {
                        Text("Reset")
                    }
                }
            }
        }
    }
}
