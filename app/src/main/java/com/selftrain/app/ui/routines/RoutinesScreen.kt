package com.selftrain.app.ui.routines

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.selftrain.app.data.model.Routine
import com.selftrain.app.util.AppPrefs
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
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var showPredefinedDialog by rememberSaveable { mutableStateOf(false) }
    var shareRoutineId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showScanDialog by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // ponytail: bienvenida la primera vez — explica la app y ofrece cargar rutinas de ejemplo
    if (!AppPrefs.onboardingDone) {
        AlertDialog(
            onDismissRequest = { AppPrefs.setOnboardingDone(context) },
            shape = MaterialTheme.shapes.large,
            title = { Text("¡Bienvenido a SelfTrain!") },
            text = {
                Text(
                    "Registra tus entrenamientos ejercicio a ejercicio: cada serie se guarda al instante, " +
                        "el temporizador de descanso sigue contando fuera de la app y tu progreso queda en el historial.\n\n" +
                        "Si entrenas con el Método Bilbo, la app te guía con la serie de activación y las series de trabajo " +
                        "(puedes ver cómo funciona con el botón ? durante el entreno).\n\n" +
                        "¿Quieres empezar con rutinas de ejemplo (PPL, Full Body, Bilbo)?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    AppPrefs.setOnboardingDone(context)
                    showPredefinedDialog = true
                }) { Text("Cargar rutinas de ejemplo") }
            },
            dismissButton = {
                TextButton(onClick = { AppPrefs.setOnboardingDone(context) }) { Text("Explorar por mi cuenta") }
            }
        )
    }

    // Toast para resultados de import/escaneo (lectura directa: snapshot rastrea el State del VM)
    val importMessage = viewModel.importMessage
    LaunchedEffect(importMessage) {
        importMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearImportMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Rutinas") },
                windowInsets = TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    TextButton(onClick = { showPredefinedDialog = true }) {
                        Text("Cargar rutinas",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    IconButton(onClick = { showScanDialog = true }) {
                        Icon(Icons.Default.QrCodeScanner, "Escanear QR")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, "Ajustes")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, "Nuevo programa")
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

        // ponytail: tarjeta "Continuar" (F4) — el día que toca del programa con el entreno
        // más reciente (misma lógica que "Te toca" en ProgramDaysScreen, a nivel global)
        val lastWorkoutByRoutine by viewModel.lastWorkoutByRoutine.collectAsState()
        val suggested = remember(groups, lastWorkoutByRoutine) {
            var best: Pair<Routine, Routine>? = null   // (child, parent)
            var bestRecency = -1L
            groups.forEach { (parent, children) ->
                if (children.isNotEmpty()) {
                    val lastDone = children.withIndex()
                        .map { (idx, r) -> idx to (lastWorkoutByRoutine[r.id] ?: 0L) }
                        .maxByOrNull { it.second }
                    val idx = if (lastDone == null || lastDone.second == 0L) 0 else (lastDone.first + 1) % children.size
                    val recency = lastDone?.second ?: 0L
                    if (recency > bestRecency) {
                        bestRecency = recency
                        best = children[idx] to parent
                    }
                }
            }
            best
        }

        if (groups.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No hay rutinas aún",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(12.dp))
                    // ponytail: CTA directo — antes solo texto, la carga estaba escondida en la top bar
                    Button(onClick = { showPredefinedDialog = true }) {
                        Text("Cargar rutinas de ejemplo")
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "o toca + para crear la tuya",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                Modifier.padding(padding),
                contentPadding = PaddingValues(top = 8.dp)
            ) {
                // ponytail: tarjeta "Continuar: {día que toca}" (F4)
                if (suggested != null) {
                    item(key = "continue") {
                        val (child, parent) = suggested
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable { onStartWorkout(child.id) },
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Continuar: ${child.name}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        "${parent.name} · te toca",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                IconButton(onClick = { onStartWorkout(child.id) }) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        "Empezar entreno",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
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
                                IconButton(onClick = { shareRoutineId = routine.id }) {
                                    Icon(Icons.Default.QrCode2, "Compartir", tint = MaterialTheme.colorScheme.primary)
                                }
                                Icon(Icons.Default.PlayArrow, "Entrar", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    } else {
                        // Standalone routine
                        RoutineCard(
                            routine = routine,
                            onStart = { onStartWorkout(routine.id) },
                            onEdit = { onEditRoutine(routine.id) },
                            onShare = { shareRoutineId = routine.id },
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
                viewModel.createRoutine(name = name, method = method) { id -> onEnterProgram(id) }
            }
        )
    }

    if (showPredefinedDialog) {
        PredefinedRoutinesDialog(
            viewModel = viewModel,
            onDismiss = { showPredefinedDialog = false }
        )
    }

    // Compartir por QR
    shareRoutineId?.let { id ->
        val routine = routines.find { it.id == id }
        if (routine != null) {
            ShareRoutineDialog(
                routineId = id,
                routineName = routine.name,
                viewModel = viewModel,
                onDismiss = { shareRoutineId = null }
            )
        } else {
            shareRoutineId = null
        }
    }

    if (showScanDialog) {
        ScanQrDialog(
            onScanned = { payload ->
                showScanDialog = false
                viewModel.onQrScanned(payload)
            },
            onDismiss = { showScanDialog = false }
        )
    }

    viewModel.pendingImport?.let { shared ->
        ImportRoutineDialog(
            shared = shared,
            onConfirm = { viewModel.confirmImport() },
            onDismiss = { viewModel.cancelImport() }
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
                if (routine == null) {
                    // ponytail: la rutina del entreno huérfano ya no existe — reanudar llevaría
                    // a un spinner infinito, así que solo se ofrece descartar
                    Text("Tienes un entreno sin finalizar de una rutina que ya no existe. ¿Quieres descartarlo?")
                } else {
                    Text("Tienes un entreno sin finalizar de \"${routine.name}\". ¿Quieres reanudarlo donde lo dejaste?")
                }
            },
            confirmButton = {
                if (routine != null) {
                    TextButton(onClick = {
                        viewModel.showRecoveryDialog = false
                        onResumeWorkout(uw.routineId, uw.id)
                    }) {
                        Text("Reanudar")
                    }
                } else {
                    TextButton(onClick = {
                        viewModel.showRecoveryDialog = false
                        viewModel.discardUnfinishedWorkout(uw.id)
                    }) {
                        Text("Descartar", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                if (routine != null) {
                    TextButton(onClick = {
                        viewModel.showRecoveryDialog = false
                        viewModel.discardUnfinishedWorkout(uw.id)
                    }) {
                        Text("Descartar", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
    }
}

@Composable
fun RoutineCard(
    routine: Routine,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
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
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(routine.name, style = MaterialTheme.typography.titleMedium)
                Text("Método: ${Labels.method(routine.method)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.QrCode2, "Compartir", tint = MaterialTheme.colorScheme.primary)
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
        title = { Text("Nuevo Programa") },
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
