package com.selftrain.app.ui.routines

import android.app.Application
import com.selftrain.app.data.model.Exercise
import com.selftrain.app.data.model.Routine
import com.selftrain.app.data.model.RoutineExercise
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
import org.junit.Before
import org.junit.Test

// ponytail: herencia de método en días de programa (F: bug bilbo obligatorio en rutinas no-bilbo)
class RoutinesViewModelTest {

    private val app: Application = mockk(relaxed = true)
    private val routineRepo: RoutineRepository = mockk(relaxed = true)
    private val exerciseRepo: ExerciseRepository = mockk(relaxed = true)
    private val workoutRepo: WorkoutRepository = mockk(relaxed = true)
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `createChildRoutine hereda el metodo del programa padre`() = runTest {
        coEvery { routineRepo.getById(9) } returns Routine(id = 9, name = "Programa", method = "full_body")
        coEvery { routineRepo.getAllList() } returns emptyList()
        coEvery { routineRepo.insert(any()) } returns 77L
        val vm = RoutinesViewModel(app, routineRepo, exerciseRepo, workoutRepo)

        var createdId = 0L
        vm.createChildRoutine(parentId = 9, name = "Día 1") { createdId = it }
        advanceUntilIdle()

        assertEquals(77L, createdId)
        coVerify {
            routineRepo.insert(match { it.method == "full_body" && it.parentId == 9L })
        }
    }

    @Test
    fun `createChildRoutine sin padre hereda del primer hijo existente`() = runTest {
        coEvery { routineRepo.getById(9) } returns null
        coEvery { routineRepo.getAllList() } returns listOf(
            Routine(id = 1, name = "Día viejo", method = "ppl", parentId = 9)
        )
        coEvery { routineRepo.insert(any()) } returns 78L
        val vm = RoutinesViewModel(app, routineRepo, exerciseRepo, workoutRepo)

        vm.createChildRoutine(parentId = 9, name = "Día 2") { }
        advanceUntilIdle()

        coVerify {
            routineRepo.insert(match { it.method == "ppl" })
        }
    }
}
