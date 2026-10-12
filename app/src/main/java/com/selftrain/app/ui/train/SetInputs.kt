package com.selftrain.app.ui.train

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.selftrain.app.data.model.WorkoutSet

// ponytail: inputs de registro de series (Bilbo y trabajo) con nota opcional y
// calculadora de discos. Extraído de TrainScreen (refactor F1).

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
