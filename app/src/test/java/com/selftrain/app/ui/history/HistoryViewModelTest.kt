package com.selftrain.app.ui.history

import com.selftrain.app.data.db.SetWithExercise
import com.selftrain.app.data.model.Exercise
import com.selftrain.app.data.model.Routine
import com.selftrain.app.data.model.Workout
import com.selftrain.app.data.model.WorkoutSet
import com.selftrain.app.data.repository.ExerciseRepository
import com.selftrain.app.data.repository.RoutineRepository
import com.selftrain.app.data.repository.WorkoutRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// ponytail: filtros del historial y edición (F5) — justo donde salieron bugs del audit
class HistoryViewModelTest {

    private val workoutRepo: WorkoutRepository = mockk(relaxed = true)
    private val exerciseRepo: ExerciseRepository = mockk(relaxed = true)
    private val routineRepo: RoutineRepository = mockk(relaxed = true)
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { exerciseRepo.exercises } returns MutableStateFlow(emptyList())
        every { routineRepo.routines } returns MutableStateFlow(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun workout(id: Long, routineId: Long, date: Long, completed: Boolean = true) =
        Workout(id = id, routineId = routineId, date = date, completed = completed)

    private fun seedWorkouts(): List<Workout> {
        val now = System.currentTimeMillis()
        return listOf(
            workout(1, routineId = 1, date = now),
            workout(2, routineId = 2, date = now - 1000),
            workout(3, routineId = 1, date = now - 2000, completed = false)
        )
    }

    @Test
    fun `sin filtro devuelve solo completados ordenados desc`() = runTest {
        every { workoutRepo.workouts } returns MutableStateFlow(seedWorkouts())
        val vm = HistoryViewModel(workoutRepo, exerciseRepo, routineRepo)

        val list = vm.completedWorkouts.first { it.isNotEmpty() }

        assertEquals(listOf(1L, 2L), list.map { it.id })
    }

    @Test
    fun `filtro por rutina`() = runTest {
        every { workoutRepo.workouts } returns MutableStateFlow(seedWorkouts())
        val vm = HistoryViewModel(workoutRepo, exerciseRepo, routineRepo)
        vm.setFilter(HistoryFilter(routineId = 2))

        val list = vm.completedWorkouts.first { it.isNotEmpty() }

        assertEquals(listOf(2L), list.map { it.id })
    }

    @Test
    fun `filtro por ejercicio resuelve los entrenos que lo contienen`() = runTest {
        every { workoutRepo.workouts } returns MutableStateFlow(seedWorkouts())
        // el ejercicio 5 aparece en el entreno 1 y en el incompleto 3
        coEvery { workoutRepo.getSetHistory(5) } returns listOf(
            SetWithExercise(WorkoutSet(id = 1, workoutId = 1, exerciseId = 5, setType = "work", reps = 10, weightKg = 50f), "Press", "chest"),
            SetWithExercise(WorkoutSet(id = 2, workoutId = 3, exerciseId = 5, setType = "work", reps = 10, weightKg = 50f), "Press", "chest")
        )
        val vm = HistoryViewModel(workoutRepo, exerciseRepo, routineRepo)
        vm.setFilter(HistoryFilter(exerciseId = 5))

        val list = vm.completedWorkouts.first { it.isNotEmpty() }

        assertEquals(listOf(1L), list.map { it.id })
    }

    @Test
    fun `filtro combinado rutina mas ejercicio`() = runTest {
        every { workoutRepo.workouts } returns MutableStateFlow(seedWorkouts())
        coEvery { workoutRepo.getSetHistory(5) } returns listOf(
            SetWithExercise(WorkoutSet(id = 1, workoutId = 1, exerciseId = 5, setType = "work", reps = 10, weightKg = 50f), "Press", "chest"),
            SetWithExercise(WorkoutSet(id = 2, workoutId = 2, exerciseId = 5, setType = "work", reps = 10, weightKg = 50f), "Press", "chest")
        )
        val vm = HistoryViewModel(workoutRepo, exerciseRepo, routineRepo)
        vm.setFilter(HistoryFilter(routineId = 2, exerciseId = 5))

        val list = vm.completedWorkouts.first { it.isNotEmpty() }

        assertEquals(listOf(2L), list.map { it.id })
    }

    @Test
    fun `limpiar filtro restaura todo`() = runTest {
        every { workoutRepo.workouts } returns MutableStateFlow(seedWorkouts())
        val vm = HistoryViewModel(workoutRepo, exerciseRepo, routineRepo)
        vm.setFilter(HistoryFilter(routineId = 2))
        vm.completedWorkouts.first { it.size == 1 }
        vm.setFilter(HistoryFilter())

        val list = vm.completedWorkouts.first { it.size == 2 }

        assertEquals(2, list.size)
    }

    @Test
    fun `updateSet aplica el tipo cambiado en el dialogo`() = runTest {
        val now = System.currentTimeMillis()
        every { workoutRepo.workouts } returns MutableStateFlow(seedWorkouts())
        val set = WorkoutSet(id = 9, workoutId = 1, exerciseId = 5, setType = "work", reps = 10, weightKg = 50f, rir = 2)
        val updated = set.copy(setType = "bilbo", reps = 12, weightKg = 55f, rir = 1, notes = "dura")
        coEvery { workoutRepo.getSetsWithExercise(1) } returns listOf(
            SetWithExercise(updated, "Press", "chest")
        )
        val vm = HistoryViewModel(workoutRepo, exerciseRepo, routineRepo)
        vm.selectWorkout(workout(1, routineId = 1, date = now))
        advanceUntilIdle()

        vm.updateSet(set, setType = "bilbo", reps = 12, weightKg = 55f, rir = 1, notes = "dura")
        advanceUntilIdle()

        coVerify {
            workoutRepo.updateSet(match {
                it.setType == "bilbo" && it.reps == 12 && it.weightKg == 55f && it.notes == "dura"
            })
        }
        val detail = vm.workoutDetailSets.first { it.isNotEmpty() }
        assertEquals("bilbo", detail[0].set.setType)
    }

    @Test
    fun `updateWorkoutDate conserva la hora del dia original`() = runTest {
        val now = System.currentTimeMillis()
        every { workoutRepo.workouts } returns MutableStateFlow(seedWorkouts())
        val original = workout(1, routineId = 1, date = now)
        coEvery { workoutRepo.getSetsWithExercise(1) } returns emptyList()
        val vm = HistoryViewModel(workoutRepo, exerciseRepo, routineRepo)
        vm.selectWorkout(original)
        advanceUntilIdle()

        val zone = java.time.ZoneId.systemDefault()
        val originalTime = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalTime()
        val newDate = java.time.LocalDate.now().minusDays(3)
            .atTime(originalTime).atZone(zone).toInstant().toEpochMilli()
        vm.updateWorkoutDate(original, newDate)
        advanceUntilIdle()

        val updated = vm.selectedWorkout.first { it != null }!!
        assertEquals(newDate, updated.date)
    }
}
