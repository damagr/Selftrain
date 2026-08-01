package com.selftrain.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineShareCodecTest {

    private val sample = SharedRoutine(
        name = "Push/Pull",
        method = "bilbo",
        notes = "Semana 1",
        days = listOf(
            SharedDay("Día 1", listOf(
                SharedExercise("Press banca con barra", "chest", "compound", true, "barbell"),
                SharedExercise("Press inclinado con mancuernas", "chest", "compound", true, "dumbbell")
            )),
            SharedDay("Día 2", listOf(
                SharedExercise("Sentadilla con barra", "legs", "compound", true, "barbell")
            ))
        )
    )

    private fun bigRoutine(exercisesPerDay: Int) = SharedRoutine(
        name = "Programa grande",
        days = (1..10).map { d ->
            SharedDay("Día $d", (1..exercisesPerDay).map { e ->
                SharedExercise("Ejercicio $d-$e con nombre largo para probar la compresión", "muscleGroup", "compound", true, "barbell")
            })
        }
    )

    @Test fun roundTrip_plainJson() {
        val payload = RoutineShareCodec.encode(sample)
        assertTrue("esperado prefijo ST1, got ${payload.take(12)}", payload.startsWith("ST1"))
        assertEquals(sample, RoutineShareCodec.decode(payload))
    }

    @Test fun roundTrip_deflated_whenLarge() {
        val big = bigRoutine(exercisesPerDay = 8) // 80 ejercicios -> ~8KB -> comprime
        val payload = RoutineShareCodec.encode(big)
        assertTrue("esperado prefijo STZ1, got ${payload.take(12)}", payload.startsWith("STZ1"))
        assertEquals(big, RoutineShareCodec.decode(payload))
    }

    @Test fun decode_garbage_isNull() {
        assertNull(RoutineShareCodec.decode(""))
        assertNull(RoutineShareCodec.decode("hola que tal"))
        assertNull(RoutineShareCodec.decode("ST1 no-es-json"))
        assertNull(RoutineShareCodec.decode("STZ1 base64-rotto"))
    }
}
