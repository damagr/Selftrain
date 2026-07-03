package com.selftrain.app.data.repository

import android.content.Context
import com.selftrain.app.data.db.ExerciseDao
import com.selftrain.app.data.model.Exercise
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class SeedExercise(
    val name: String,
    val muscleGroup: String,
    val category: String,
    val isBilboEligible: Boolean,
    val equipment: String = ""
)

@Singleton
class ExerciseRepository @Inject constructor(
    private val dao: ExerciseDao,
    @ApplicationContext private val context: Context
) {
    val exercises = dao.getAll()

    suspend fun seedIfEmpty() {
        if (dao.count() == 0) {
            val json = context.assets.open("exercises.json").bufferedReader().readText()
            val seedData: List<SeedExercise> = Gson().fromJson(json, object : TypeToken<List<SeedExercise>>() {}.type)
            val exercises = seedData.map { seed ->
                Exercise(name = seed.name, muscleGroup = seed.muscleGroup, category = seed.category, isBilboEligible = seed.isBilboEligible, equipment = seed.equipment)
            }
            dao.insertAll(exercises)
        }
    }

    suspend fun getById(id: Long) = dao.getById(id)
    suspend fun getByIds(ids: List<Long>) = dao.getByIds(ids)
    suspend fun addExercise(exercise: Exercise) = dao.insert(exercise)
    suspend fun deleteExercise(exercise: Exercise) {
        if (dao.countUsageInRoutines(exercise.id) > 0 || dao.countUsageInSets(exercise.id) > 0) {
            throw IllegalStateException("El ejercicio está en uso")
        }
        dao.softDelete(exercise.id)
    }
    suspend fun getUsageCount(exerciseId: Long): Pair<Int, Int> =
        dao.countUsageInRoutines(exerciseId) to dao.countUsageInSets(exerciseId)

    suspend fun getDeletedExercises(): List<Exercise> = dao.getDeleted()
    suspend fun restoreExercise(id: Long) = dao.restore(id)
    suspend fun getAllList() = dao.getAllList()
}
