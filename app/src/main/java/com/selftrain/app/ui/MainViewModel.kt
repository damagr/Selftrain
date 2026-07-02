package com.selftrain.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selftrain.app.BuildConfig
import com.selftrain.app.util.BackupManager
import com.selftrain.app.util.GitHubRelease
import com.selftrain.app.util.UpdateChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

sealed class UpdateState {
    data object Checking : UpdateState()
    data object UpToDate : UpdateState()
    data class Available(val release: GitHubRelease) : UpdateState()
    data class Downloading(val progress: Int) : UpdateState()
    data class ReadyToInstall(val file: File) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupManager: BackupManager
) : ViewModel() {

    var updateState by mutableStateOf<UpdateState>(UpdateState.UpToDate)
        private set

    var showUpdateDialog by mutableStateOf(false)
    var downloadProgress by mutableIntStateOf(0)
    var userMessage by mutableStateOf<String?>(null)
        private set

    private val currentVersion: String = BuildConfig.VERSION_NAME
    private val checker = UpdateChecker(context.cacheDir)

    fun checkForUpdate(userInitiated: Boolean = false) {
        updateState = UpdateState.Checking
        viewModelScope.launch {
            try {
                val release = withContext(Dispatchers.IO) {
                    checker.checkForUpdate(currentVersion)
                }
                if (release != null) {
                    updateState = UpdateState.Available(release)
                    showUpdateDialog = true
                } else {
                    updateState = UpdateState.UpToDate
                    if (userInitiated) {
                        userMessage = "Ya tienes la última versión (v$currentVersion)"
                    }
                }
            } catch (e: Exception) {
                updateState = UpdateState.UpToDate // fail silently
                if (userInitiated) {
                    userMessage = "No se pudo comprobar la actualización"
                }
            }
        }
    }

    fun startUpdate(release: GitHubRelease) {
        val apkUrl = release.assets.firstOrNull()?.browserDownloadUrl ?: release.htmlUrl
        showUpdateDialog = false

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    backupManager.createAutomaticBackup(currentVersion)
                }
                userMessage = "Backup de seguridad creado"
                updateState = UpdateState.Downloading(0)

                val file = withContext(Dispatchers.IO) {
                    checker.downloadApk(apkUrl) { progress ->
                        downloadProgress = progress
                        updateState = UpdateState.Downloading(progress)
                    }
                }
                updateState = UpdateState.ReadyToInstall(file)
            } catch (e: Exception) {
                updateState = UpdateState.Error(e.message ?: "Error al descargar")
            }
        }
    }

    fun installApk(file: File) {
        // ponytail: check install permission on Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return
        }
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }

    fun dismissError() {
        updateState = UpdateState.UpToDate
    }

    fun dismissDialog() {
        showUpdateDialog = false
        updateState = UpdateState.UpToDate
    }

    fun clearMessage() {
        userMessage = null
    }
}
