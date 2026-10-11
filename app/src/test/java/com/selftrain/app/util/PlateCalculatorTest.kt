package com.selftrain.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlateCalculatorTest {

    @Test
    fun `peso alcanzable exacto`() {
        // 100 kg con barra 20 → 40 kg por lado → 25 + 15
        val r = PlateCalculator.compute(100.0)
        assertEquals(listOf(25.0, 15.0), r.perSide)
        assertEquals(100.0, r.totalKg, 1e-9)
        assertEquals(0.0, r.remainderKg, 1e-9)
    }

    @Test
    fun `peso menor o igual a la barra devuelve solo barra`() {
        val r = PlateCalculator.compute(15.0, barKg = 20.0)
        assertTrue(r.perSide.isEmpty())
        assertEquals(20.0, r.totalKg, 1e-9)
        assertEquals(0.0, r.remainderKg, 1e-9)
    }

    @Test
    fun `peso no alcanzable exacto devuelve resto`() {
        // 97.5 kg con barra 20 → 38.75 por lado → 25 + 10 + 2.5 + 1.25 = 38.75 exacto
        val r = PlateCalculator.compute(97.5)
        assertEquals(listOf(25.0, 10.0, 2.5, 1.25), r.perSide)
        assertEquals(0.0, r.remainderKg, 1e-9)

        // 98 kg con barra 20 → 39 por lado → 25 + 10 + 2.5 + 1.25 = 38.75 → total 97.5, faltan 0.5 kg (en total)
        val r2 = PlateCalculator.compute(98.0)
        assertEquals(0.5, r2.remainderKg, 1e-9)
        assertEquals(97.5, r2.totalKg, 1e-9)
    }

    @Test
    fun `discos limitados devuelven lo mejor posible`() {
        // solo hay 10 y 5: 90 kg con barra 20 → 35 por lado → 10+10+10+5, resto 0
        val r = PlateCalculator.compute(90.0, available = listOf(10.0, 5.0))
        assertEquals(listOf(10.0, 10.0, 10.0, 5.0), r.perSide)
        assertEquals(0.0, r.remainderKg, 1e-9)

        // 80 kg con barra 20 → 30 por lado → 10+10+10 = 30 exacto
        val r2 = PlateCalculator.compute(80.0, available = listOf(10.0))
        assertEquals(listOf(10.0, 10.0, 10.0), r2.perSide)
    }
}
