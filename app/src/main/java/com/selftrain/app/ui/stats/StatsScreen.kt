package com.selftrain.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.selftrain.app.data.db.ExercisePrRow
import com.selftrain.app.util.Labels
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val completedWorkouts by viewModel.completedWorkouts.collectAsState()
    val weeklyVolume by viewModel.weeklyVolume.collectAsState()
    val weeklyStreak by viewModel.weeklyStreak.collectAsState()
    val totalVolume by viewModel.totalVolume.collectAsState()
    val personalRecords by viewModel.personalRecords.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val latestBodyWeight by viewModel.latestBodyWeight.collectAsState()

    val exerciseById = remember(exercises) { exercises.associateBy { it.id } }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal),
                title = { Text("Estadísticas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // --- Summary cards ---
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        value = "${completedWorkouts.size}",
                        label = "Entrenos"
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        value = "$weeklyStreak",
                        label = "Semanas de racha"
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        value = "%,.0f kg".format(Locale.getDefault(), totalVolume),
                        label = "Volumen total"
                    )
                }
            }

            // --- Weekly volume chart ---
            item {
                ElevatedCard(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Volumen semanal", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "suma de reps × kg por semana",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        if (weeklyVolume.isEmpty()) {
                            Text(
                                "Aún no hay datos suficientes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            WeeklyVolumeChart(weeklyVolume)
                        }
                    }
                }
            }

            // --- Personal records ---
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.EmojiEvents, null,
                        Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Récords personales (1RM estimado)", style = MaterialTheme.typography.titleSmall)
                }
            }
            if (personalRecords.isEmpty()) {
                item {
                    Text(
                        "Aún no hay récords registrados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(personalRecords, key = { it.exerciseId }) { pr ->
                    val ex = exerciseById[pr.exerciseId]
                    ElevatedCard(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    ex?.name ?: "Ejercicio",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (ex != null) {
                                    Text(
                                        Labels.muscleGroup(ex.muscleGroup),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "%,.1f kg".format(Locale.getDefault(), pr.max1RM),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (latestBodyWeight > 0f) {
                                    // ponytail: fuerza relativa (1RM / peso corporal)
                                    Text(
                                        "%.2f × PC".format(Locale.getDefault(), pr.max1RM / latestBodyWeight),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "con ${String.format("%.1f", pr.weightKg)} kg × ${pr.reps} reps",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier = Modifier, value: String, label: String) {
    ElevatedCard(modifier = modifier, shape = MaterialTheme.shapes.medium) {
        Column(
            Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Gráfico de barras simple: volumen por semana (últimas 12) */
@Composable
private fun WeeklyVolumeChart(weeks: List<WeekVolume>) {
    val barColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val weekFormatter = remember { DateTimeFormatter.ofPattern("d/M") }

    val maxVolume = weeks.maxOf { it.volumeKg }.coerceAtLeast(1f)
    val chartHeight = 140.dp
    val labelHeight = 16.dp

    Column(Modifier.fillMaxWidth()) {
        Canvas(
            Modifier.fillMaxWidth().height(chartHeight)
        ) {
            val n = weeks.size
            val gap = 6.dp.toPx()
            val barWidth = ((size.width - gap * (n - 1)) / n).coerceAtLeast(2f)
            weeks.forEachIndexed { i, week ->
                val barHeightPx = (week.volumeKg / maxVolume) * size.height
                if (barHeightPx > 1f) {
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(i * (barWidth + gap), size.height - barHeightPx),
                        size = Size(barWidth, barHeightPx),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
            }
        }
        // Week labels under the bars
        Row(Modifier.fillMaxWidth()) {
            weeks.forEach { week ->
                Text(
                    weekFormatter.format(week.weekStart),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
        Spacer(Modifier.height(labelHeight / 4))
    }
}
