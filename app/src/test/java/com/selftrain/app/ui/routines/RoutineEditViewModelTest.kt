package com.selftrain.app.ui.routines

import com.selftrain.app.data.model.RoutineExercise
import com.selftrain.app.data.model.Exercise
import com.selftrain.app.data.model.Routine
import com.selftrain.app.data.repository.ExerciseRepository
import com.selftrain.app.data.repository.RoutineRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

// ponytail: editor de rutinas — mapeo de descanso, replace y guardado transaccional (F5)
class RoutineEditViewModelTest {

    private val routineRepo: RoutineRepository = mockk(relaxed = true)
    private val exerciseRepo: ExerciseRepository = mockk(relaxed = true)
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { exerciseRepo.exercises } returns MutableStateFlow(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun exercise(id: Long, bilboEligible: Boolean = true) = Exercise(
        id = id, name = "Ejercicio $id", muscleGroup = "chest",
        category = "compound", isBilboEligible = bilboEligible
    )

    private fun seedRoutine(method: String = "fullbody", bilboEligible: Boolean = true) {
        coEvery { routineRepo.getById(1) } returns Routine(id = 1, name = "Test", method = method)
        coEvery { routineRepo.getWithExercises(1) } returns listOf(
            RoutineExercise(id = 5, routineId = 1, exerciseId = 10, order = 0, isBilbo = false, restSeconds = 120)
        )
        coEvery { exerciseRepo.getByIds(listOf(10)) } returns listOf(exercise(10, bilboEligible))
    }

    private suspend fun kotlinx.coroutines.test.TestScope.loadedVm(): RoutineEditViewModel {
        val vm = RoutineEditViewModel(routineRepo, exerciseRepo)
        vm.loadRoutine(1)
        advanceUntilIdle()
        return vm
    }

    @Test
    fun `loadRoutine mapea restSeconds y routineExerciseId`() = runTest {
        seedRoutine()
        val vm = loadedVm()

        val item = vm.routineExercises.value[0]
        assertEquals(120, item.restSeconds)
        assertEquals(5L, item.routineExerciseId)
    }

    @Test
    fun `setRest solo cambia estado local y hace clamp`() = runTest {
        seedRoutine()
        val vm = loadedVm()
        val item = vm.routineExercises.value[0]

        vm.setRest(item, seconds = 999)
        assertEquals(300, vm.routineExercises.value[0].restSeconds)

        vm.setRest(vm.routineExercises.value[0], seconds = -5)
        assertEquals(0, vm.routineExercises.value[0].restSeconds)

        // no escribe en BD al instante: persiste en save()
        coVerify(exactly = 0) { routineRepo.setRest(any(), any()) }
    }

    @Test
    fun `replaceExercise conserva el restSeconds del slot`() = runTest {
        seedRoutine()
        val vm = loadedVm()

        vm.replaceExercise(0, exercise(20))
        advanceUntilIdle()

        val replaced = vm.routineExercises.value[0]
        assertEquals(20L, replaced.exercise.id)
        assertEquals(120, replaced.restSeconds)
    }

    @Test
    fun `save persiste via saveRoutineExercises con items mapeados`() = runTest {
        seedRoutine()
        val vm = loadedVm()
        vm.setRest(vm.routineExercises.value[0], seconds = 90)
        var saved = false

        vm.save { saved = true }
        advanceUntilIdle()

        coVerify(exactly = 1) {
            routineRepo.saveRoutineExercises(
                match { it.id == 1L },
                match { items ->
                    items.size == 1 && items[0].exerciseId == 10L &&
                        items[0].restSeconds == 90 && items[0].order == 0
                }
            )
        }
        assertEquals(true, saved)
    }

    @Test
    fun `save de rutina bilbo sin marca bloquea con mensaje`() = runTest {
        // ejercicio NO elegible → el backfill de loadRoutine no puede marcar Bilbo
        seedRoutine(method = "bilbo", bilboEligible = false)
        val vm = loadedVm()
        var saved = false

        vm.save { saved = true }
        advanceUntilIdle()

        assertEquals("Marca al menos un ejercicio como Serie Bilbo", vm.message)
        assertEquals(false, saved)
        coVerify(exactly = 0) { routineRepo.saveRoutineExercises(any(), any()) }
    }
}
