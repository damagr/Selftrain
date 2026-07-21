package com.selftrain.app.ui.routines

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.selftrain.app.data.model.Routine

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProgramDaysScreen(
    routineId: Long,
    onStartWorkout: (Long) -> Unit,
    onEditRoutine: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: RoutinesViewModel = hiltViewModel()
) {
    val allRoutines by viewModel.routines.collectAsState()
    val parent = allRoutines.find { it.id == routineId }
    val children = allRoutines.filter { it.parentId == routineId }.sortedBy { it.order }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddDayDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDayDialog = true }) {
                Icon(Icons.Default.Add, "Añadir día")
            }
        },
        topBar = {
            TopAppBar(
                windowInsets = TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal),
                title = { Text(parent?.name ?: "Programa") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, "Eliminar programa",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        if (children.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No hay días en este programa", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(Modifier.padding(padding)) {
                items(children, key = { it.id }) { child ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = MaterialTheme.shapes.largeIncreased
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(child.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            IconButton(onClick = { onStartWorkout(child.id) }) {
                                Icon(Icons.Default.PlayArrow, "Empezar", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onEditRoutine(child.id) }) {
                                Icon(Icons.Default.Edit, "Editar")
                            }
                        }
                    }
                }
            }
        }
    }

    // Add new day dialog
    if (showAddDayDialog && parent != null) {
        var dayName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDayDialog = false },
            shape = MaterialTheme.shapes.large,
            title = { Text("Nuevo día") },
            text = {
                OutlinedTextField(
                    value = dayName,
                    onValueChange = { dayName = it },
                    label = { Text("Nombre del día") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (dayName.isNotBlank()) {
                        viewModel.createChildRoutine(parent!!.id, dayName.trim()) {
                            showAddDayDialog = false
                        }
                    }
                }) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDayDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Delete confirmation
    if (showDeleteConfirm && parent != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            shape = MaterialTheme.shapes.large,
            title = { Text("¿Eliminar programa?") },
            text = {
                Text("Se eliminará \"${parent!!.name}\" y todos sus ${children.size} días. Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteRoutine(parent!!)
                    onBack()
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
