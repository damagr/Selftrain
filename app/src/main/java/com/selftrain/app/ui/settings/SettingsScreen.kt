package com.selftrain.app.ui.settings

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selftrain.app.data.model.Exercise
import com.selftrain.app.data.repository.ExerciseRepository
import com.selftrain.app.util.BackupManager
import com.selftrain.app.util.Labels
import com.selftrain.app.BuildConfig
import com.selftrain.app.ui.AppThemePrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupManager: BackupManager,
    private val exerciseRepo: ExerciseRepository,
    private val workoutRepo: com.selftrain.app.data.repository.WorkoutRepository,
    private val routineRepo: com.selftrain.app.data.repository.RoutineRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: Context
) : ViewModel() {

    var isExporting by mutableStateOf(false)
    var isImporting by mutableStateOf(false)

    private val _deletedExercises = MutableStateFlow<List<Exercise>>(emptyList())
    val deletedExercises: StateFlow<List<Exercise>> = _deletedExercises.asStateFlow()

    private val _backupFolderDisplay = MutableStateFlow(backupManager.getBackupFolderDisplay())
    val backupFolderDisplay: StateFlow<String> = _backupFolderDisplay.asStateFlow()

    fun loadDeletedExercises() {
        viewModelScope.launch {
            _deletedExercises.value = exerciseRepo.getDeletedExercises()
        }
    }

    fun restoreExercise(id: Long) {
        viewModelScope.launch {
            exerciseRepo.restoreExercise(id)
            _deletedExercises.value = exerciseRepo.getDeletedExercises()
        }
    }

    fun setBackupFolder(uri: Uri, onDone: () -> Unit) {
        viewModelScope.launch {
            backupManager.setBackupFolder(uri)
            _backupFolderDisplay.value = backupManager.getBackupFolderDisplay()
            onDone()
        }
    }

    fun clearBackupFolder(onDone: () -> Unit) {
        viewModelScope.launch {
            backupManager.clearBackupFolder()
            _backupFolderDisplay.value = backupManager.getBackupFolderDisplay()
            onDone()
        }
    }

    fun exportTo(uri: Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            isExporting = true
            var ok = false
            try {
                backupManager.exportTo(uri)
                ok = true
            } catch (_: Exception) { }
            isExporting = false
            onDone(ok)
        }
    }

    fun importFrom(uri: Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            isImporting = true
            var ok = false
            try {
                backupManager.importFrom(uri)
                ok = true
            } catch (_: Exception) { }
            isImporting = false
            onDone(ok)
        }
    }

    // ponytail: exportar CSV (una fila por serie) para Excel / Google Sheets
    var isExportingCsv by mutableStateOf(false)

    fun exportCsvTo(uri: Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            isExportingCsv = true
            var ok = false
            try {
                val workouts = workoutRepo.getAllWorkouts().filter { it.completed }.associateBy { it.id }
                val routines = routineRepo.getAllList().associateBy { it.id }
                val exercises = exerciseRepo.getByIds(workoutRepo.getAllSets().map { it.exerciseId }.distinct())
                    .associateBy { it.id }
                val dateFmt = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                val sb = StringBuilder()
                sb.appendLine("fecha;rutina;ejercicio;tipo;reps;peso_kg;rir;notas")
                // Orden: por fecha de entreno, luego orden de inserción de la serie
                val setsByWorkout = workoutRepo.getAllSets()
                    .filter { it.workoutId in workouts }
                    .groupBy { it.workoutId }
                workouts.values.sortedBy { it.date }.forEach { w ->
                    val routineName = routines[w.routineId]?.name ?: ""
                    setsByWorkout[w.id]?.forEach { s ->
                        val exName = exercises[s.exerciseId]?.name ?: ""
                        sb.appendLine(
                            listOf(
                                dateFmt.format(java.util.Date(w.date)),
                                routineName.csvEscape(),
                                exName.csvEscape(),
                                s.setType,
                                s.reps.toString(),
                                String.format(java.util.Locale.US, "%.1f", s.weightKg),
                                s.rir.toString(),
                                s.notes.csvEscape()
                            ).joinToString(";")
                        )
                    }
                }
                appContext.contentResolver.openOutputStream(uri, "w")?.use { os ->
                    // BOM para que Excel respete los acentos
                    os.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                    os.write(sb.toString().toByteArray(Charsets.UTF_8))
                }
                ok = true
            } catch (_: Exception) { }
            isExportingCsv = false
            onDone(ok)
        }
    }

    private fun String.csvEscape(): String {
        return if (contains(';') || contains('\n') || contains('"')) {
            "\"" + replace("\"", "\"\"") + "\""
        } else this
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onCheckUpdate: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var showImportConfirm by rememberSaveable { mutableStateOf<Uri?>(null) }
    var showRecoveryDialog by rememberSaveable { mutableStateOf(false) }
    val deletedExercises by viewModel.deletedExercises.collectAsState()
    val backupFolderDisplay by viewModel.backupFolderDisplay.collectAsState()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportTo(it) { ok ->
            context.toast(if (ok) "Datos exportados correctamente" else "Error al exportar los datos")
        } }
    }

    // ponytail: exportar CSV (una fila por serie) para Excel / Sheets
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            viewModel.exportCsvTo(it) { ok ->
                context.toast(if (ok) "CSV exportado correctamente" else "Error al exportar el CSV")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { showImportConfirm = it }
    }

    val backupFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.setBackupFolder(it) { context.toast("Carpeta de backups guardada") } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal),
                title = { Text("Ajustes") },
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
        Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            // Version at the top right
            Spacer(Modifier.height(4.dp))
            Text("SelfTrain v${BuildConfig.VERSION_NAME}",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))

            // Export
            ElevatedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(16.dp)) {
                    Text("Exportar datos", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text("Guarda todos tus ejercicios, rutinas y entrenos en un archivo JSON",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    if (viewModel.isExporting) {
                        ContainedLoadingIndicator(
                            containerShape = MaterialTheme.shapes.medium,
                            polygons = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons
                        )
                    } else {
                        Button(
                            onClick = { exportLauncher.launch("selftrain_backup.json") }
                        ) {
                            Icon(Icons.Filled.FileDownload, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Exportar backup")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // ponytail: exportar CSV (una fila por serie) para Excel / Sheets
                    OutlinedButton(
                        onClick = { csvLauncher.launch("selftrain_sets.csv") },
                        enabled = !viewModel.isExportingCsv
                    ) {
                        if (viewModel.isExportingCsv) {
                            ContainedLoadingIndicator(
                                modifier = Modifier.size(18.dp),
                                containerShape = MaterialTheme.shapes.small,
                                polygons = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons
                            )
                        } else {
                            Icon(Icons.Filled.TableView, null)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(if (viewModel.isExportingCsv) "Exportando..." else "Exportar CSV (series)")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ponytail: modo claro — toggle de tema persistente
            ElevatedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Modo claro", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Tema claro para entrenar con mucha luz. El tema oscuro es el predeterminado.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = AppThemePrefs.lightMode,
                        onCheckedChange = { AppThemePrefs.set(context, it) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Import
            ElevatedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(16.dp)) {
                    Text("Importar datos", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text("Restaura tus datos desde un archivo de backup. Esto reemplazará todos los datos actuales.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    if (viewModel.isImporting) {
                        ContainedLoadingIndicator(
                            containerShape = MaterialTheme.shapes.medium,
                            polygons = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons
                        )
                    } else {
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }
                        ) {
                            Icon(Icons.Filled.FileUpload, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Importar backup")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Backup folder
            ElevatedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(16.dp)) {
                    Text("Carpeta de backups automáticos", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text("Los backups que se crean al actualizar o diariamente se guardarán en esta carpeta. Si no eliges ninguna, se guardan en el almacenamiento interno de la app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("Actual: $backupFolderDisplay",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Row {
                        OutlinedButton(onClick = { backupFolderLauncher.launch(null) }) {
                            Icon(Icons.Filled.Folder, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Elegir carpeta")
                        }
                        if (backupFolderDisplay != "Interno (no configurable)") {
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = {
                                viewModel.clearBackupFolder { context.toast("Carpeta restablecida") }
                            }) { Text("Restablecer") }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Recover deleted exercises
            ElevatedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(16.dp)) {
                    Text("Recuperar ejercicios borrados", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text("Restaura ejercicios que hayas eliminado por error. No se pierden tus datos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.loadDeletedExercises()
                            showRecoveryDialog = true
                        }
                    ) {
                        Icon(Icons.Filled.Restore, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Ver ejercicios borrados")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Check for updates
            ElevatedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(16.dp)) {
                    Text("Actualización", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text("Comprueba si hay una nueva versión disponible para descargar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onCheckUpdate) {
                        Icon(Icons.Default.SystemUpdate, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Buscar actualización")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Créditos: Freepik \u2014 Flaticon",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
        }
    }

    // Import confirmation dialog
    showImportConfirm?.let { uri ->
        AlertDialog(
            onDismissRequest = { showImportConfirm = null },
            shape = MaterialTheme.shapes.large,
            title = { Text("¿Importar datos?") },
            text = { Text("Esto reemplazará todos tus datos actuales con los del archivo de backup. ¿Continuar?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.importFrom(uri) { ok ->
                        context.toast(if (ok) "Datos importados correctamente" else "Error al importar el backup")
                    }
                    showImportConfirm = null
                }) {
                    Text("Importar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = null }) { Text("Cancelar") }
            }
        )
    }

    // Recovery dialog
    if (showRecoveryDialog) {
        AlertDialog(
            onDismissRequest = { showRecoveryDialog = false },
            shape = MaterialTheme.shapes.large,
            title = { Text("Ejercicios borrados") },
            text = {
                if (deletedExercises.isEmpty()) {
                    Text("No hay ejercicios borrados.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        deletedExercises.forEach { ex ->
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(ex.name, style = MaterialTheme.typography.titleSmall)
                                        Text(Labels.equipment(ex.equipment),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    TextButton(onClick = {
                                        viewModel.restoreExercise(ex.id)
                                        context.toast("${ex.name} recuperado")
                                    }) {
                                        Text("Recuperar")
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showRecoveryDialog = false }) { Text("Cerrar") }
            }
        )
    }
}

private fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
