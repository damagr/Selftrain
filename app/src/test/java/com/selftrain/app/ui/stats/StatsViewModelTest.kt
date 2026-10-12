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
import org.junit.Assert.assertTrue
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
    fun `reps semanales conseguidas vs objetivo excluyendo bilbo`() = runTest {
        val monday = mondayOfThisWeek()
        every { workoutRepo.getCompletedSetVolume() } returns MutableStateFlow(
            listOf(
                // entreno 1 (misma semana): 2 series de trabajo + 1 Bilbo (excluida)
                SetVolumeRow(workoutId = 1, date = monday, weightKg = 50f, reps = 10, setType = "work", rir = 2),
                SetVolumeRow(workoutId = 1, date = monday, weightKg = 50f, reps = 8, setType = "work", rir = 0),
                SetVolumeRow(workoutId = 1, date = monday, weightKg = 25f, reps = 30, setType = "bilbo", rir = 5),
                // entreno 2 (misma semana): 1 serie de trabajo
                SetVolumeRow(workoutId = 2, date = monday + 1000, weightKg = 60f, reps = 12, setType = "work", rir = 1),
                // semana anterior: 1 serie
                SetVolumeRow(workoutId = 3, date = monday - 7L * 24 * 60 * 60 * 1000, weightKg = 55f, reps = 9, setType = "work", rir = 2)
            )
        )
        val vm = StatsViewModel(workoutRepo, exerciseRepo)

        val weeks = vm.weeklyReps.first { it.isNotEmpty() }

        assertEquals(2, weeks.size)
        val thisWeek = weeks.last()
        // conseguidas: 10 + 8 + 12 = 30 (la Bilbo de 30 reps NO cuenta)
        assertEquals(30, thisWeek.achievedReps)
        // objetivo rango: 3 series de trabajo × 8-12 = 24-36
        assertEquals(24, thisWeek.targetMinReps)
        assertEquals(36, thisWeek.targetMaxReps)
        assertEquals(2, thisWeek.workoutCount)
        // semana anterior: 9 conseguidas, objetivo 8-12
        assertEquals(9, weeks[0].achievedReps)
        assertEquals(8, weeks[0].targetMinReps)
        assertEquals(12, weeks[0].targetMaxReps)
    }

    @Test
    fun `reps semanales sin series de trabajo no genera semana`() = runTest {
        val monday = mondayOfThisWeek()
        every { workoutRepo.getCompletedSetVolume() } returns MutableStateFlow(
            listOf(
                SetVolumeRow(workoutId = 1, date = monday, weightKg = 25f, reps = 30, setType = "bilbo", rir = 5)
            )
        )
        val vm = StatsViewModel(workoutRepo, exerciseRepo)

        assertEquals(0, vm.weeklyReps.first().size)
    }

    @Test
    fun `weeklyReps no recorta el historico`() = runTest {
        val monday = mondayOfThisWeek()
        val week = 7L * 24 * 60 * 60 * 1000
        // 10 semanas de datos: antes el ViewModel recortaba a 12 pero si hubiera 13+ se perderían
        val rows = (0L until 10).map { i ->
            SetVolumeRow(workoutId = i, date = monday - i * week, weightKg = 50f, reps = 10, setType = "work", rir = 2)
        }
        every { workoutRepo.getCompletedSetVolume() } returns MutableStateFlow(rows)
        val vm = StatsViewModel(workoutRepo, exerciseRepo)

        assertEquals(10, vm.weeklyReps.first { it.isNotEmpty() }.size)
    }

    @Test
    fun `pageOfWeeks pagina desde la semana mas reciente`() {
        val weeks = (1..10).map { i ->
            WeekReps(
                weekStart = LocalDate.of(2026, 1, 1).plusWeeks(i.toLong() - 1),
                achievedReps = i * 10, targetMinReps = 8, targetMaxReps = 12, workoutCount = 1
            )
        } // semanas ordenadas ascendente: índice 9 = la más reciente

        // página 0 = las 8 más recientes (índices 2..9)
        val p0 = pageOfWeeks(weeks, page = 0, pageSize = 8)
        assertEquals(8, p0.size)
        assertEquals(weeks[2].weekStart, p0.first().weekStart)
        assertEquals(weeks[9].weekStart, p0.last().weekStart)

        // página 1 = las 2 restantes (más antiguas), recortadas
        val p1 = pageOfWeeks(weeks, page = 1, pageSize = 8)
        assertEquals(2, p1.size)
        assertEquals(weeks[0].weekStart, p1.first().weekStart)
        assertEquals(weeks[1].weekStart, p1.last().weekStart)

        // página fuera de rango → vacía; página negativa → coerce a la 0 (la más reciente)
        assertTrue(pageOfWeeks(weeks, page = 2, pageSize = 8).isEmpty())
        assertEquals(p0, pageOfWeeks(weeks, page = -1, pageSize = 8))
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
