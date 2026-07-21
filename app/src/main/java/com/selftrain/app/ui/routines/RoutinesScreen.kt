package com.selftrain.app.ui.routines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.selftrain.app.data.model.Routine
import com.selftrain.app.util.Labels
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RoutinesScreen(
    onStartWorkout: (Long) -> Unit,
    onEditRoutine: (Long) -> Unit,
    onEnterProgram: (Long) -> Unit,
    onSettings: () -> Unit,
    onResumeWorkout: (routineId: Long, workoutId: Long) -> Unit = { _, _ -> },
    viewModel: RoutinesViewModel = hiltViewModel()
) {
    val routines by viewModel.routines.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showPredefinedDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Rutinas") },
                windowInsets = TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    TextButton(onClick = { showPredefinedDialog = true }) {
                        Text("Cargar rutinas", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, "Ajustes")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, "Nueva rutina")
            }
        }
    ) { padding ->
        val groups = remember(routines) {
            val topLevel = routines.filter { it.parentId == null }.sortedBy { it.order }
            topLevel.map { parent ->
                val children = routines.filter { it.parentId == parent.id }
                parent to children
            }
        }

        if (groups.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No hay rutinas aún.\nToca + para crear una.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(Modifier.padding(padding)) {
                items(groups.size) { index ->
                    val (routine, children) = groups[index]
                    if (children.isNotEmpty()) {
                        // Parent — tap to navigate
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable { onEnterProgram(routine.id) }
                                .animateItem(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(routine.name, style = MaterialTheme.typography.titleMedium)
                                    Text("${children.size} días · ${Labels.method(routine.method)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Default.PlayArrow, "Entrar", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    } else {
                        // Standalone routine
                        RoutineCard(
                            routine = routine,
                            index = index,
                            total = groups.size,
                            onStart = { onStartWorkout(routine.id) },
                            onEdit = { onEditRoutine(routine.id) },
                            onMove = { direction ->
                                viewModel.moveRoutine(index, direction,
                                    groups.map { it.first })
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateRoutineDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, method ->
                showCreateDialog = false
                viewModel.createRoutine(name = name, method = method) { id -> onEditRoutine(id) }
            }
        )
    }

    if (showPredefinedDialog) {
        PredefinedRoutinesDialog(
            viewModel = viewModel,
            onDismiss = { showPredefinedDialog = false }
        )
    }

    // Crash recovery dialog — state lives in ViewModel, user MUST choose
    if (viewModel.showRecoveryDialog && viewModel.unfinishedWorkout != null) {
        val uw = viewModel.unfinishedWorkout!!
        val routine = routines.find { it.id == uw.routineId }
        AlertDialog(
            onDismissRequest = { /* bloqueado: el usuario debe elegir */ },
            shape = MaterialTheme.shapes.large,
            title = { Text("Entreno sin finalizar") },
            text = {
                Text("Tienes un entreno sin finalizar de \"${routine?.name ?: "rutina"}\". ¿Quieres reanudarlo donde lo dejaste?")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.showRecoveryDialog = false
                    onResumeWorkout(uw.routineId, uw.id)
                }) {
                    Text("Reanudar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.showRecoveryDialog = false
                    viewModel.discardUnfinishedWorkout(uw.id)
                }) {
                    Text("Descartar", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}

@Composable
fun RoutineCard(
    routine: Routine,
    index: Int,
    total: Int,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onMove: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.largeIncreased
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reorder buttons
            Column {
                if (index > 0) {
                    IconButton(onClick = { onMove(-1) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.KeyboardArrowUp, "Subir", Modifier.size(18.dp))
                    }
                }
                if (index < total - 1) {
                    IconButton(onClick = { onMove(1) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.KeyboardArrowDown, "Bajar", Modifier.size(18.dp))
                    }
                }
            }
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(routine.name, style = MaterialTheme.typography.titleMedium)
                Text("Método: ${Labels.method(routine.method)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onStart) {
                Icon(Icons.Default.PlayArrow, "Empezar", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "Editar")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CreateRoutineDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, method: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val methods = listOf("bilbo", "full_body", "ppl")
    val methodLabels = methods.map { Labels.method(it) }
    var selectedMethod by remember { mutableStateOf("bilbo") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text("Nueva Rutina") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Method selector dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = Labels.method(selectedMethod),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Método") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        methods.forEachIndexed { index, method ->
                            DropdownMenuItem(
                                text = { Text(methodLabels[index]) },
                                onClick = {
                                    selectedMethod = method
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onCreate(name.trim(), selectedMethod) }) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PredefinedRoutinesDialog(
    viewModel: RoutinesViewModel,
    onDismiss: () -> Unit
) {
    val programs = remember { viewModel.loadPredefinedPrograms() }
    var showConfirm by remember { mutableStateOf<PredefinedProgram?>(null) }

    if (showConfirm != null) {
        val program = showConfirm!!
        AlertDialog(
            onDismissRequest = { showConfirm = null },
            shape = MaterialTheme.shapes.large,
            title = { Text(program.program) },
            text = {
                Column {
                    Text("Se crearán ${program.routines.size} rutinas:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    program.routines.forEach { r ->
                        Text("• ${r.name} (${r.exercises.size} ejercicios)",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createRoutinesFromProgram(program) {
                        showConfirm = null
                        onDismiss()
                    }
                }) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = null }) { Text("Cancelar") }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = MaterialTheme.shapes.large,
            title = { Text("Cargar rutinas predefinidas") },
            text = {
                LazyColumn(Modifier.height(400.dp)) {
                    items(programs) { program ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .clickable { showConfirm = program },
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(program.program, style = MaterialTheme.typography.titleSmall)
                                    Text("${program.routines.size} días · ${Labels.method(program.method)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        )
    }
}
