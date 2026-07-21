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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupManager: BackupManager,
    private val exerciseRepo: ExerciseRepository
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

    fun exportTo(uri: Uri, onDone: () -> Unit) {
        viewModelScope.launch {
            isExporting = true
            try {
                backupManager.exportTo(uri)
            } catch (_: Exception) { }
            isExporting = false
            onDone()
        }
    }

    fun importFrom(uri: Uri, onDone: () -> Unit) {
        viewModelScope.launch {
            isImporting = true
            try {
                backupManager.importFrom(uri)
            } catch (_: Exception) { }
            isImporting = false
            onDone()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onCheckUpdate: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var showImportConfirm by remember { mutableStateOf<Uri?>(null) }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    val deletedExercises by viewModel.deletedExercises.collectAsState()
    val backupFolderDisplay by viewModel.backupFolderDisplay.collectAsState()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportTo(it) { context.toast("Datos exportados correctamente") } }
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
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            // Export
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Exportar datos", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text("Guarda todos tus ejercicios, rutinas y entrenos en un archivo JSON",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { exportLauncher.launch("selftrain_backup.json") },
                        enabled = !viewModel.isExporting
                    ) {
                        Icon(Icons.Filled.FileDownload, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Exportar backup")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Import
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Importar datos", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text("Restaura tus datos desde un archivo de backup. Esto reemplazará todos los datos actuales.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                        enabled = !viewModel.isImporting
                    ) {
                        Icon(Icons.Filled.FileUpload, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Importar backup")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Backup folder
            Card(Modifier.fillMaxWidth()) {
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
            Card(Modifier.fillMaxWidth()) {
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
            Card(Modifier.fillMaxWidth()) {
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

            Spacer(Modifier.weight(1f))

            // About
            Text("SelfTrain v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text("Icono: Freepik — Flaticon",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    // Import confirmation dialog
    showImportConfirm?.let { uri ->
        AlertDialog(
            onDismissRequest = { showImportConfirm = null },
            title = { Text("¿Importar datos?") },
            text = { Text("Esto reemplazará todos tus datos actuales con los del archivo de backup. ¿Continuar?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.importFrom(uri) {
                        context.toast("Datos importados correctamente")
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
            title = { Text("Ejercicios borrados") },
            text = {
                if (deletedExercises.isEmpty()) {
                    Text("No hay ejercicios borrados.")
                } else {
                    Column {
                        deletedExercises.forEach { ex ->
                            ListItem(
                                headlineContent = { Text(ex.name) },
                                supportingContent = { Text(Labels.equipment(ex.equipment)) },
                                trailingContent = {
                                    TextButton(onClick = {
                                        viewModel.restoreExercise(ex.id)
                                        context.toast("${ex.name} recuperado")
                                    }) {
                                        Text("Recuperar")
                                    }
                                }
                            )
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
