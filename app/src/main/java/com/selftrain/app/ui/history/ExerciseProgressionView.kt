package com.selftrain.app.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.selftrain.app.data.model.Exercise

// ponytail: progreso de un ejercicio — 1RM máximo + historial de series.
// Extraído de HistoryScreen (refactor F1).

@Composable
fun ExerciseProgressionView(
    exercise: Exercise,
    history: List<com.selftrain.app.data.db.SetWithExercise>,
    max1RM: Float,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier.padding(16.dp)) {
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // ponytail: color del tema (antes ámbar hardcodeado — rompía el modo claro)
                        Icon(Icons.Default.EmojiEvents, null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(8.dp))
                        Text("1RM estimado max: ${String.format("%.1f", max1RM)} kg",
                            style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (history.isEmpty()) {
            item {
                Text("Sin datos aún. Haz tu primer entreno!",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp))
            }
        } else {
            item {
                Text("Historial de series", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
            }

            history.reversed().forEach { s ->
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(Modifier.padding(12.dp)) {
                            val label = if (s.set.setType == "bilbo") "Bilbo" else "Trabajo"
                            Text("$label: ${s.set.reps} reps \u00D7 ${String.format("%.1f", s.set.weightKg)} kg" +
                                if (s.set.rir > 0) " (RIR ${s.set.rir})" else "",
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
