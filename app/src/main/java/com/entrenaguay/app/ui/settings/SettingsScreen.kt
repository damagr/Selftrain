package com.entrenaguay.app.ui.settings

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entrenaguay.app.util.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupManager: BackupManager
) : ViewModel() {

    var isExporting by mutableStateOf(false)
    var isImporting by mutableStateOf(false)

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
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var showImportConfirm by remember { mutableStateOf<Uri?>(null) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
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
                        onClick = { exportLauncher.launch("entrenaguay_backup.json") },
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

            Spacer(Modifier.weight(1f))

            // About
            Text("EntrenaGuay v1.0",
                style = MaterialTheme.typography.bodySmall,
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
}

private fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
