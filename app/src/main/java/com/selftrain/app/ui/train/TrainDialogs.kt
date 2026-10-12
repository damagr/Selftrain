package com.selftrain.app.ui.train

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.selftrain.app.util.Labels

// ponytail: diálogo de resumen al finalizar entreno (duración, peso corporal, volumen,
// récords, comparativa semanal + compartir). Extraído de TrainScreen (refactor F1).

@Composable
fun WorkoutSummaryDialog(
    summary: WorkoutSummary,
    onUpdateDuration: (Int) -> Unit,
    onUpdateBodyWeight: (Float) -> Unit,
    onDismiss: () -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    var editDuration by remember { mutableStateOf(summary.durationMinutes.toString()) }
    var editBodyWeight by remember {
        mutableStateOf(if (summary.bodyWeight > 0) String.format(java.util.Locale.US, "%.1f", summary.bodyWeight) else "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text("¡Entreno completado!") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Duration
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Duración: ")
                    OutlinedTextField(
                        value = editDuration,
                        onValueChange = { v ->
                            editDuration = v.filter { ch -> ch.isDigit() }
                            v.toIntOrNull()?.let(onUpdateDuration)
                        },
                        label = { Text("min") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(100.dp),
                        singleLine = true
                    )
                }

                // ponytail: peso corporal opcional (para fuerza relativa 1RM/kg)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Peso corporal: ")
                    OutlinedTextField(
                        value = editBodyWeight,
                        onValueChange = { v ->
                            editBodyWeight = v.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
                            v.replace(",", ".").toFloatOrNull()?.let(onUpdateBodyWeight)
                            if (v.isEmpty()) onUpdateBodyWeight(0f)
                        },
                        label = { Text("kg (opcional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.width(100.dp),
                        singleLine = true
                    )
                }

                // Volume per muscle group
                if (summary.volumeByMuscle.isNotEmpty()) {
                    Text("Volumen por grupo muscular", style = MaterialTheme.typography.titleSmall)
                    summary.volumeByMuscle.forEach { mgv ->
                        Text(
                            "${Labels.muscleGroup(mgv.muscleGroup)}: %,.1f kg (${mgv.setCount} series)".format(
                                java.util.Locale.getDefault(), mgv.totalKg
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Personal records
                if (summary.personalRecords.isNotEmpty()) {
                    Text("¡Nuevos récords!", style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface)
                    summary.personalRecords.forEach { pr ->
                        Text(
                            "${pr.exerciseName}: %,.1f → %,.1f kg (1RM)".format(
                                java.util.Locale.getDefault(),
                                pr.previousBest, pr.newRecord
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Weekly comparison
                if (summary.weeklyComparison.isNotEmpty()) {
                    Text("vs semana pasada", style = MaterialTheme.typography.titleSmall)
                    summary.weeklyComparison.forEach { wc ->
                        val pct = if (wc.lastWeekKg > 0) {
                            "%,.0f%%".format(java.util.Locale.getDefault(), ((wc.thisWorkoutKg - wc.lastWeekKg) / wc.lastWeekKg) * 100)
                        } else "—"
                        Text(
                            "${Labels.muscleGroup(wc.muscleGroup)}: ${"%,.1f".format(java.util.Locale.getDefault(), wc.thisWorkoutKg)} kg → $pct vs semana pasada",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row {
                // ponytail: compartir el resumen del entreno como texto
                TextButton(onClick = {
                    val shareText = buildString {
                        appendLine("Entreno completado")
                        appendLine("Duración: ${summary.durationMinutes} min")
                        if (summary.bodyWeight > 0f) {
                            appendLine("Peso corporal: ${String.format("%.1f", summary.bodyWeight)} kg")
                        }
                        if (summary.volumeByMuscle.isNotEmpty()) {
                            appendLine("Volumen por grupo muscular:")
                            summary.volumeByMuscle.forEach {
                                appendLine("  - ${Labels.muscleGroup(it.muscleGroup)}: ${String.format("%.1f", it.totalKg)} kg (${it.setCount} series)")
                            }
                        }
                        if (summary.personalRecords.isNotEmpty()) {
                            appendLine("Nuevos récords (1RM estimado):")
                            summary.personalRecords.forEach {
                                appendLine("  - ${it.exerciseName}: ${String.format("%.1f", it.newRecord)} kg (antes ${String.format("%.1f", it.previousBest)})")
                            }
                        }
                    }
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(send, "Compartir resumen"))
                }) {
                    Text("Compartir")
                }
                TextButton(onClick = onFinish) {
                    Text("Volver a rutinas")
                }
            }
        },
        dismissButton = null
    )
}
