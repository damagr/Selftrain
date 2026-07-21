package com.selftrain.app.ui.train

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.selftrain.app.data.model.WorkoutSet
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.selftrain.app.util.BilboProgression
import com.selftrain.app.util.Labels
import com.selftrain.app.util.RestTimerService
import com.selftrain.app.util.getExerciseGifUrl

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TrainScreen(
    routineId: Long,
    resumeWorkoutId: Long? = null,
    onFinish: () -> Unit,
    viewModel: TrainViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(routineId, resumeWorkoutId) { viewModel.startWorkout(routineId, resumeWorkoutId) }

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
                ContainedLoadingIndicator(
                    containerShape = MaterialTheme.shapes.medium,
                    polygons = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons
                )
            }
        }
        return
    }

    var showJumpDialog by remember { mutableStateOf(false) }
    var showGifDialog by remember { mutableStateOf(false) }
    var showBackConfirm by remember { mutableStateOf(false) }

    BackHandler { showBackConfirm = true }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal),
                title = { Text(state.routine?.name ?: "Entrenando...") },
                actions = {
                    TextButton(onClick = { viewModel.showConfirmFinish() }) {
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = MaterialTheme.shapes.large,
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

                        // Exercise name + info button + position
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    currentEx?.exercise?.name ?: "",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (currentEx?.exercise?.name?.let { getExerciseGifUrl(it) } != null) {
                                    IconButton(
                                        onClick = { showGifDialog = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = "Ver demostración",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                "${currentIndex + 1} de $totalExercises",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
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
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.List, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Saltar a otro ejercicio")
                    }
                }
            }

            // Previous best session (PRs) — collapsible
            currentEx?.let { ex ->
                if (ex.lastSessionSets.isNotEmpty()) {
                    var prExpanded by remember { mutableStateOf(true) }
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                Modifier.fillMaxWidth().clickable { prExpanded = !prExpanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Tu mejor sesión anterior",
                                    style = MaterialTheme.typography.titleSmall)
                                Icon(
                                    if (prExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (prExpanded) "Colapsar" else "Expandir",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            AnimatedVisibility(
                                visible = prExpanded,
                                enter = fadeIn(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()) +
                                    expandVertically(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()),
                                exit = fadeOut(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()) +
                                    shrinkVertically(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec())
                            ) {
                                Column {
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
                }
            }

            // Rest timer (shared across exercises)
            RestTimer()

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
                    exerciseKey = ex.exercise.id.toString(),
                    onLogBilboSet = { reps, weight, rir ->
                        viewModel.logSet(ex.exercise.id, "bilbo", reps, weight, rir, true)
                    },
                    onLogWorkSet = { reps, weight, rir ->
                        viewModel.logSet(ex.exercise.id, "work", reps, weight, rir, false)
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
            shape = MaterialTheme.shapes.large,
            title = { Text("Saltar a ejercicio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.exercises.forEachIndexed { index, ex ->
                        val isCurrent = index == currentIndex
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setCurrentExercise(index)
                                    showJumpDialog = false
                                },
                            shape = MaterialTheme.shapes.medium,
                            colors = if (isCurrent) CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ) else CardDefaults.cardColors()
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(ex.exercise.name, style = MaterialTheme.typography.titleSmall)
                                    val setsCount = ex.sets.size
                                    Text(
                                        if (setsCount > 0) "$setsCount series registradas" else "Sin series",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isCurrent) {
                                    Icon(Icons.Default.Check, "Actual",
                                        tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) { Text("Cerrar") }
            }
        )
    }

    // Exercise GIF demonstration dialog
    if (showGifDialog) {
        val gifUrl = currentEx?.exercise?.name?.let { getExerciseGifUrl(it) }
        AlertDialog(
            onDismissRequest = { showGifDialog = false },
            shape = MaterialTheme.shapes.large,
            title = { Text(currentEx?.exercise?.name ?: "") },
            text = {
                if (gifUrl != null) {
                    AsyncImage(
                        model = gifUrl,
                        contentDescription = "Demostración del ejercicio",
                        modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text("No hay demostración disponible para este ejercicio.")
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showGifDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Empty workout confirmation dialog
    if (state.confirmEmpty) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelEmptyFinish() },
            shape = MaterialTheme.shapes.large,
            title = { Text("Entreno vacío") },
            text = { Text("No has registrado ninguna serie. El entreno no se guardará.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.discardEmptyWorkout()
                    onFinish()
                }) {
                    Text("Salir sin guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelEmptyFinish() }) {
                    Text("Seguir entrenando")
                }
            }
        )
    }

    // Back confirm dialog
    if (showBackConfirm) {
        AlertDialog(
            onDismissRequest = { showBackConfirm = false },
            shape = MaterialTheme.shapes.large,
            title = { Text("¿Cancelar entreno?") },
            text = { Text("Se perderán los datos no guardados.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.discardWorkout()
                    onFinish()
                }) {
                    Text("Cancelar entreno", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackConfirm = false }) {
                    Text("Seguir entrenando")
                }
            }
        )
    }

    // Pre-confirm finish dialog
    if (state.confirmFinish) {
        val totalSets = state.exercises.sumOf { it.sets.size }
        val exercisesWithSets = state.exercises.count { it.sets.isNotEmpty() }
        val workout = state.routine

        AlertDialog(
            onDismissRequest = { viewModel.cancelConfirmFinish() },
            shape = MaterialTheme.shapes.large,
            title = { Text("¿Terminar entreno?") },
            text = {
                Text("Has registrado $totalSets series en $exercisesWithSets ejercicios.")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.finishWorkout() }) {
                    Text("Sí, finalizar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelConfirmFinish() }) {
                    Text("Seguir entrenando")
                }
            }
        )
    }

    // Workout summary dialog
    state.workoutSummary?.let { summary ->
        var editDuration by remember { mutableStateOf(summary.durationMinutes.toString()) }

        AlertDialog(
            onDismissRequest = { viewModel.dismissSummary(); onFinish() },
            shape = MaterialTheme.shapes.large,
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
                                "${Labels.muscleGroup(wc.muscleGroup)}: ${"%,.1f".format(java.util.Locale.getDefault(), wc.thisWorkoutKg)} kg → $pct vs semana pasada",
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExerciseSetContent(
    exerciseName: String,
    muscleGroup: String,
    appliesBilbo: Boolean,
    sets: List<WorkoutSet>,
    suggestion: PerExerciseSuggestion,
    exerciseKey: String,
    onLogBilboSet: (reps: Int, weight: Float, rir: Int) -> Unit,
    onLogWorkSet: (reps: Int, weight: Float, rir: Int) -> Unit,
    onDeleteLastSet: () -> Unit
) {
    val lastWorkSet = sets.lastOrNull { it.setType == "work" }
    val intraAdjustment = lastWorkSet?.let {
        BilboProgression.workSetAdjustment(it.reps, it.weightKg)
    }
    val hasBetweenHint = suggestion.hasHistory && lastWorkSet == null &&
        suggestion.workProgression != BilboProgression.WorkProgression.MAINTAIN
    val bilboDone = sets.any { it.setType == "bilbo" }

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
                ElevatedCard(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
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

        // Bilbo 50+ progression hint
        val lastSet = sets.lastOrNull()
        if (lastSet?.setType == "bilbo" && lastSet.reps >= 50) {
            item {
                ElevatedCard(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("¡50 reps alcanzadas!", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Próxima sesión: sube peso un ${if (lastSet.rir == 0) "15" else "10"}% y resetea a 15-20 reps",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Between-sessions progression hint
        if (hasBetweenHint) {
            item {
                val isIncrease = suggestion.workProgression == BilboProgression.WorkProgression.INCREASE
                ElevatedCard(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isIncrease) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isIncrease) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            null,
                            tint = if (isIncrease) MaterialTheme.colorScheme.onPrimaryContainer
                                   else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isIncrease) "Subir peso \u2014 hiciste >10 reps la sesi\u00F3n anterior"
                            else "Bajar peso \u2014 hiciste <8 reps la sesi\u00F3n anterior",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isIncrease) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Intra-session advice
        intraAdjustment?.let { (progression, suggestedWeight) ->
            item {
                val isIncrease = progression == BilboProgression.WorkProgression.INCREASE
                val lastReps = lastWorkSet?.reps ?: 0
                ElevatedCard(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isIncrease) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isIncrease) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            null,
                            tint = if (isIncrease) MaterialTheme.colorScheme.onPrimaryContainer
                                   else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isIncrease) "Sube a ~${String.format(java.util.Locale.US, "%.1f", suggestedWeight)} kg (hiciste $lastReps, >10)"
                            else "Baja a ~${String.format(java.util.Locale.US, "%.1f", suggestedWeight)} kg (hiciste $lastReps, <8)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isIncrease) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Input section with animated transition
        item {
            ElevatedCard(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(12.dp)) {
                    if (appliesBilbo) {
                        AnimatedVisibility(
                            visible = !bilboDone,
                            enter = fadeIn(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()) +
                                expandVertically(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()),
                            exit = fadeOut(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()) +
                                shrinkVertically(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec())
                        ) {
                            BilboSetInput(
                                exerciseKey = exerciseKey, sets = sets,
                                suggestion = suggestion, onLog = onLogBilboSet,
                                onDeleteLastSet = onDeleteLastSet
                            )
                        }
                        AnimatedVisibility(
                            visible = bilboDone,
                            enter = fadeIn(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()) +
                                expandVertically(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()),
                            exit = fadeOut(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()) +
                                shrinkVertically(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec())
                        ) {
                            WorkSetInput(
                                exerciseKey = exerciseKey, sets = sets,
                                suggestion = suggestion, onLog = onLogWorkSet,
                                onDeleteLastSet = onDeleteLastSet
                            )
                        }
                    } else {
                        WorkSetInput(
                            exerciseKey = exerciseKey, sets = sets,
                            suggestion = suggestion, onLog = onLogWorkSet,
                            onDeleteLastSet = onDeleteLastSet
                        )
                    }
                }
            }
        }

        // Bottom spacer
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun BilboSetInput(
    exerciseKey: String,
    sets: List<WorkoutSet>,
    suggestion: PerExerciseSuggestion,
    onLog: (reps: Int, weight: Float, rir: Int) -> Unit,
    onDeleteLastSet: () -> Unit
) {
    val lastSetWeight = sets.lastOrNull()?.weightKg
    var reps by remember(exerciseKey) { mutableStateOf("") }
    var weight by remember(exerciseKey) {
        mutableStateOf(
            if (lastSetWeight != null && lastSetWeight > 0) String.format(java.util.Locale.US, "%.1f", lastSetWeight)
            else if (suggestion.hasHistory && suggestion.bilboWeight > 0) String.format(java.util.Locale.US, "%.1f", suggestion.bilboWeight)
            else ""
        )
    }
    var rir by remember { mutableStateOf("2") }

    LaunchedEffect(lastSetWeight) {
        if (lastSetWeight != null && lastSetWeight > 0) {
            weight = String.format(java.util.Locale.US, "%.1f", lastSetWeight)
        }
    }

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

        // Row 1: Reps + Peso
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = reps,
                onValueChange = { reps = it.filter { c -> c.isDigit() } },
                label = { Text("Reps") },
                placeholder = if (!suggestion.hasHistory) {{ Text("Ej: 15") }} else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f), singleLine = true
            )
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Peso (kg)") },
                placeholder = if (!suggestion.hasHistory) {{ Text("Ej: 40") }} else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f), singleLine = true
            )
        }
        Spacer(Modifier.height(8.dp))

        // Row 2: RIR sola
        OutlinedTextField(
            value = rir,
            onValueChange = { rir = it.filter { c -> c.isDigit() } },
            label = { Text("RIR (Reps en Reserva)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        // Row 3: Registrar + Deshacer
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val r = reps.toIntOrNull() ?: return@Button
                    val w = weight.toFloatOrNull() ?: return@Button
                    val ri = rir.toIntOrNull() ?: 2
                    onLog(r, w, ri)
                    reps = ""
                },
                modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Whatshot, null)
                Spacer(Modifier.width(8.dp))
                Text("Registrar Bilbo")
            }
            if (sets.isNotEmpty()) {
                OutlinedButton(onClick = onDeleteLastSet, shape = MaterialTheme.shapes.medium) {
                    Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Deshacer")
                }
            }
        }
    }
}

@Composable
fun WorkSetInput(
    exerciseKey: String,
    sets: List<WorkoutSet>,
    suggestion: PerExerciseSuggestion,
    onLog: (reps: Int, weight: Float, rir: Int) -> Unit,
    onDeleteLastSet: () -> Unit
) {
    val lastWorkSet = sets.lastOrNull { it.setType == "work" }
    val lastSetWeight = lastWorkSet?.weightKg
    val lastBilboInSession = sets.lastOrNull { it.setType == "bilbo" }
    var reps by remember(exerciseKey) { mutableStateOf("") }
    var weight by remember(exerciseKey) {
        mutableStateOf(
            if (lastSetWeight != null && lastSetWeight > 0) String.format(java.util.Locale.US, "%.1f", lastSetWeight)
            else if (suggestion.hasHistory && suggestion.workWeight > 0) String.format(java.util.Locale.US, "%.1f", suggestion.workWeight)
            else if (lastBilboInSession != null) String.format(java.util.Locale.US, "%.1f", lastBilboInSession.weightKg * 1.40f)
            else ""
        )
    }
    var rir by remember { mutableStateOf("2") }

    LaunchedEffect(lastSetWeight) {
        if (lastSetWeight != null && lastSetWeight > 0) {
            weight = String.format(java.util.Locale.US, "%.1f", lastSetWeight)
        }
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

        // Row 1: Reps + Peso
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = reps,
                onValueChange = { reps = it.filter { c -> c.isDigit() } },
                label = { Text("Reps") },
                placeholder = if (!suggestion.hasHistory) {{ Text("Ej: 10") }} else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f), singleLine = true
            )
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Peso (kg)") },
                placeholder = if (!suggestion.hasHistory) {{ Text("Ej: 60") }} else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f), singleLine = true
            )
        }
        Spacer(Modifier.height(8.dp))

        // Row 2: RIR sola
        OutlinedTextField(
            value = rir,
            onValueChange = { rir = it.filter { c -> c.isDigit() } },
            label = { Text("RIR (Reps en Reserva)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        // Row 3: Registrar + Deshacer
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val r = reps.toIntOrNull() ?: return@Button
                    val w = weight.toFloatOrNull() ?: return@Button
                    val ri = rir.toIntOrNull() ?: 2
                    onLog(r, w, ri)
                    reps = ""
                },
                modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.medium
            ) {
                Text("Registrar serie")
            }
            if (sets.isNotEmpty()) {
                OutlinedButton(onClick = onDeleteLastSet, shape = MaterialTheme.shapes.medium) {
                    Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Deshacer")
                }
            }
        }
    }
}

@Composable
fun RestTimer() {
    val context = LocalContext.current
    var totalSeconds by remember { mutableIntStateOf(90) }
    var remaining by remember { mutableIntStateOf(90) }
    var isRunning by remember { mutableStateOf(false) }
    var showTimer by remember { mutableStateOf(false) }
    var pausedRemaining by remember { mutableIntStateOf(0) }
    var showFinishedMessage by remember { mutableStateOf(false) }

    // Stop service when composable leaves composition (e.g. navigate away)
    DisposableEffect(Unit) {
        onDispose {
            context.stopService(RestTimerService.createStopIntent(context))
        }
    }

    // Tick loop: decrement every second while running
    if (isRunning) {
        LaunchedEffect(isRunning) {
            while (isRunning) {
                delay(1000L)
                remaining = (remaining - 1).coerceAtLeast(0)
                if (remaining <= 0) {
                    // ponytail: service notification handles sound+vibrate; in-app shows flash only
                    showFinishedMessage = true
                    delay(2000)
                    showFinishedMessage = false
                    isRunning = false
                    showTimer = false
                }
            }
        }
    }

    if (!showTimer) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // +/- 30s buttons
            IconButton(onClick = {
                if (totalSeconds > 30) totalSeconds -= 30
                remaining = totalSeconds
            }) {
                Text("−30s", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = {
                showTimer = true
                isRunning = true
                remaining = totalSeconds
                // Start foreground service
                RestTimerService.createChannel(context)
                context.startForegroundService(RestTimerService.createStartIntent(context, totalSeconds))
            }) {
                Icon(Icons.Default.Timer, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Iniciar descanso (${totalSeconds}s)")
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = {
                if (totalSeconds < 300) totalSeconds += 30
                remaining = totalSeconds
            }) {
                Text("+30s", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Timer, "Descanso")
                Text(
                    "${remaining / 60}:${(remaining % 60).toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.headlineSmall
                )
                Row {
                    TextButton(onClick = {
                        if (isRunning) {
                            // Pause
                            isRunning = false
                            pausedRemaining = remaining
                            context.stopService(RestTimerService.createStopIntent(context))
                        } else {
                            // Resume
                            isRunning = true
                            remaining = pausedRemaining
                            totalSeconds = pausedRemaining
                            // Restart service with remaining time
                            RestTimerService.createChannel(context)
                            context.startForegroundService(RestTimerService.createStartIntent(context, pausedRemaining))
                        }
                    }) {
                        Text(if (isRunning) "Pausa" else "Reanudar")
                    }
                    TextButton(onClick = {
                        isRunning = false
                        remaining = totalSeconds
                        showTimer = false
                        context.stopService(RestTimerService.createStopIntent(context))
                    }) {
                        Text("Reset")
                    }
                }
            }
        }
    }

    // Flash "finished" message
    if (showFinishedMessage) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Text(
                "¡Descanso terminado!",
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
