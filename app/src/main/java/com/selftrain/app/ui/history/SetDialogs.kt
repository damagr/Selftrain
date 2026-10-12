package com.selftrain.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.selftrain.app.data.model.Exercise
import com.selftrain.app.data.model.WorkoutSet
import com.selftrain.app.util.Labels

// ponytail: diálogos inline de edición del detalle de entreno — editar serie, añadir serie,
// añadir ejercicio. Extraídos de HistoryScreen (refactor F1).

@Composable
fun EditSetDialog(
    set: WorkoutSet,
    onDismiss: () -> Unit,
    onSave: (setType: String, reps: Int, weight: Float, rir: Int, notes: String) -> Unit
) {
    var reps by remember { mutableStateOf(set.reps.toString()) }
    // ponytail: Locale.US — con locale ES, "%.1f" produce "12,5" y toFloatOrNull() falla (guardado mudo)
    var weight by remember { mutableStateOf(String.format(java.util.Locale.US, "%.1f", set.weightKg)) }
    var rir by remember { mutableStateOf(if (set.rir > 0) set.rir.toString() else "") }
    var setType by remember { mutableStateOf(set.setType) }
    var notes by remember { mutableStateOf(set.notes) }

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
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Nota (opcional)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val r = reps.toIntOrNull() ?: return@TextButton
                val w = weight.replace(",", ".").toFloatOrNull() ?: return@TextButton
                val ri = rir.toIntOrNull() ?: 0
                onSave(setType, r, w, ri, notes.trim())
            }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

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
