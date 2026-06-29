package com.entrenaguay.app.util

object Labels {
    private val muscleGroupEs = mapOf(
        "chest" to "Pecho",
        "back" to "Espalda",
        "legs" to "Piernas",
        "shoulders" to "Hombros",
        "arms" to "Brazos",
        "core" to "Core"
    )

    private val categoryEs = mapOf(
        "compound" to "Compuesto",
        "isolation" to "Aislamiento"
    )

    private val methodEs = mapOf(
        "bilbo" to "Bilbo",
        "full_body" to "Full Body",
        "push_pull_legs" to "Push-Pull-Legs",
        "ppl" to "Push-Pull-Legs"
    )

    private val equipmentEs = mapOf(
        "barbell" to "Barra",
        "dumbbell" to "Mancuerna",
        "cable" to "Polea",
        "machine" to "Máquina",
        "bodyweight" to "Peso corporal"
    )

    fun muscleGroup(key: String): String = muscleGroupEs[key] ?: key.replaceFirstChar { it.uppercase() }
    fun category(key: String): String = categoryEs[key] ?: key.replaceFirstChar { it.uppercase() }
    fun method(key: String): String = methodEs[key] ?: key.replaceFirstChar { it.uppercase() }
    fun equipment(key: String): String = equipmentEs[key] ?: key.replaceFirstChar { it.uppercase() }
}
