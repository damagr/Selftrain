package com.selftrain.app.ui.exercises

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.selftrain.app.data.model.Exercise
import com.selftrain.app.util.Labels
import com.selftrain.app.util.getExerciseGifUrl

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExerciseLibraryScreen(
    onSettings: () -> Unit,
    viewModel: ExerciseLibraryViewModel = hiltViewModel()
) {
    val exercises by viewModel.exercises.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<Exercise?>(null) }
    var showGifExercise by remember { mutableStateOf<Exercise?>(null) }
    var deleteMode by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal),
                colors = if (deleteMode) TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    titleContentColor = MaterialTheme.colorScheme.onErrorContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onErrorContainer
                ) else TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    Column {
                        Text("Biblioteca de Ejercicios")
                        if (deleteMode) {
                            Text("Modo borrado — selecciona un ejercicio para eliminarlo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { deleteMode = !deleteMode }) {
                        Icon(
                            Icons.Default.Delete,
                            "Modo borrado",
                            tint = if (deleteMode) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, "Ajustes")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, "Añadir ejercicio")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                ContainedLoadingIndicator(
                    containerShape = MaterialTheme.shapes.medium,
                    polygons = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons
                )
            }
        } else {
            val grouped = exercises.groupBy { it.muscleGroup }

            LazyColumn(Modifier.padding(padding)) {
                grouped.entries.forEach { (group, exs) ->
                    item {
                        Text(
                            Labels.muscleGroup(group),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(exs, key = { it.id }) { ex ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                        .combinedClickable(
                                            onClick = { showGifExercise = ex },
                                            onLongClick = { showDeleteConfirm = ex }
                                        ),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
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
                                if (deleteMode) {
                                    IconButton(onClick = { showDeleteConfirm = ex }) {
                                        Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
                                    }
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
        val gifUrl = getExerciseGifUrl(ex.name)
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

    if (showCreateDialog) {
        CreateExerciseDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, muscleGroup, category, bilbo, equipment ->
                viewModel.addExercise(name, muscleGroup, category, bilbo, equipment)
                showCreateDialog = false
            }
        )
    }

    showDeleteConfirm?.let { ex ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            shape = MaterialTheme.shapes.large,
            title = { Text("¿Eliminar ejercicio?") },
            text = { Text("¿Seguro que quieres eliminar \"${ex.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteExercise(ex)
                    showDeleteConfirm = null
                    deleteMode = false
                    Toast.makeText(context, "Ejercicio eliminado. Puedes recuperarlo en Ajustes.", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancelar") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CreateExerciseDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, muscleGroup: String, category: String, isBilbo: Boolean, equipment: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val muscleGroups = listOf("pecho", "piernas", "espalda", "hombros", "brazos", "core")
    var selectedMuscle by remember { mutableStateOf(muscleGroups[0]) }
    val categories = listOf("compound", "isolation")
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var isBilbo by remember { mutableStateOf(false) }
    var muscleExpanded by remember { mutableStateOf(false) }
    var catExpanded by remember { mutableStateOf(false) }
    val equipments = listOf("barbell", "dumbbell", "cable", "machine", "bodyweight")
    var selectedEquipment by remember { mutableStateOf(equipments[0]) }
    var equipExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.85f),
        shape = MaterialTheme.shapes.large,
        title = { Text("Nuevo Ejercicio") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Muscle group
                ExposedDropdownMenuBox(expanded = muscleExpanded, onExpandedChange = { muscleExpanded = it }) {
                    OutlinedTextField(
                        value = Labels.muscleGroup(selectedMuscle),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Grupo muscular") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(muscleExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = muscleExpanded, onDismissRequest = { muscleExpanded = false }) {
                        muscleGroups.forEach { mg ->
                            DropdownMenuItem(
                                text = { Text(Labels.muscleGroup(mg)) },
                                onClick = { selectedMuscle = mg; muscleExpanded = false }
                            )
                        }
                    }
                }

                // Category
                ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                    OutlinedTextField(
                        value = Labels.category(selectedCategory),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(Labels.category(cat)) },
                                onClick = { selectedCategory = cat; catExpanded = false }
                            )
                        }
                    }
                }

                // Bilbo toggle (only for compound)
                if (selectedCategory == "compound") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("¿Aplica método Bilbo?")
                        Switch(checked = isBilbo, onCheckedChange = { isBilbo = it })
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Equipment
                ExposedDropdownMenuBox(expanded = equipExpanded, onExpandedChange = { equipExpanded = it }) {
                    OutlinedTextField(
                        value = Labels.equipment(selectedEquipment),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Equipamiento") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(equipExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = equipExpanded, onDismissRequest = { equipExpanded = false }) {
                        equipments.forEach { eq ->
                            DropdownMenuItem(
                                text = { Text(Labels.equipment(eq)) },
                                onClick = { selectedEquipment = eq; equipExpanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name.trim(), selectedMuscle, selectedCategory, isBilbo, selectedEquipment)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Añadir")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
