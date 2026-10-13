package com.selftrain.app.ui.train

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.selftrain.app.data.model.WorkoutSet
import com.selftrain.app.util.BilboProgression

// ponytail: secciones LazyColumn del ejercicio actual — series de hoy, hints de progresión
// y tarjeta de input. Extraído de TrainScreen (refactor F1).

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun LazyListScope.exerciseSetItems(
    scrollToInput: () -> Unit = {},
    appliesBilbo: Boolean,
    isDumbbell: Boolean,
    sets: List<WorkoutSet>,
    suggestion: PerExerciseSuggestion,
    exerciseKey: String,
    onLogBilboSet: (reps: Int, weight: Float, rir: Int, notes: String) -> Unit,
    onLogWorkSet: (reps: Int, weight: Float, rir: Int, notes: String) -> Unit,
    onDeleteLastSet: () -> Unit
) {
    val lastWorkSet = sets.lastOrNull { it.setType == "work" }
    val intraAdjustment = lastWorkSet?.let {
        BilboProgression.workSetAdjustment(it.reps, it.rir, it.weightKg)?.let { (prog, w) ->
            prog to (if (isDumbbell) BilboProgression.roundToDumbbellStep(w) else w)
        }
    }
    val bilboDone = sets.any { it.setType == "bilbo" }

    // Exercise info
    item(key = "$exerciseKey-info") {
        if (appliesBilbo) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Whatshot, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text("Serie Bilbo + series de trabajo",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }

    // Logged sets
    if (sets.isNotEmpty()) {
        item(key = "$exerciseKey-sets") {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Series de hoy:", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(4.dp))
                    sets.forEachIndexed { i, set ->
                        val label = if (appliesBilbo && i == 0) "Bilbo" else "Set ${if (appliesBilbo) i else i + 1}"
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 1.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("$label: ${set.reps} reps \u00D7 ${String.format("%.1f", set.weightKg)} kg",
                                style = MaterialTheme.typography.bodyMedium)
                            if (set.setType == "bilbo") {
                                Text("RIR ${set.rir}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        // ponytail: nota de la serie, visible en la sesión
                        if (set.notes.isNotBlank()) {
                            Text(
                                "↳ ${set.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Bilbo 50+ progression hint
    val lastSet = sets.lastOrNull()
    if (lastSet?.setType == "bilbo" && lastSet.reps >= 50) {
        item(key = "$exerciseKey-hint50") {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("¡50 reps alcanzadas!", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Próxima sesión: sube peso un ${if (lastSet.rir == 0) "15" else "10"}% y resetea a 15-20 reps",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    // Intra-session advice
    intraAdjustment?.let { (progression, suggestedWeight) ->
            item(key = "$exerciseKey-intra") {
                val isIncrease = progression == BilboProgression.WorkProgression.INCREASE
                val lastEffective = BilboProgression.effectiveReps(lastWorkSet?.reps ?: 0, lastWorkSet?.rir ?: 0)
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isIncrease) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isIncrease) "Sube a ~${String.format(java.util.Locale.US, "%.1f", suggestedWeight)} kg (hiciste $lastEffective reps efectivas, >=12)"
                        else "Baja a ~${String.format(java.util.Locale.US, "%.1f", suggestedWeight)} kg (hiciste $lastEffective reps efectivas, <8)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    // Input section with animated transition
    item(key = "$exerciseKey-input") {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(Modifier.padding(12.dp)) {
                if (appliesBilbo) {
                    AnimatedVisibility(
                        visible = !bilboDone,
                        enter = fadeIn(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()) +
                            expandVertically(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()),
                        exit = fadeOut(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()) +
                            shrinkVertically(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec())
                    ) {
                        BilboSetInput(
                            exerciseKey = exerciseKey, sets = sets,
                            suggestion = suggestion, onLog = onLogBilboSet,
                            onDeleteLastSet = onDeleteLastSet,
                            onFieldFocused = scrollToInput
                        )
                    }
                    AnimatedVisibility(
                        visible = bilboDone,
                        enter = fadeIn(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()) +
                            expandVertically(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()),
                        exit = fadeOut(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()) +
                            shrinkVertically(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec())
                    ) {
                        WorkSetInput(
                            exerciseKey = exerciseKey, sets = sets,
                            suggestion = suggestion, onLog = onLogWorkSet,
                            onDeleteLastSet = onDeleteLastSet,
                            onFieldFocused = scrollToInput
                        )
                    }
                } else {
                    WorkSetInput(
                        exerciseKey = exerciseKey, sets = sets,
                        suggestion = suggestion, onLog = onLogWorkSet,
                        onDeleteLastSet = onDeleteLastSet,
                        onFieldFocused = scrollToInput
                    )
                }
            }
        }
    }

    // Bottom spacer
    item(key = "$exerciseKey-spacer") { Spacer(Modifier.height(16.dp)) }
}
