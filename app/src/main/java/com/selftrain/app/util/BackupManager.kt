package com.selftrain.app.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.selftrain.app.data.db.AppDatabase
import com.selftrain.app.data.model.*
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class BackupData(
    val exercises: List<Exercise> = emptyList(),
    val routines: List<Routine> = emptyList(),
    val routineExercises: List<RoutineExercise> = emptyList(),
    val workouts: List<Workout> = emptyList(),
    val workoutSets: List<WorkoutSet> = emptyList()
)

@Singleton
class BackupManager @Inject constructor(
    private val db: AppDatabase,
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()
    private val prefs = context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)

    fun setBackupFolder(uri: Uri) {
        val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: SecurityException) { }
        prefs.edit().putString(KEY_BACKUP_URI, uri.toString()).apply()
    }

    fun clearBackupFolder() {
        getBackupFolderUri()?.let { uri ->
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.releasePersistableUriPermission(uri, flags)
            } catch (_: SecurityException) { }
        }
        prefs.edit().remove(KEY_BACKUP_URI).apply()
    }

    fun getBackupFolderUri(): Uri? =
        prefs.getString(KEY_BACKUP_URI, null)?.let { Uri.parse(it) }

    fun getBackupFolderDisplay(): String {
        val uri = getBackupFolderUri() ?: return "Interno (no configurable)"
        return runCatching {
            DocumentFile.fromTreeUri(context, uri)?.name
        }.getOrNull() ?: uri.toString()
    }

    private suspend fun buildBackupData(): BackupData = BackupData(
        exercises = db.exerciseDao().getAllList(),
        routines = db.routineDao().getAllList(),
        routineExercises = db.routineDao().getAllRoutineExercises(),
        workouts = db.workoutDao().getAllList(),
        workoutSets = db.workoutDao().getAllSets()
    )

    private fun backupFileName(versionName: String): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
        return "selftrain_${versionName}_${timestamp}.json"
    }

    suspend fun exportTo(uri: Uri) {
        val data = buildBackupData()
        val json = gson.toJson(data)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
        }
    }

    suspend fun importFrom(uri: Uri) {
        val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
            ?: throw IllegalStateException("No se pudo leer el archivo")
        val data = gson.fromJson(json, BackupData::class.java)

        // Clear existing and re-insert
        db.workoutDao().getAllSets().forEach { db.workoutDao().deleteSet(it) }
        db.workoutDao().getAllList().forEach { db.workoutDao().delete(it) }
        db.routineDao().getAllRoutineExercises().forEach { db.routineDao().removeExercise(it.id) }
        db.routineDao().getAllList().forEach { db.routineDao().delete(it) }
        db.exerciseDao().getAllListRaw().forEach { db.exerciseDao().softDelete(it.id) }

        // Re-insert from backup
        db.exerciseDao().insertAll(data.exercises)
        data.routines.forEach { db.routineDao().insert(it) }
        data.routineExercises.forEach { db.routineDao().addExercise(it) }
        data.workouts.forEach { db.workoutDao().insert(it) }
        data.workoutSets.forEach { db.workoutDao().insertSet(it) }
    }

    suspend fun createAutomaticBackup(versionName: String) {
        val data = buildBackupData()
        val json = gson.toJson(data)
        val name = backupFileName(versionName)

        val treeUri = getBackupFolderUri()
        if (treeUri != null) {
            // ponytail: SAF tree chosen by user; create/overwrite file in that folder
            val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return
            val existing = tree.findFile(name)
            existing?.delete()
            tree.createFile("application/json", name)?.let { file ->
                context.contentResolver.openOutputStream(file.uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
            }
        } else {
            val dir = File(context.filesDir, "backups")
            if (!dir.exists()) dir.mkdirs()
            File(dir, name).writeText(json, Charsets.UTF_8)
        }
    }

    fun cleanOldBackups(maxKeep: Int = 5) {
        val treeUri = getBackupFolderUri()
        if (treeUri != null) {
            val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return
            val backups = tree.listFiles()
                .filter { it.name?.startsWith("selftrain_") == true && it.name?.endsWith(".json") == true }
            if (backups.size <= maxKeep) return
            backups.sortedByDescending { it.lastModified() }
                .drop(maxKeep)
                .forEach { it.delete() }
        } else {
            val dir = File(context.filesDir, "backups")
            if (!dir.exists()) return

            val backups = dir.listFiles { f -> f.name.startsWith("selftrain_") && f.name.endsWith(".json") }
                ?: return

            if (backups.size <= maxKeep) return

            backups.sortedByDescending { it.lastModified() }
                .drop(maxKeep)
                .forEach { it.delete() }
        }
    }

    companion object {
        private const val KEY_BACKUP_URI = "backup_folder_uri"
    }
}
