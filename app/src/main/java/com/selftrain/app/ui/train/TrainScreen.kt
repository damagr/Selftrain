package com.selftrain.app.ui.train

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.compose.BackHandler
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.selftrain.app.data.model.WorkoutSet
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.selftrain.app.util.BilboProgression
import com.selftrain.app.util.Labels
import com.selftrain.app.util.PlateCalculator
import com.selftrain.app.util.RestTimerController
import com.selftrain.app.util.RestTimerService
import com.selftrain.app.util.getExerciseGifUrl

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun TrainScreen(
    routineId: Long,
    resumeWorkoutId: Long? = null,
    onFinish: () -> Unit,
    viewModel: TrainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val restTrigger by viewModel.restTrigger.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current
    // ponytail: scope de composición para discard — el DELETE se completa ANTES de onFinish()
    val discardScope = rememberCoroutineScope()

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

    var showJumpDialog by rememberSaveable { mutableStateOf(false) }
    var showGifDialog by rememberSaveable { mutableStateOf(false) }
    var showBackConfirm by rememberSaveable { mutableStateOf(false) }
    var showUndoConfirm by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scrollScope = rememberCoroutineScope()

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
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(padding).fillMaxSize().imeNestedScroll().imePadding()
        ) {
            // Navigation header
            item(key = "nav") {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        currentEx?.exercise?.name ?: "",
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    val hasGif = currentEx?.exercise?.gifUrl != null ||
                                        getExerciseGifUrl(currentEx?.exercise?.name ?: "") != null
                                    if (hasGif) {
                                        IconButton(
                                            onClick = { showGifDialog = true },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Info,
                                                contentDescription = "Ver demostración",
                                                modifier = Modifier.size(20.dp)
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
            }

            // Last session for this exercise — collapsible
            currentEx?.let { ex ->
                if (ex.lastSessionSets.isNotEmpty()) {
                    item(key = "last-session") {
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
                                    Text("Última sesión",
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
            }

            // Rest timer (shared across exercises) — default: configurado en la rutina o por categoría
            item(key = "rest") {
                val restDefault = currentEx?.restSeconds?.takeIf { it > 0 }
                    ?: defaultRestSeconds(currentEx?.exercise?.category)
                RestTimer(
                    category = state.exercises.getOrNull(currentIndex)?.exercise?.category,
                    restTrigger = restTrigger,
                    defaultSeconds = restDefault
                )
            }

            // Current exercise sets + input
            currentEx?.let { ex ->
                val appliesBilbo = ex.isBilbo
                exerciseSetItems(
                    scrollToInput = { scrollScope.launch {
                        delay(400)
                        val last = listState.layoutInfo.totalItemsCount - 1
                        if (last >= 0) listState.animateScrollToItem(last)
                    }},
                    appliesBilbo = appliesBilbo,
                    isDumbbell = ex.exercise.equipment == "dumbbell",
                    sets = ex.sets,
                    suggestion = currentSuggestion,
                    exerciseKey = ex.exercise.id.toString(),
                    onLogBilboSet = { reps, weight, rir, notes ->
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.logSet(ex.exercise.id, "bilbo", reps, weight, rir, true, notes)
                    },
                    onLogWorkSet = { reps, weight, rir, notes ->
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.logSet(ex.exercise.id, "work", reps, weight, rir, false, notes)
                    },
                    onDeleteLastSet = { showUndoConfirm = true }
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
                                containerColor = MaterialTheme.colorScheme.surface
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
                                        tint = MaterialTheme.colorScheme.onSurface)
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
        val gifUrl = currentEx?.exercise?.gifUrl ?: currentEx?.exercise?.name?.let { getExerciseGifUrl(it) }
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
                    discardScope.launch {
                        viewModel.discardEmptyWorkout()
                        stopRestTimer(context)
                        onFinish()
                    }
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
                    discardScope.launch {
                        viewModel.discardWorkout()
                        stopRestTimer(context)
                        onFinish()
                    }
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

    // Undo last set confirmation dialog
    if (showUndoConfirm) {
        AlertDialog(
            onDismissRequest = { showUndoConfirm = false },
            shape = MaterialTheme.shapes.large,
            title = { Text("¿Deshacer última serie?") },
            text = { Text("Se eliminará la última serie registrada de este ejercicio.") },
            confirmButton = {
                TextButton(onClick = {
                    showUndoConfirm = false
                    viewModel.deleteLastSet(currentEx!!.exercise.id)
                }) {
                    Text("Deshacer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUndoConfirm = false }) {
                    Text("Cancelar")
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
        var editBodyWeight by remember {
            mutableStateOf(if (summary.bodyWeight > 0) String.format(java.util.Locale.US, "%.1f", summary.bodyWeight) else "")
        }

        AlertDialog(
            onDismissRequest = { viewModel.dismissSummary(); stopRestTimer(context); onFinish() },
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

                    // ponytail: peso corporal opcional (para fuerza relativa 1RM/kg)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Peso corporal: ")
                        OutlinedTextField(
                            value = editBodyWeight,
                            onValueChange = { v ->
                                editBodyWeight = v.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
                                v.replace(",", ".").toFloatOrNull()?.let { viewModel.updateBodyWeight(it) }
                                if (v.isEmpty()) viewModel.updateBodyWeight(0f)
                            },
                            label = { Text("kg (opcional)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                            color = MaterialTheme.colorScheme.onSurface)
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
                Row {
                    // ponytail: compartir el resumen del entreno como texto
                    TextButton(onClick = {
                        val shareText = buildString {
                            appendLine("Entreno completado")
                            appendLine("Duración: ${summary.durationMinutes} min")
                            if (summary.bodyWeight > 0f) {
                                appendLine("Peso corporal: ${String.format("%.1f", summary.bodyWeight)} kg")
                            }
                            if (summary.volumeByMuscle.isNotEmpty()) {
                                appendLine("Volumen por grupo muscular:")
                                summary.volumeByMuscle.forEach {
                                    appendLine("  - ${Labels.muscleGroup(it.muscleGroup)}: ${String.format("%.1f", it.totalKg)} kg (${it.setCount} series)")
                                }
                            }
                            if (summary.personalRecords.isNotEmpty()) {
                                appendLine("Nuevos récords (1RM estimado):")
                                summary.personalRecords.forEach {
                                    appendLine("  - ${it.exerciseName}: ${String.format("%.1f", it.newRecord)} kg (antes ${String.format("%.1f", it.previousBest)})")
                                }
                            }
                        }
                        val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(android.content.Intent.createChooser(send, "Compartir resumen"))
                    }) {
                        Text("Compartir")
                    }
                    TextButton(onClick = { viewModel.dismissSummary(); stopRestTimer(context); onFinish() }) {
                        Text("Volver a rutinas")
                    }
                }
            },
            dismissButton = null
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun LazyListScope.exerciseSetItems(
    scrollToInput: () -> Unit = {},
    appliesBilbo: Boolean,
    isDumbbell: Boolean,
    sets: List<WorkoutSet>,
    suggestion: PerExerciseSuggestion,
    exerciseKey: String,
    onLogBilboSet: (reps: Int, weight: Float, rir: Int, notes: String) -> Unit,
    onLogWorkSet: (reps: Int, weight: Float, rir: Int, notes: String) -> Unit,
    onDeleteLastSet: () -> Unit
) {
    val lastWorkSet = sets.lastOrNull { it.setType == "work" }
    val intraAdjustment = lastWorkSet?.let {
        BilboProgression.workSetAdjustment(it.reps, it.rir, it.weightKg)?.let { (prog, w) ->
            prog to (if (isDumbbell) BilboProgression.roundToDumbbellStep(w) else w)
        }
    }
    val bilboDone = sets.any { it.setType == "bilbo" }

    // Exercise info
    item(key = "$exerciseKey-info") {
        if (appliesBilbo) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Whatshot, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text("Serie Bilbo + series de trabajo",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }

    // Logged sets
    if (sets.isNotEmpty()) {
        item(key = "$exerciseKey-sets") {
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
                        // ponytail: nota de la serie, visible en la sesión
                        if (set.notes.isNotBlank()) {
                            Text(
                                "↳ ${set.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Bilbo 50+ progression hint
    val lastSet = sets.lastOrNull()
    if (lastSet?.setType == "bilbo" && lastSet.reps >= 50) {
        item(key = "$exerciseKey-hint50") {
            ElevatedCard(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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

    // Intra-session advice
    intraAdjustment?.let { (progression, suggestedWeight) ->
        item(key = "$exerciseKey-intra") {
            val isIncrease = progression == BilboProgression.WorkProgression.INCREASE
            val lastEffective = BilboProgression.effectiveReps(lastWorkSet?.reps ?: 0, lastWorkSet?.rir ?: 0)
            ElevatedCard(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isIncrease) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isIncrease) "Sube a ~${String.format(java.util.Locale.US, "%.1f", suggestedWeight)} kg (hiciste $lastEffective reps efectivas, >=12)"
                        else "Baja a ~${String.format(java.util.Locale.US, "%.1f", suggestedWeight)} kg (hiciste $lastEffective reps efectivas, <8)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    // Input section with animated transition
    item(key = "$exerciseKey-input") {
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
                            onDeleteLastSet = onDeleteLastSet,
                            onFieldFocused = scrollToInput
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
                            onDeleteLastSet = onDeleteLastSet,
                            onFieldFocused = scrollToInput
                        )
                    }
                } else {
                    WorkSetInput(
                        exerciseKey = exerciseKey, sets = sets,
                        suggestion = suggestion, onLog = onLogWorkSet,
                        onDeleteLastSet = onDeleteLastSet,
                        onFieldFocused = scrollToInput
                    )
                }
            }
        }
    }

    // Bottom spacer
    item(key = "$exerciseKey-spacer") { Spacer(Modifier.height(16.dp)) }
}

@Composable
fun BilboSetInput(
    exerciseKey: String,
    sets: List<WorkoutSet>,
    suggestion: PerExerciseSuggestion,
    onLog: (reps: Int, weight: Float, rir: Int, notes: String) -> Unit,
    onDeleteLastSet: () -> Unit,
    onFieldFocused: () -> Unit = {}
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
    var note by remember(exerciseKey) { mutableStateOf("") }
    var showPlateCalc by remember { mutableStateOf(false) }

    LaunchedEffect(lastSetWeight) {
        if (lastSetWeight != null && lastSetWeight > 0) {
            weight = String.format(java.util.Locale.US, "%.1f", lastSetWeight)
        }
    }

    Column {
        Text("Registrar serie Bilbo", style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface)
        Text("Explosivo en la subida, controlado en la bajada",
            style = MaterialTheme.typography.bodySmall)
        if (!suggestion.hasHistory) {
            Spacer(Modifier.height(4.dp))
            Text("Primer entreno: introduce los valores manualmente",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))

        // Row 1: Reps + Peso + discos
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = reps,
                onValueChange = { reps = it.filter { c -> c.isDigit() } },
                label = { Text("Reps") },
                placeholder = if (!suggestion.hasHistory) {{ Text("Ej: 15") }} else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) onFieldFocused() }, singleLine = true
            )
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Peso (kg)") },
                placeholder = if (!suggestion.hasHistory) {{ Text("Ej: 40") }} else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) onFieldFocused() }, singleLine = true
            )
            IconButton(onClick = { showPlateCalc = true }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Calculate, "Calcular discos", Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(8.dp))

        // Row 2: RIR sola
        OutlinedTextField(
            value = rir,
            onValueChange = { rir = it.filter { c -> c.isDigit() } },
            label = { Text("RIR (Reps en Reserva)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) onFieldFocused() }, singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        // ponytail: nota opcional de la serie
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Nota (opcional)") },
            modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) onFieldFocused() }, singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        // Row 3: Registrar + Deshacer
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val r = reps.toIntOrNull() ?: return@Button
                    val w = weight.toFloatOrNull() ?: return@Button
                    val ri = rir.toIntOrNull() ?: 2
                    onLog(r, w, ri, note.trim())
                    reps = ""
                    note = ""
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

        if (showPlateCalc) {
            PlateCalculatorDialog(
                initialWeight = weight.toFloatOrNull() ?: 0f,
                onDismiss = { showPlateCalc = false }
            )
        }
    }
}

@Composable
fun WorkSetInput(
    exerciseKey: String,
    sets: List<WorkoutSet>,
    suggestion: PerExerciseSuggestion,
    onLog: (reps: Int, weight: Float, rir: Int, notes: String) -> Unit,
    onDeleteLastSet: () -> Unit,
    onFieldFocused: () -> Unit = {}
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
    var note by remember(exerciseKey) { mutableStateOf("") }
    var showPlateCalc by remember { mutableStateOf(false) }

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

        // Row 1: Reps + Peso + discos
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = reps,
                onValueChange = { reps = it.filter { c -> c.isDigit() } },
                label = { Text("Reps") },
                placeholder = if (!suggestion.hasHistory) {{ Text("Ej: 10") }} else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) onFieldFocused() }, singleLine = true
            )
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Peso (kg)") },
                placeholder = if (!suggestion.hasHistory) {{ Text("Ej: 60") }} else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) onFieldFocused() }, singleLine = true
            )
            IconButton(onClick = { showPlateCalc = true }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Calculate, "Calcular discos", Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(8.dp))

        // Row 2: RIR sola
        OutlinedTextField(
            value = rir,
            onValueChange = { rir = it.filter { c -> c.isDigit() } },
            label = { Text("RIR (Reps en Reserva)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) onFieldFocused() }, singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        // ponytail: nota opcional de la serie
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Nota (opcional)") },
            modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) onFieldFocused() }, singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        // Row 3: Registrar + Deshacer
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val r = reps.toIntOrNull() ?: return@Button
                    val w = weight.toFloatOrNull() ?: return@Button
                    val ri = rir.toIntOrNull() ?: 2
                    onLog(r, w, ri, note.trim())
                    reps = ""
                    note = ""
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

        if (showPlateCalc) {
            PlateCalculatorDialog(
                initialWeight = weight.toFloatOrNull() ?: 0f,
                onDismiss = { showPlateCalc = false }
            )
        }
    }
}

@Composable
fun RestTimer(category: String? = null, restTrigger: Int = 0, defaultSeconds: Int = 90) {
    val context = LocalContext.current

    // ponytail: única fuente de verdad = RestTimerController (lo escribe el servicio, la UI lee).
    // Sin estado local duplicado: la UI siempre muestra el valor del controller — el servicio y
    // la app quedan sincronizados (pausa por notificación incluida), y la cuenta sobrevive a
    // navegación y rotación porque el controller vive a nivel de proceso.
    val isActive = RestTimerController.isActive
    val isRunning = RestTimerController.isRunning
    val remaining = RestTimerController.remaining

    // Payload para restaurar tras muerte de proceso: guarda el DEADLINE (no los segundos),
    // así el remaining real se recalcula aunque el proceso haya estado muerto un rato.
    var savedActive by rememberSaveable { mutableStateOf(false) }
    var savedRunning by rememberSaveable { mutableStateOf(false) }
    var savedDeadline by rememberSaveable { mutableLongStateOf(0L) }
    var savedTotal by rememberSaveable { mutableIntStateOf(0) }
    var savedPaused by rememberSaveable { mutableIntStateOf(0) }

    // Restore-once: SOLO si el controller no ha tenido sesión en este proceso (proceso fresco
    // tras muerte) y el payload guardado dice que había timer activo. Si el controller ya
    // vivió una sesión, su estado es la verdad y un restore stale re-disparaba el flash.
    LaunchedEffect(Unit) {
        if (!RestTimerController.sessionStarted && savedActive) {
            if (savedRunning) {
                val rem = ((savedDeadline - System.currentTimeMillis()) / 1000L).toInt()
                if (rem > 0) {
                    // rearranca servicio + notificación con el tiempo real restante
                    RestTimerController.restoreRunning(savedTotal, savedDeadline)
                    context.startForegroundService(
                        RestTimerService.createStartIntent(context, savedDeadline, savedTotal)
                    )
                } else {
                    // expiró mientras el proceso estuvo muerto → flash, sin servicio
                    RestTimerController.finish()
                }
            } else {
                // estaba en pausa: restaura la tarjeta; la notificación la recupera el sticky
                // restart si lo hubo, y reanudar in-app rearraque el servicio
                RestTimerController.restorePaused(savedTotal, savedPaused)
            }
            savedActive = false
        }
    }

    // Sync del payload tras cada composición
    SideEffect {
        savedActive = RestTimerController.isActive
        savedRunning = RestTimerController.isRunning
        savedDeadline = RestTimerController.endTimestamp
        savedTotal = RestTimerController.totalSeconds
        savedPaused = RestTimerController.pausedRemaining
    }

    // Flash "terminado": háptico + se auto-limpia a los 2s
    val hapticFeedback = LocalHapticFeedback.current
    LaunchedEffect(RestTimerController.finishedFlash) {
        if (RestTimerController.finishedFlash) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(2000)
            RestTimerController.clearFlash()
        }
    }

    // Reset del default al cambiar de ejercicio, solo si no hay descanso activo
    // (para no cortar un descanso en marcha). El ±30s manual sigue disponible.
    var customSeconds by remember(defaultSeconds) { mutableIntStateOf(defaultSeconds) }
    LaunchedEffect(defaultSeconds) {
        if (!RestTimerController.isActive) customSeconds = defaultSeconds
    }

    // ponytail: descanso automático — al registrar una serie (trigger del ViewModel) arranca
    // el descanso con los customSeconds del usuario (respeta sus ±30s ajustados).
    // El último trigger atendido vive en RestTimerController (nivel proceso): el remember local
    // se perdía al desecharse el item (scroll) o rotar y se REINICIABA el descanso en marcha.
    LaunchedEffect(restTrigger) {
        if (restTrigger > RestTimerController.lastHandledTrigger) {
            RestTimerController.lastHandledTrigger = restTrigger
            if (restTrigger > 0) {
                RestTimerController.start(customSeconds)
                context.startForegroundService(
                    RestTimerService.createStartIntent(context, RestTimerController.endTimestamp, customSeconds)
                )
            }
        }
    }

    if (!isActive) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // +/- 30s buttons
            IconButton(onClick = {
                if (customSeconds > 30) customSeconds -= 30
            }) {
                Text("−30s", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = {
                RestTimerController.start(customSeconds)
                context.startForegroundService(
                    RestTimerService.createStartIntent(context, RestTimerController.endTimestamp, customSeconds)
                )
            }) {
                Icon(Icons.Default.Timer, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Iniciar descanso (${customSeconds}s)")
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = {
                if (customSeconds < 300) customSeconds += 30
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
                        // mismo intent toggle que la notificación → sincronizados
                        context.startService(RestTimerService.createPauseIntent(context))
                    }) {
                        Text(if (isRunning) "Pausa" else "Reanudar")
                    }
                    TextButton(onClick = {
                        context.startService(RestTimerService.createStopIntent(context))
                    }) {
                        Text("Reset")
                    }
                }
            }
        }
    }

    // Flash "finished" message
    if (RestTimerController.finishedFlash) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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

/** Default de descanso según tipo de ejercicio: compuestos 2-3min, aislamiento 90-120s */
private fun defaultRestSeconds(category: String?): Int = when (category) {
    "compound" -> 150
    "isolation" -> 105
    else -> 90
}

/** ponytail: calculadora de discos — cómo cargar la barra para el peso indicado */
@Composable
fun PlateCalculatorDialog(
    initialWeight: Float,
    onDismiss: () -> Unit
) {
    var targetText by remember { mutableStateOf(
        if (initialWeight > 0) String.format(java.util.Locale.US, "%.1f", initialWeight) else ""
    ) }
    var barKg by remember { mutableDoubleStateOf(20.0) }
    val target = targetText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val result = remember(target, barKg) {
        if (target > 0) PlateCalculator.compute(target, barKg) else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text("Calculadora de discos") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = { Text("Peso objetivo (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                // Barra: 20 / 15 / 10 kg
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = barKg == 20.0,
                        onClick = { barKg = 20.0 },
                        label = { Text("Barra 20kg") }
                    )
                    FilterChip(
                        selected = barKg == 15.0,
                        onClick = { barKg = 15.0 },
                        label = { Text("15kg") }
                    )
                    FilterChip(
                        selected = barKg == 10.0,
                        onClick = { barKg = 10.0 },
                        label = { Text("10kg") }
                    )
                }
                when {
                    target <= 0 -> {
                        Text("Introduce el peso objetivo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    target <= barKg -> {
                        Text("El peso no supera a la barra: carga solo la barra",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    result != null -> {
                        Text("Por lado:", style = MaterialTheme.typography.titleSmall)
                        if (result.perSide.isEmpty()) {
                            Text("Sin discos", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text(
                                result.perSide.joinToString(" + ") {
                                    if (it == it.toLong().toDouble()) "${it.toLong()}" else "$it"
                                } + " kg",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (result.remainderKg > 0) {
                            Text(
                                "Con los discos estándar cargarás ${String.format("%.1f", result.totalKg)} kg (faltan ${result.remainderKg} kg en total)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "Total en barra: ${String.format("%.1f", result.totalKg)} kg",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

/** ponytail: para el descanso activo y limpia el controller al terminar/descartar el entreno.
 *  Usa el intent "stop" (no stopService) para que el servicio limpie también las prefs. */
private fun stopRestTimer(context: Context) {
    if (RestTimerController.isActive) {
        context.startService(RestTimerService.createStopIntent(context))
    }
    RestTimerController.stop()
}
