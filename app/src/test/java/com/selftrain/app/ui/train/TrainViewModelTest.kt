package com.selftrain.app.ui.train

import com.selftrain.app.data.model.Exercise
import com.selftrain.app.data.model.Routine
import com.selftrain.app.data.model.RoutineExercise
import com.selftrain.app.data.model.Workout
import com.selftrain.app.data.repository.ExerciseRepository
import com.selftrain.app.data.repository.RoutineRepository
import com.selftrain.app.data.repository.WorkoutRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// ponytail: arranque/reanudación de entrenos, registro de series y descarte (F5) —
// los flujos que produjeron los bugs del audit (spinner infinito, huérfanos, race)
class TrainViewModelTest {

    private val routineRepo: RoutineRepository = mockk(relaxed = true)
    private val workoutRepo: WorkoutRepository = mockk(relaxed = true)
    private val exerciseRepo: ExerciseRepository = mockk(relaxed = true)
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun exercise(id: Long, bilboEligible: Boolean = false) = Exercise(
        id = id, name = "Ejercicio $id", muscleGroup = "chest",
        category = "compound", isBilboEligible = bilboEligible
    )

    private fun routine(id: Long, method: String = "fullbody") = Routine(
        id = id, name = "Test", method = method
    )

    private fun re(exerciseId: Long, order: Int = 0, isBilbo: Boolean = false) = RoutineExercise(
        routineId = 1, exerciseId = exerciseId, order = order, isBilbo = isBilbo
    )

    // Configura mocks para una rutina con un ejercicio (id 10) sin historial
    private fun stubSimpleRoutine(routineId: Long = 1) {
        coEvery { routineRepo.getById(routineId) } returns routine(routineId)
        coEvery { routineRepo.getWithExercises(routineId) } returns listOf(re(10))
        coEvery { exerciseRepo.getById(10) } returns exercise(10)
        coEvery { workoutRepo.getSetHistory(10) } returns emptyList()
        coEvery { workoutRepo.getLastExerciseSession(10) } returns emptyList()
    }

    private fun workout(id: Long) = Workout(id = id, routineId = 1, date = System.currentTimeMillis())

    @Test
    fun `startWorkout sin huerfano crea workout nuevo`() = runTest {
        stubSimpleRoutine()
        coEvery { workoutRepo.getUnfinishedForRoutine(1) } returns null
        coEvery { workoutRepo.insert(any()) } returns 42L
        val vm = TrainViewModel(routineRepo, workoutRepo, exerciseRepo)

        vm.startWorkout(1)
        advanceUntilIdle()

        assertEquals(42L, vm.state.value.workoutId)
        assertEquals(1, vm.state.value.exercises.size)
        coVerify(exactly = 1) { workoutRepo.insert(any()) }
    }

    @Test
    fun `startWorkout reanuda automaticamente el huerfano de la rutina`() = runTest {
        stubSimpleRoutine()
        val orphan = workout(7)
        coEvery { workoutRepo.getUnfinishedForRoutine(1) } returns orphan
        coEvery { workoutRepo.getById(7) } returns orphan
        coEvery { workoutRepo.getSetsWithExercise(7) } returns emptyList()
        val vm = TrainViewModel(routineRepo, workoutRepo, exerciseRepo)

        vm.startWorkout(1)
        advanceUntilIdle()

        assertEquals(7L, vm.state.value.workoutId)
        coVerify(exactly = 0) { workoutRepo.insert(any()) }
    }

    @Test
    fun `resume aplica sets existentes y lastExerciseIndex`() = runTest {
        stubSimpleRoutine()
        val existing = workout(7).copy(lastExerciseIndex = 0)
        coEvery { workoutRepo.getById(7) } returns existing
        coEvery { workoutRepo.getSetsWithExercise(7) } returns listOf(
            com.selftrain.app.data.db.SetWithExercise(
                com.selftrain.app.data.model.WorkoutSet(
                    id = 1, workoutId = 7, exerciseId = 10,
                    setType = "work", reps = 10, weightKg = 50f
                ),
                exerciseName = "Ejercicio 10", muscleGroup = "chest"
            )
        )
        val vm = TrainViewModel(routineRepo, workoutRepo, exerciseRepo)

        vm.startWorkout(1, existingWorkoutId = 7)
        advanceUntilIdle()

        assertEquals(7L, vm.state.value.workoutId)
        assertEquals(1, vm.state.value.exercises[0].sets.size)
        assertEquals(10, vm.state.value.exercises[0].sets[0].reps)
    }

    @Test
    fun `resume con id invalido cae al flujo de crear`() = runTest {
        stubSimpleRoutine()
        coEvery { workoutRepo.getById(99) } returns null
        coEvery { workoutRepo.insert(any()) } returns 42L
        val vm = TrainViewModel(routineRepo, workoutRepo, exerciseRepo)

        vm.startWorkout(1, existingWorkoutId = 99)
        advanceUntilIdle()

        assertEquals(42L, vm.state.value.workoutId)
        coVerify(exactly = 1) { workoutRepo.insert(any()) }
    }

    @Test
    fun `startWorkout salta ejercicios borrados sin colgarse`() = runTest {
        coEvery { routineRepo.getById(1) } returns routine(1)
        coEvery { routineRepo.getWithExercises(1) } returns listOf(re(10), re(20))
        coEvery { exerciseRepo.getById(10) } returns exercise(10)
        coEvery { exerciseRepo.getById(20) } returns null  // soft-borrado
        coEvery { workoutRepo.getSetHistory(10) } returns emptyList()
        coEvery { workoutRepo.getLastExerciseSession(10) } returns emptyList()
        // sin este stub, el mock relajado devuelve un Workout dummy no-null y entraría por resume
        coEvery { workoutRepo.getUnfinishedForRoutine(1) } returns null
        coEvery { workoutRepo.insert(any()) } returns 42L
        val vm = TrainViewModel(routineRepo, workoutRepo, exerciseRepo)

        vm.startWorkout(1)
        advanceUntilIdle()

        assertEquals(1, vm.state.value.exercises.size)
        assertEquals(42L, vm.state.value.workoutId)
    }

    @Test
    fun `logSet persiste la serie dispara trigger y guarda el indice`() = runTest {
        stubSimpleRoutine()
        coEvery { workoutRepo.getUnfinishedForRoutine(1) } returns null
        coEvery { workoutRepo.insert(any()) } returns 42L
        val vm = TrainViewModel(routineRepo, workoutRepo, exerciseRepo)
        vm.startWorkout(1)
        advanceUntilIdle()
        coEvery { workoutRepo.getById(42) } returns workout(42)

        vm.logSet(10, setType = "work", reps = 10, weightKg = 50f, rir = 2, explosive = false)
        advanceUntilIdle()

        coVerify {
            workoutRepo.insertSet(match {
                it.exerciseId == 10L && it.setType == "work" && it.reps == 10 && it.weightKg == 50f
            })
        }
        assertEquals(1, vm.restTrigger.value)
        coVerify { workoutRepo.update(match { it.lastExerciseIndex == 0 }) }
    }

    @Test
    fun `finishWorkout marca completado y muestra resumen`() = runTest {
        stubSimpleRoutine()
        coEvery { workoutRepo.getUnfinishedForRoutine(1) } returns null
        val inserted = workout(42)
        coEvery { workoutRepo.insert(any()) } returns 42L
        coEvery { workoutRepo.getById(42) } returns inserted
        val vm = TrainViewModel(routineRepo, workoutRepo, exerciseRepo)
        vm.startWorkout(1)
        advanceUntilIdle()
        // reloadSets tras logSet: sin este stub la sesión no ve la serie y finish pediría confirmEmpty
        coEvery { workoutRepo.getSetsForExercise(42, 10) } returns listOf(
            com.selftrain.app.data.model.WorkoutSet(
                id = 1, workoutId = 42, exerciseId = 10,
                setType = "work", reps = 10, weightKg = 50f
            )
        )
        vm.logSet(10, "work", 10, 50f, 2, false)
        advanceUntilIdle()
        coEvery { workoutRepo.getSetsWithExercise(42) } returns listOf(
            com.selftrain.app.data.db.SetWithExercise(
                com.selftrain.app.data.model.WorkoutSet(
                    id = 1, workoutId = 42, exerciseId = 10,
                    setType = "work", reps = 10, weightKg = 50f
                ),
                exerciseName = "Ejercicio 10", muscleGroup = "chest"
            )
        )
        coEvery { workoutRepo.getCompletedWorkoutsBetween(any(), any()) } returns emptyList()

        vm.finishWorkout()
        advanceUntilIdle()

        assertTrue(vm.state.value.isFinishing)
        coVerify { workoutRepo.update(match { it.completed && it.id == 42L }) }
    }

    @Test
    fun `finishWorkout sin series pide confirmacion y no guarda`() = runTest {
        stubSimpleRoutine()
        coEvery { workoutRepo.getUnfinishedForRoutine(1) } returns null
        coEvery { workoutRepo.insert(any()) } returns 42L
        coEvery { workoutRepo.getById(42) } returns workout(42)
        val vm = TrainViewModel(routineRepo, workoutRepo, exerciseRepo)
        vm.startWorkout(1)
        advanceUntilIdle()

        vm.finishWorkout()
        advanceUntilIdle()

        assertTrue(vm.state.value.confirmEmpty)
        assertFalse(vm.state.value.isFinishing)
        coVerify(exactly = 0) { workoutRepo.update(any()) }
    }

    @Test
    fun `discardWorkout borra el workout`() = runTest {
        stubSimpleRoutine()
        coEvery { workoutRepo.getUnfinishedForRoutine(1) } returns null
        coEvery { workoutRepo.insert(any()) } returns 42L
        val vm = TrainViewModel(routineRepo, workoutRepo, exerciseRepo)
        vm.startWorkout(1)
        advanceUntilIdle()
        val toDelete = workout(42)
        coEvery { workoutRepo.getById(42) } returns toDelete

        vm.discardWorkout()
        advanceUntilIdle()

        coVerify { workoutRepo.delete(toDelete) }
    }

    @Test
    fun `setCurrentExercise persiste el indice`() = runTest {
        stubSimpleRoutine()
        coEvery { routineRepo.getWithExercises(1) } returns listOf(re(10), re(20))
        coEvery { exerciseRepo.getById(10) } returns exercise(10)
        coEvery { exerciseRepo.getById(20) } returns exercise(20)
        coEvery { workoutRepo.getSetHistory(10) } returns emptyList()
        coEvery { workoutRepo.getSetHistory(20) } returns emptyList()
        coEvery { workoutRepo.getLastExerciseSession(10) } returns emptyList()
        coEvery { workoutRepo.getLastExerciseSession(20) } returns emptyList()
        coEvery { workoutRepo.getUnfinishedForRoutine(1) } returns null
        coEvery { workoutRepo.insert(any()) } returns 42L
        coEvery { workoutRepo.getById(42) } returns workout(42)
        val vm = TrainViewModel(routineRepo, workoutRepo, exerciseRepo)
        vm.startWorkout(1)
        advanceUntilIdle()

        vm.setCurrentExercise(1)
        advanceUntilIdle()

        assertEquals(1, vm.state.value.currentExerciseIndex)
        coVerify { workoutRepo.update(match { it.lastExerciseIndex == 1 }) }
    }
}
