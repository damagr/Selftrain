package com.selftrain.app.data.repository

import android.content.Context
import com.selftrain.app.data.db.ExerciseDao
import com.selftrain.app.data.model.Exercise
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.selftrain.app.util.findMatchingGifUrl
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
        val json = context.assets.open("exercises.json").bufferedReader().readText()
        val seedData: List<SeedExercise> = Gson().fromJson(json, object : TypeToken<List<SeedExercise>>() {}.type)

        if (dao.count() == 0) {
            dao.insertAll(seedData.map { it.toExercise() })
            return
        }

        val existing = dao.getAllList().map { it.name }.toSet()
        val missing = seedData.filter { it.name !in existing }
        if (missing.isNotEmpty()) {
            dao.insertAll(missing.map { it.toExercise() })
        }

        // ponytail: sync isBilboEligible de los que ya existen (migración de datos sin
        // tocar schema — usuarios instalados tienen flags de la regla antigua "compound")
        val byName = dao.getAllList().associateBy { it.name }
        seedData.forEach { seed ->
            byName[seed.name]?.takeIf { it.isBilboEligible != seed.isBilboEligible }
                ?.let { dao.updateBilboEligible(it.id, seed.isBilboEligible) }
        }
    }

    private fun SeedExercise.toExercise() = Exercise(
        name = name, muscleGroup = muscleGroup,
        category = category, isBilboEligible = isBilboEligible,
        equipment = equipment,
        gifUrl = findMatchingGifUrl(name)
    )

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

    suspend fun updateExercise(exercise: Exercise, newName: String, gifUrl: String?) {
        if (dao.countUsageInRoutines(exercise.id) > 0 || dao.countUsageInSets(exercise.id) > 0) {
            throw IllegalStateException("El ejercicio está en uso")
        }
        dao.updateNameAndGif(id = exercise.id, name = newName.trim(), gifUrl = gifUrl)
    }

    suspend fun getUsedExerciseIds(): Set<Long> = dao.getUsedExerciseIds().toSet()

    suspend fun getDeletedExercises(): List<Exercise> = dao.getDeleted()
    suspend fun restoreExercise(id: Long) = dao.restore(id)
    suspend fun getAllList() = dao.getAllList()
}
