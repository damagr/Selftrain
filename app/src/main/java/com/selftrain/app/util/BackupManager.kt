package com.selftrain.app.util

import android.content.Context
import android.net.Uri
import com.selftrain.app.data.db.AppDatabase
import com.selftrain.app.data.model.*
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
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

    suspend fun exportTo(uri: Uri) {
        val data = BackupData(
            exercises = db.exerciseDao().getAllList(),
            routines = db.routineDao().getAllList(),
            routineExercises = db.routineDao().getAllRoutineExercises(),
            workouts = db.workoutDao().getAllList(),
            workoutSets = db.workoutDao().getAllSets()
        )
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
}
