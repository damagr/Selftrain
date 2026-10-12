package com.selftrain.app.ui.stats

import com.selftrain.app.data.db.SetVolumeRow
import com.selftrain.app.data.model.Workout
import com.selftrain.app.data.repository.ExerciseRepository
import com.selftrain.app.data.repository.WorkoutRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// ponytail: comportamiento de Estadísticas — racha semanal, volumen, peso corporal (F5)
class StatsViewModelTest {

    private val workoutRepo: WorkoutRepository = mockk(relaxed = true)
    private val exerciseRepo: ExerciseRepository = mockk(relaxed = true)
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { exerciseRepo.exercises } returns MutableStateFlow(emptyList())
        every { workoutRepo.getMax1RMPerExercise() } returns MutableStateFlow(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun workout(id: Long, date: Long, completed: Boolean = true, bodyWeight: Float = 0f) =
        Workout(id = id, routineId = 1, date = date, completed = completed, bodyWeight = bodyWeight)

    private fun mondayOfThisWeek(): Long =
        LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun `completedWorkouts filtra incompletos y ordena desc`() = runTest {
        val now = System.currentTimeMillis()
        every { workoutRepo.workouts } returns MutableStateFlow(
            listOf(
                workout(1, now - 1000),
                workout(2, now - 2000),
                workout(3, now - 3000, completed = false)
            )
        )
        val vm = StatsViewModel(workoutRepo, exerciseRepo)

        val list = vm.completedWorkouts.first { it.isNotEmpty() }

        assertEquals(listOf(1L, 2L), list.map { it.id })
    }

    @Test
    fun `racha cuenta semanas consecutivas incluyendo la actual`() = runTest {
        val monday = mondayOfThisWeek()
        val week = 7L * 24 * 60 * 60 * 1000
        every { workoutRepo.workouts } returns MutableStateFlow(
            listOf(
                workout(1, monday),
                workout(2, monday - week),
                workout(3, monday - 2 * week)
            )
        )
        val vm = StatsViewModel(workoutRepo, exerciseRepo)

        assertEquals(3, vm.weeklyStreak.first { it > 0 })
    }

    @Test
    fun `racha se rompe con hueco de una semana`() = runTest {
        val monday = mondayOfThisWeek()
        val week = 7L * 24 * 60 * 60 * 1000
        every { workoutRepo.workouts } returns MutableStateFlow(
            listOf(
                workout(1, monday),
                workout(2, monday - 2 * week)
            )
        )
        val vm = StatsViewModel(workoutRepo, exerciseRepo)

        assertEquals(1, vm.weeklyStreak.first { it > 0 })
    }

    @Test
    fun `racha es cero si la ultima semana no es la actual ni la pasada`() = runTest {
        val monday = mondayOfThisWeek()
        val week = 7L * 24 * 60 * 60 * 1000
        every { workoutRepo.workouts } returns MutableStateFlow(
            listOf(workout(1, monday - 3 * week))
        )
        val vm = StatsViewModel(workoutRepo, exerciseRepo)

        assertEquals(0, vm.weeklyStreak.first())
    }

    @Test
    fun `racha ignora semanas futuras`() = runTest {
        val monday = mondayOfThisWeek()
        val week = 7L * 24 * 60 * 60 * 1000
        every { workoutRepo.workouts } returns MutableStateFlow(
            listOf(workout(1, monday + 2 * week))
        )
        val vm = StatsViewModel(workoutRepo, exerciseRepo)

        assertEquals(0, vm.weeklyStreak.first())
    }

    @Test
    fun `volumen semanal agrupa y cuenta entrenos reales no series`() = runTest {
        val monday = mondayOfThisWeek()
        every { workoutRepo.getCompletedSetVolume() } returns MutableStateFlow(
            listOf(
                SetVolumeRow(workoutId = 1, date = monday, weightKg = 50f, reps = 10),
                SetVolumeRow(workoutId = 1, date = monday, weightKg = 50f, reps = 10),
                SetVolumeRow(workoutId = 2, date = monday + 1000, weightKg = 60f, reps = 8)
            )
        )
        val vm = StatsViewModel(workoutRepo, exerciseRepo)

        val weeks = vm.weeklyVolume.first { it.isNotEmpty() }

        assertEquals(1, weeks.size)
        // 50*10 + 50*10 + 60*8 = 1480
        assertEquals(1480f, weeks[0].volumeKg, 0.01f)
        assertEquals(2, weeks[0].workoutCount)
    }

    @Test
    fun `latestBodyWeight toma el mas reciente registrado`() = runTest {
        val now = System.currentTimeMillis()
        every { workoutRepo.workouts } returns MutableStateFlow(
            listOf(
                workout(1, now, bodyWeight = 0f),
                workout(2, now - 1000, bodyWeight = 75.5f),
                workout(3, now - 2000, bodyWeight = 76f)
            )
        )
        val vm = StatsViewModel(workoutRepo, exerciseRepo)

        assertEquals(75.5f, vm.latestBodyWeight.first { it > 0 })
    }
}
