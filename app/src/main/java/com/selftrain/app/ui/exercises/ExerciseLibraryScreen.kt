package com.selftrain.app.ui.exercises

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
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
import com.selftrain.app.data.model.Exercise
import com.selftrain.app.util.Labels

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExerciseLibraryScreen(
    onSettings: () -> Unit,
    viewModel: ExerciseLibraryViewModel = hiltViewModel()
) {
    val exercises by viewModel.exercises.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<Exercise?>(null) }
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
                colors = if (deleteMode) TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    titleContentColor = MaterialTheme.colorScheme.onErrorContainer
                ) else TopAppBarDefaults.topAppBarColors(),
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
        val grouped = exercises.groupBy { it.muscleGroup }

        LazyColumn(Modifier.padding(padding)) {
            grouped.entries.forEach { (group, exs) ->
                item {
                    Text(
                        Labels.muscleGroup(group),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(exs, key = { it.id }) { ex ->
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
                        trailingContent = {
                            if (deleteMode) {
                                IconButton(onClick = { showDeleteConfirm = ex }) {
                                    Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        },
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { showDeleteConfirm = ex }
                            )
                    )
                }
            }
        }
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

@OptIn(ExperimentalMaterial3Api::class)
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
        title = { Text("Nuevo Ejercicio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
