package com.selftrain.app.ui.routines

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.selftrain.app.data.model.Exercise
import com.selftrain.app.util.Labels
import com.selftrain.app.util.getExerciseGifUrl

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RoutineEditScreen(
    routineId: Long,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: RoutineEditViewModel = hiltViewModel()
) {
    val routine by viewModel.routine.collectAsState()
    val exercises by viewModel.routineExercises.collectAsState()
    val allExercises by viewModel.allExercises.collectAsState()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var replaceIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    // ponytail: descanso configurable por ejercicio
    var restEditItem by remember { mutableStateOf<RoutineExerciseItem?>(null) }

    LaunchedEffect(routineId) { viewModel.loadRoutine(routineId) }

    // Toasts de validación (ej: falta Serie Bilbo, mismo grupo muscular)
    val context = LocalContext.current
    val validationMessage = viewModel.message
    LaunchedEffect(validationMessage) {
        validationMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal),
                title = { Text(routine?.name ?: "Editar Rutina") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = { viewModel.save(onSaved) }) {
                        Icon(Icons.Default.Check, "Guardar")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            routine?.let { r ->
                OutlinedTextField(
                    value = r.name,
                    onValueChange = { viewModel.updateName(it) },
                    label = { Text("Nombre de la rutina") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true
                )
            }

            Text(
                "Ejercicios de la rutina (arrastra para reordenar)",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ponytail: weight(1f) — antes sin weight la lista ocupaba todo y el botón
            // "Eliminar rutina" quedaba fuera de pantalla
            LazyColumn(Modifier.weight(1f)) {
                itemsIndexed(exercises, key = { _, item -> item.exercise.id }) { index, item ->
                    val ex = item.exercise
                    val isFirst = index == 0
                    val isLast = index == exercises.size - 1

                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .animateItem(),
                        shape = MaterialTheme.shapes.largeIncreased,
                        colors = if (item.isBilbo) CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ) else CardDefaults.cardColors()
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Reorder buttons
                            Column {
                                if (!isFirst) {
                                    IconButton(
                                        onClick = { viewModel.moveExercise(index, index - 1) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowUp, "Subir", Modifier.size(20.dp))
                                    }
                                }
                                if (!isLast) {
                                    IconButton(
                                        onClick = { viewModel.moveExercise(index, index + 1) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowDown, "Bajar", Modifier.size(20.dp))
                                    }
                                }
                            }
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(ex.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${Labels.category(ex.category)} · ${Labels.muscleGroup(ex.muscleGroup)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                // ponytail: el switch Bilbo solo existe en rutinas con método Bilbo
                                if (ex.isBilboEligible && routine?.method.equals("bilbo", true)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked = item.isBilbo,
                                            onCheckedChange = { viewModel.toggleBilbo(item) }
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "Serie Bilbo",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (item.isBilbo) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    // ponytail: explica la regla — onboarding in-context (F3)
                                    Text(
                                        "Activación explosiva 15-50 reps al ~50% 1RM. Máximo una por grupo muscular.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                // ponytail: descanso configurable por ejercicio (0 = default por categoría)
                                Text(
                                    if (item.restSeconds > 0) "Descanso: ${item.restSeconds}s"
                                    else "Descanso: Auto (por categoría)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (item.restSeconds > 0) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .clickable { restEditItem = item }
                                )
                            }

                            IconButton(onClick = { replaceIndex = index }) {
                                Icon(Icons.Default.Edit, "Reemplazar", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { viewModel.removeExercise(item) }) {
                                Icon(Icons.Default.Close, "Quitar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ponytail: añadir como botón anclado — el FAB colisionaba con las X de la
            // última tarjeta y tapaba acciones en listas cortas
            FilledTonalButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Añadir ejercicio")
            }

            Spacer(Modifier.height(16.dp))

            // Delete routine button
            OutlinedButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, null)
                Spacer(Modifier.width(8.dp))
                Text("Eliminar rutina")
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // ponytail: editar el descanso de un ejercicio concreto
    restEditItem?.let { item ->
        var restText by remember(item.exercise.id) {
            mutableStateOf(if (item.restSeconds > 0) item.restSeconds.toString() else "")
        }
        AlertDialog(
            onDismissRequest = { restEditItem = null },
            shape = MaterialTheme.shapes.large,
            title = { Text("Descanso: ${item.exercise.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Segundos de descanso tras registrar una serie de este ejercicio. Déjalo vacío para usar el default por categoría (compuesto 150s, aislamiento 105s).",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = restText,
                        onValueChange = { restText = it.filter { c -> c.isDigit() } },
                        label = { Text("Segundos (30-300)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setRest(item, restText.toIntOrNull() ?: 0)
                    restEditItem = null
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { restEditItem = null }) { Text("Cancelar") }
            }
        )
    }

    if (showAddDialog) {
        ExercisePickerDialog(
            exercises = allExercises.filter { ex -> exercises.none { it.exercise.id == ex.id } },
            onDismiss = { showAddDialog = false },
            onSelectMany = { selected ->
                selected.forEach { viewModel.addExercise(it) }
                showAddDialog = false
            }
        )
    }

    replaceIndex?.let { idx ->
        ExercisePickerDialog(
            exercises = allExercises,
            onDismiss = { replaceIndex = null },
            onSelectMany = { selected ->
                if (selected.isNotEmpty()) {
                    viewModel.replaceExercise(idx, selected.first())
                }
                replaceIndex = null
            },
            singleSelect = true
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            shape = MaterialTheme.shapes.large,
            title = { Text("¿Eliminar rutina?") },
            text = { Text("¿Seguro que quieres eliminar \"${routine?.name}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteRoutine(onDeleted)
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExercisePickerDialog(
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onSelectMany: (List<Exercise>) -> Unit,
    singleSelect: Boolean = false
) {
    val selectedIds = remember { mutableStateListOf<Long>() }
    var showGifExercise by remember { mutableStateOf<Exercise?>(null) }
    var query by remember { mutableStateOf("") }
    val filtered = if (query.isBlank()) exercises
        else exercises.filter { it.name.contains(query, ignoreCase = true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(if (singleSelect) "Reemplazar Ejercicio" else "Añadir Ejercicios") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Cerrar")
                        }
                    },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                    actions = {
                        if (selectedIds.isNotEmpty()) {
                            Text(
                                "${selectedIds.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        IconButton(
                            onClick = {
                                val selected = if (singleSelect) {
                                    exercises.filter { it.id in selectedIds }
                                } else {
                                    filtered.filter { it.id in selectedIds }
                                }
                                if (selected.isNotEmpty()) onSelectMany(selected)
                            },
                            enabled = selectedIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Check, "Aceptar")
                        }
                    }
                )
            }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Buscar ejercicio...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, "Limpiar")
                            }
                        }
                    }
                )
                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Sin resultados")
                    }
                } else {
                    LazyColumn(Modifier.weight(1f)) {
                        val grouped = filtered.groupBy { it.muscleGroup }
                        for ((group, exs) in grouped) {
                            item {
                                Text(
                                    Labels.muscleGroup(group),
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 16.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            items(exs) { ex ->
                                val isSelected = ex.id in selectedIds
                                ElevatedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 2.dp)
                                        .clickable {
                                            if (singleSelect) {
                                                selectedIds.clear()
                                                selectedIds.add(ex.id)
                                            } else {
                                                if (isSelected) selectedIds.remove(ex.id)
                                                else selectedIds.add(ex.id)
                                            }
                                        },
                                    shape = MaterialTheme.shapes.medium,
                                    colors = if (isSelected) CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ) else CardDefaults.cardColors()
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(ex.name, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                buildString {
                                                    if (ex.equipment.isNotEmpty()) {
                                                        append(Labels.equipment(ex.equipment))
                                                        append(" · ")
                                                    }
                                                    append(Labels.category(ex.category))
                                                    if (ex.isBilboEligible) append(" · Bilbo")
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (singleSelect) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = {
                                                    selectedIds.clear()
                                                    selectedIds.add(ex.id)
                                                }
                                            )
                                        } else {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = {
                                                    if (isSelected) selectedIds.remove(ex.id)
                                                    else selectedIds.add(ex.id)
                                                }
                                            )
                                        }
                                        IconButton(
                                            onClick = { showGifExercise = ex },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.PlayArrow,
                                                "Ver demostración",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // Exercise GIF demonstration dialog
            showGifExercise?.let { ex ->
                val gifUrl = ex.gifUrl ?: getExerciseGifUrl(ex.name)
                AlertDialog(
                    onDismissRequest = { showGifExercise = null },
                    shape = MaterialTheme.shapes.large,
                    title = { Text(ex.name) },
                    text = {
                        if (gifUrl != null) {
                            AsyncImage(
                                model = gifUrl,
                                contentDescription = "Demostración de ${ex.name}",
                                modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text("No hay demostración disponible para este ejercicio.")
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showGifExercise = null }) {
                            Text("Cerrar")
                        }
                    }
                )
            }
        }
    }
}
