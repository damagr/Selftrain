package com.selftrain.app.ui.train

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.selftrain.app.util.PlateCalculator

/** ponytail: calculadora de discos — cómo cargar la barra para el peso indicado */
@Composable
fun PlateCalculatorDialog(
    initialWeight: Float,
    onDismiss: () -> Unit
) {
    var targetText by remember { mutableStateOf(
        if (initialWeight > 0) String.format(java.util.Locale.US, "%.1f", initialWeight) else ""
    ) }
    var barKg by remember { mutableDoubleStateOf(20.0) }
    val target = targetText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val result = remember(target, barKg) {
        if (target > 0) PlateCalculator.compute(target, barKg) else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text("Calculadora de discos") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = { Text("Peso objetivo (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                // Barra: 20 / 15 / 10 kg
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = barKg == 20.0,
                        onClick = { barKg = 20.0 },
                        label = { Text("Barra 20kg") }
                    )
                    FilterChip(
                        selected = barKg == 15.0,
                        onClick = { barKg = 15.0 },
                        label = { Text("15kg") }
                    )
                    FilterChip(
                        selected = barKg == 10.0,
                        onClick = { barKg = 10.0 },
                        label = { Text("10kg") }
                    )
                }
                when {
                    target <= 0 -> {
                        Text("Introduce el peso objetivo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    target <= barKg -> {
                        Text("El peso no supera a la barra: carga solo la barra",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    result != null -> {
                        Text("Por lado:", style = MaterialTheme.typography.titleSmall)
                        if (result.perSide.isEmpty()) {
                            Text("Sin discos", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text(
                                result.perSide.joinToString(" + ") {
                                    if (it == it.toLong().toDouble()) "${it.toLong()}" else "$it"
                                } + " kg",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (result.remainderKg > 0) {
                            Text(
                                "Con los discos estándar cargarás ${String.format("%.1f", result.totalKg)} kg (faltan ${result.remainderKg} kg en total)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "Total en barra: ${String.format("%.1f", result.totalKg)} kg",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}
