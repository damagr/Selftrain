package com.selftrain.app.ui.train

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
import com.selftrain.app.data.model.WorkoutSet
import com.selftrain.app.util.BilboProgression
import com.selftrain.app.util.Labels

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
        if (state.routine != null) {
            // Routine loaded but has no exercises
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Esta rutina no tiene ejercicios.", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("Añade ejercicios desde la pantalla de edición.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = onFinish) {
                        Text("Volver")
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    var showJumpDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal),
                title = { Text(state.routine?.name ?: "Entrenando...") },
                actions = {
                    TextButton(onClick = { viewModel.finishWorkout() }) {
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

    // Workout summary dialog
    state.workoutSummary?.let { summary ->
        var editDuration by remember { mutableStateOf(summary.durationMinutes.toString()) }

        AlertDialog(
            onDismissRequest = { viewModel.dismissSummary(); onFinish() },
            title = { Text("¡Entreno completado!") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Duration
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Duración: ")
                        OutlinedTextField(
                            value = editDuration,
                            onValueChange = { v ->
                                editDuration = v.filter { ch -> ch.isDigit() }
                                v.toIntOrNull()?.let { viewModel.updateDuration(it) }
                            },
                            label = { Text("min") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(100.dp),
                            singleLine = true
                        )
                    }

                    // Volume per muscle group
                    if (summary.volumeByMuscle.isNotEmpty()) {
                        Text("Volumen por grupo muscular", style = MaterialTheme.typography.titleSmall)
                        summary.volumeByMuscle.forEach { mgv: MuscleGroupVolume ->
                            Text(
                                "${Labels.muscleGroup(mgv.muscleGroup)}: %,.1f kg (${mgv.setCount} series)".format(
                                    java.util.Locale.getDefault(), mgv.totalKg
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Personal records
                    if (summary.personalRecords.isNotEmpty()) {
                        Text("¡Nuevos récords!", style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary)
                        summary.personalRecords.forEach { pr: PersonalRecord ->
                            Text(
                                "${pr.exerciseName}: %,.1f → %,.1f kg (1RM)".format(
                                    java.util.Locale.getDefault(),
                                    pr.previousBest, pr.newRecord
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Weekly comparison
                    if (summary.weeklyComparison.isNotEmpty()) {
                        Text("vs semana pasada", style = MaterialTheme.typography.titleSmall)
                        summary.weeklyComparison.forEach { wc: WeeklyComparison ->
                            val pct = if (wc.lastWeekKg > 0) {
                                "%,.0f%%".format(java.util.Locale.getDefault(), ((wc.thisWorkoutKg - wc.lastWeekKg) / wc.lastWeekKg) * 100)
                            } else "—"
                            Text(
                                "${Labels.muscleGroup(wc.muscleGroup)}: %,.1f kg → ${pct} vs semana pasada".format(
                                    java.util.Locale.getDefault(), wc.thisWorkoutKg
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissSummary(); onFinish() }) {
                    Text("Volver a rutinas")
                }
            },
            dismissButton = null
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
