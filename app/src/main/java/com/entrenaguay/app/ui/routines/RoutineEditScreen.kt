package com.entrenaguay.app.ui.routines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.entrenaguay.app.data.model.Exercise
import com.entrenaguay.app.util.Labels

@OptIn(ExperimentalMaterial3Api::class)
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
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(routineId) { viewModel.loadRoutine(routineId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(routine?.name ?: "Editar Rutina") },
                actions = {
                    IconButton(onClick = { viewModel.save(onSaved) }) {
                        Icon(Icons.Default.Check, "Guardar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Añadir ejercicio")
            }
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

            LazyColumn {
                items(exercises.size) { index ->
                    val ex = exercises[index]
                    val isFirst = index == 0
                    val isLast = index == exercises.size - 1

                    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
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
                                if (ex.isBilboEligible) {
                                    Text("Bilbo", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            IconButton(onClick = { viewModel.removeExercise(ex) }) {
                                Icon(Icons.Default.Close, "Quitar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
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

    if (showAddDialog) {
        ExercisePickerDialog(
            exercises = allExercises.filter { ex -> exercises.none { it.id == ex.id } },
            onDismiss = { showAddDialog = false },
            onSelectMany = { selected ->
                selected.forEach { viewModel.addExercise(it) }
                showAddDialog = false
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerDialog(
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onSelectMany: (List<Exercise>) -> Unit
) {
    val selectedIds = remember { mutableStateListOf<Long>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Añadir Ejercicios")
                if (selectedIds.isNotEmpty()) {
                    Text(
                        "${selectedIds.size} seleccionados",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            if (exercises.isEmpty()) {
                Text("Todos los ejercicios ya están en la rutina")
            } else {
                LazyColumn(Modifier.height(400.dp)) {
                    val grouped = exercises.groupBy { it.muscleGroup }
                    for ((group, exs) in grouped) {
                        item {
                            Text(
                                Labels.muscleGroup(group),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(exs) { ex ->
                            val isSelected = ex.id in selectedIds
                            ListItem(
                                headlineContent = { Text(ex.name) },
                                supportingContent = {
                                    Text(buildString {
                                        if (ex.equipment.isNotEmpty()) {
                                            append(Labels.equipment(ex.equipment))
                                            append(" · ")
                                        }
                                        append(Labels.category(ex.category))
                                        if (ex.isBilboEligible) append(" · Bilbo")
                                    })
                                },
                                leadingContent = {
                                    val icon = when (ex.equipment) {
                                        "barbell" -> Icons.Filled.FitnessCenter
                                        "dumbbell" -> Icons.Filled.MonitorWeight
                                        "cable" -> Icons.Filled.Cable
                                        "machine" -> Icons.Filled.PrecisionManufacturing
                                        "bodyweight" -> Icons.Filled.AccessibilityNew
                                        else -> Icons.Filled.FitnessCenter
                                    }
                                    Icon(icon, contentDescription = null)
                                },
                                trailingContent = {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            if (isSelected) selectedIds.remove(ex.id)
                                            else selectedIds.add(ex.id)
                                        }
                                    )
                                },
                                modifier = Modifier.clickable {
                                    if (isSelected) selectedIds.remove(ex.id)
                                    else selectedIds.add(ex.id)
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val selected = exercises.filter { it.id in selectedIds }
                    if (selected.isNotEmpty()) onSelectMany(selected)
                },
                enabled = selectedIds.isNotEmpty()
            ) {
                Text("Añadir (${selectedIds.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
