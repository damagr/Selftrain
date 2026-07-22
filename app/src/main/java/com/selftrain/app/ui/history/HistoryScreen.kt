package com.selftrain.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.selftrain.app.data.model.Exercise
import com.selftrain.app.data.model.Workout
import com.selftrain.app.data.db.SetWithExercise
import com.selftrain.app.data.model.WorkoutSet
import com.selftrain.app.util.Labels
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    val allExercises by viewModel.getAllExercises().collectAsState()

    val currentMonth by viewModel.currentMonth.collectAsState()
    val workoutsByDay by viewModel.workoutsByDay.collectAsState()
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    val today = remember { LocalDate.now() }
    val localeES = remember { Locale.forLanguageTag("es-ES") }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm2 by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
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
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth().padding(16.dp).clickable { viewModel.showWorkoutList() },
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
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
                                    ElevatedCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 4.dp)
                                            .clickable { viewModel.selectExercise(ex) },
                                        shape = MaterialTheme.shapes.medium,
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Row(Modifier.fillMaxWidth().padding(12.dp)) {
                                            Column(Modifier.weight(1f)) {
                                                Text(ex.name, style = MaterialTheme.typography.titleSmall)
                                                Text(Labels.muscleGroup(ex.muscleGroup),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HistoryView.WORKOUT_LIST -> {
                val firstOfMonth = remember(currentMonth) { currentMonth.atDay(1) }
                val daysInMonth = remember(currentMonth) { currentMonth.lengthOfMonth() }
                val startDow = remember(currentMonth) { firstOfMonth.dayOfWeek.value }

                val allDays = remember(currentMonth) {
                    buildList<LocalDate?> {
                        repeat(startDow - 1) { add(null) }
                        for (d in 1..daysInMonth) add(currentMonth.atDay(d))
                        val remainder = size % 7
                        if (remainder != 0) repeat(7 - remainder) { add(null) }
                    }
                }

                LaunchedEffect(currentMonth) { selectedDay = null }

                val dayWorkouts = remember(selectedDay, workoutsByDay) {
                    selectedDay?.let { workoutsByDay[it] } ?: emptyList()
                }

                LazyColumn(Modifier.padding(padding)) {
                    // --- Calendar header ---
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { viewModel.previousMonth() }) {
                                Icon(Icons.Default.ChevronLeft, "Mes anterior")
                            }
                            TextButton(onClick = { viewModel.goToToday() }) {
                                Text(
                                    "${currentMonth.month.getDisplayName(TextStyle.FULL, localeES)} ${currentMonth.year}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            IconButton(onClick = { viewModel.nextMonth() }) {
                                Icon(Icons.Default.ChevronRight, "Mes siguiente")
                            }
                        }

                        Row(Modifier.fillMaxWidth()) {
                            DayOfWeek.entries.forEach { dow ->
                                Text(
                                    dow.getDisplayName(TextStyle.NARROW, localeES),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        allDays.chunked(7).forEach { week ->
                            Row(Modifier.fillMaxWidth()) {
                                week.forEach { date ->
                                    Box(
                                        Modifier.weight(1f).aspectRatio(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (date != null) {
                                            val hasWorkout = workoutsByDay.containsKey(date)
                                            val isToday = date == today
                                            val isSelected = date == selectedDay

                                            val bgMod = when {
                                                isSelected -> Modifier.background(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape
                                                )
                                                hasWorkout -> Modifier.background(
                                                    MaterialTheme.colorScheme.surfaceVariant, CircleShape
                                                )
                                                else -> Modifier
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .then(bgMod)
                                                    .then(
                                                        if (isSelected) Modifier.border(
                                                            2.dp,
                                                            MaterialTheme.colorScheme.primary,
                                                            CircleShape
                                                        ) else Modifier
                                                    )
                                                    .clickable(enabled = hasWorkout) { selectedDay = date },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    date.dayOfMonth.toString(),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (hasWorkout) FontWeight.Bold else FontWeight.Normal,
                                                    color = when {
                                                        hasWorkout -> MaterialTheme.colorScheme.primary
                                                        isToday -> MaterialTheme.colorScheme.primary
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                    }

                    // --- Selected day header ---
                    if (selectedDay != null) {
                        item {
                            HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                thickness = 2.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${selectedDay!!.dayOfMonth} de ${selectedDay!!.month.getDisplayName(TextStyle.FULL, localeES)}",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    // --- Workout cards for selected day ---
                    if (selectedDay != null) {
                        items(dayWorkouts, key = { it.id }) { workout ->
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clickable { viewModel.selectWorkout(workout) },
                                shape = MaterialTheme.shapes.largeIncreased
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(
                                        dateFormat.format(Date(workout.date)),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (workout.notes.isNotBlank()) {
                                        Text(workout.notes, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    // --- Hint / empty state ---
                    if (selectedDay == null) {
                        item {
                            if (completedWorkouts.isEmpty()) {
                                Box(
                                    Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No hay entrenos completados aun",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            } else {
                                Box(
                                    Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Toca un día resaltado para ver los entrenos",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                            ElevatedCard(
                                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.FitnessCenter, null,
                                            Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurface)
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

                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)
                                    .clickable { showEditDialog = true },
                                shape = MaterialTheme.shapes.medium
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

                    // Add exercise button — for missed exercises
                    val exercisesInWorkout = viewModel.getExercisesInWorkout()
                    val availableExercises = allExercises.filter { it.id !in exercisesInWorkout }
                    item(key = "add-exercise") {
                        var showAddExerciseDialog by remember { mutableStateOf(false) }
                        TextButton(
                            onClick = { showAddExerciseDialog = true },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Añadir ejercicio")
                        }
                        if (showAddExerciseDialog) {
                            AddExerciseToWorkoutDialog(
                                exercises = availableExercises,
                                onDismiss = { showAddExerciseDialog = false },
                                onAdd = { exerciseId ->
                                    viewModel.addExerciseToWorkout(exerciseId)
                                    showAddExerciseDialog = false
                                }
                            )
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
            shape = MaterialTheme.shapes.large,
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
            shape = MaterialTheme.shapes.large,
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
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
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
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
        shape = MaterialTheme.shapes.large,
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
        shape = MaterialTheme.shapes.large,
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AddExerciseToWorkoutDialog(
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onAdd: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text("Añadir ejercicio") },
        text = {
            if (exercises.isEmpty()) {
                Text("Todos los ejercicios ya están en este entrenamiento.")
            } else {
                LazyColumn {
                    items(exercises) { ex ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .clickable { onAdd(ex.id) },
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(ex.name, style = MaterialTheme.typography.titleSmall)
                                    Text(Labels.muscleGroup(ex.muscleGroup),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
