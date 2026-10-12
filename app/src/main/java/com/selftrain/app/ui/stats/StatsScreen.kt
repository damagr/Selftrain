package com.selftrain.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.selftrain.app.data.db.ExercisePrRow
import com.selftrain.app.util.Labels
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// ponytail: semanas por página del gráfico de reps (navegación con flechas)
private const val WEEKS_PER_PAGE = 8

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val completedWorkouts by viewModel.completedWorkouts.collectAsState()
    val weeklyReps by viewModel.weeklyReps.collectAsState()
    val weeklyStreak by viewModel.weeklyStreak.collectAsState()
    val totalVolume by viewModel.totalVolume.collectAsState()
    val personalRecords by viewModel.personalRecords.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val latestBodyWeight by viewModel.latestBodyWeight.collectAsState()

    // ponytail: paginación del gráfico de reps (F: navegación por flechas, 8 semanas/página)
    var repsPage by rememberSaveable { mutableIntStateOf(0) }

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

            // --- Weekly work reps chart: conseguidas vs objetivo ---
            item {
                ElevatedCard(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Reps de trabajo", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "conseguidas vs objetivo (8-12 por serie, sin contar Bilbo)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        if (weeklyReps.isEmpty()) {
                            Text(
                                "Aún no hay datos suficientes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            val pageCount = (weeklyReps.size + WEEKS_PER_PAGE - 1) / WEEKS_PER_PAGE
                            val safePage = repsPage.coerceIn(0, pageCount - 1)
                            val visible = pageOfWeeks(weeklyReps, safePage, WEEKS_PER_PAGE)
                            if (pageCount > 1) {
                                // navegación estilo calendario del Historial: < = semanas anteriores
                                val rangeFormatter = remember { DateTimeFormatter.ofPattern("d/M") }
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { repsPage = safePage + 1 },
                                        enabled = safePage < pageCount - 1
                                    ) {
                                        Icon(Icons.Default.ChevronLeft, "Semanas anteriores")
                                    }
                                    Text(
                                        "${rangeFormatter.format(visible.first().weekStart)} – ${rangeFormatter.format(visible.last().weekStart)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    IconButton(
                                        onClick = { repsPage = safePage - 1 },
                                        enabled = safePage > 0
                                    ) {
                                        Icon(Icons.Default.ChevronRight, "Semanas más recientes")
                                    }
                                }
                            }
                            WeeklyRepsChart(visible)
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

/** Gráfico de reps semanales (bullet): zonas objetivo ancladas a 0 + barra de conseguidas */
@Composable
private fun WeeklyRepsChart(weeks: List<WeekReps>) {
    val achievedColor = MaterialTheme.colorScheme.primary
    val zoneColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val weekFormatter = remember { DateTimeFormatter.ofPattern("d/M") }

    // semana seleccionada para el caption — por defecto la más reciente (siempre hay cifra visible)
    var selected by remember(weeks) { mutableIntStateOf(weeks.lastIndex) }
    val sel = weeks.getOrNull(selected) ?: weeks.last()

    val maxReps = weeks.maxOf { it.targetMaxReps }.coerceAtLeast(1)
    val chartHeight = 140.dp

    Column(Modifier.fillMaxWidth()) {
        // Caption de la semana seleccionada (tap en una columna para cambiarla)
        Text(
            buildAnnotatedString {
                append("${weekFormatter.format(sel.weekStart)}: ")
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                    append("${sel.achievedReps} reps")
                }
                append(" · objetivo ${sel.targetMinReps}-${sel.targetMaxReps}")
                append(" · " + when {
                    sel.achievedReps > sel.targetMaxReps -> "sobre el objetivo"
                    sel.achievedReps < sel.targetMinReps -> "por debajo"
                    else -> "en objetivo"
                })
                if (sel.workoutCount > 0) append(" · ${sel.workoutCount} entreno${if (sel.workoutCount == 1) "" else "s"}")
            },
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1
        )
        Spacer(Modifier.height(8.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .pointerInput(weeks) {
                    detectTapGestures { offset ->
                        val n = weeks.size
                        val slot = size.width / n
                        selected = (offset.x / slot).toInt().coerceIn(0, n - 1)
                    }
                }
        ) {
            val n = weeks.size
            val slotWidth = size.width / n
            val gap = 4.dp.toPx()
            // zonas a lo ancho del slot (menos el gap); barra de conseguidas estrecha y centrada
            val zoneWidth = slotWidth - gap
            val barWidth = (zoneWidth * 0.5f).coerceAtLeast(2f)
            weeks.forEachIndexed { i, week ->
                val isSelected = i == selected
                val slotStart = i * slotWidth
                val zoneX = slotStart + gap / 2f
                val targetMaxH = (week.targetMaxReps.toFloat() / maxReps) * size.height
                val targetMinH = (week.targetMinReps.toFloat() / maxReps) * size.height
                val achievedH = (week.achievedReps.toFloat() / maxReps) * size.height
                val shortZoneAlpha = if (isSelected) 0.35f else 0.18f
                val targetZoneAlpha = if (isSelected) 0.55f else 0.35f
                // zona "corto": de 0 al mínimo objetivo (series × 8)
                drawRoundRect(
                    color = zoneColor.copy(alpha = shortZoneAlpha),
                    topLeft = Offset(zoneX, size.height - targetMinH),
                    size = Size(zoneWidth, targetMinH),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                // zona "objetivo": del mínimo al máximo (series × 8 a series × 12)
                drawRoundRect(
                    color = zoneColor.copy(alpha = targetZoneAlpha),
                    topLeft = Offset(zoneX, size.height - targetMaxH),
                    size = Size(zoneWidth, targetMaxH - targetMinH),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                // barra conseguida: estrecha, centrada sobre las zonas — su punta cuenta la historia
                if (achievedH > 1f) {
                    drawRoundRect(
                        color = achievedColor.copy(alpha = if (isSelected) 1f else 0.75f),
                        topLeft = Offset(zoneX + (zoneWidth - barWidth) / 2f, size.height - achievedH),
                        size = Size(barWidth, achievedH),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }
            }
        }
        // Week labels under the bars
        Row(Modifier.fillMaxWidth()) {
            weeks.forEachIndexed { i, week ->
                Text(
                    weekFormatter.format(week.weekStart),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (i == selected) MaterialTheme.colorScheme.primary else labelColor,
                    fontWeight = if (i == selected) FontWeight.Bold else null,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}
