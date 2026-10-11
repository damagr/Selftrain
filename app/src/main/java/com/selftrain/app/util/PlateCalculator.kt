package com.selftrain.app.util

// ponytail: calculadora de discos — cómo cargar la barra para alcanzar un peso objetivo.
// Lógica pura (testeable): reparto por lados con los discos disponibles, greedily del mayor al menor.
object PlateCalculator {

    // Discos estándar de gimnasio, por unidad (por lado)
    val STANDARD_PLATES = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)

    data class Result(
        val perSide: List<Double>,   // discos por lado, del mayor al menor
        val totalKg: Double,         // peso real cargado (barra + 2 × lados)
        val remainderKg: Double      // kg que no se pudieron cargar con los discos disponibles
    )

    fun compute(
        targetKg: Double,
        barKg: Double = 20.0,
        available: List<Double> = STANDARD_PLATES
    ): Result {
        if (targetKg <= barKg) return Result(emptyList(), barKg, 0.0)

        var remaining = round3((targetKg - barKg) / 2.0)
        val plates = mutableListOf<Double>()
        for (plate in available.sortedDescending()) {
            if (plate <= 0) continue
            while (remaining >= plate - 1e-6) {
                plates += plate
                remaining = round3(remaining - plate)
            }
        }

        val loadedPerSide = plates.sum()
        val total = round3(barKg + loadedPerSide * 2.0)
        val remainder = round3((targetKg - total).coerceAtLeast(0.0))
        return Result(plates, total, remainder)
    }

    private fun round3(v: Double): Double = Math.round(v * 1000.0) / 1000.0
}
