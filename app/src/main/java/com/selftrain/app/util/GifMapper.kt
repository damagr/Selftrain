package com.selftrain.app.util

// ponytail: hardcoded map from exercise name to CDN gifUrl. No DB field, no migration.
// Matches the 54 seed exercises from exercises.json against JahelCuadrado/ExerciseGymGifsDB.
// Add new entries here when new exercises are added to the seed.
private val gifMap: Map<String, String> by lazy {
    val base = "https://cdn.jsdelivr.net/gh/JahelCuadrado/ExerciseGymGifsDB@v1.1.0"
    mapOf(
        // Pecho
        "Press banca con barra" to "$base/pectorals/barbell-bench-press.gif",
        "Press banca con mancuernas" to "$base/pectorals/dumbbell-bench-press.gif",
        "Press inclinado con barra" to "$base/pectorals/barbell-incline-bench-press.gif",
        "Press inclinado con mancuernas" to "$base/pectorals/dumbbell-incline-bench-press.gif",
        "Press declinado" to "$base/pectorals/barbell-decline-bench-press.gif",
        "Aperturas con mancuernas" to "$base/pectorals/dumbbell-fly.gif",
        "Cruce de poleas" to "$base/pectorals/cable-standing-fly.gif",
        "Fondos en paralelas" to "$base/pectorals/chest-dip.gif",
        "Aperturas en máquina" to "$base/pectorals/lever-seated-fly.gif",
        "Press declinado en máquina" to "$base/pectorals/lever-decline-chest-press.gif",
        "Cruce polea baja-arriba" to "$base/pectorals/cable-upper-chest-crossovers.gif",
        "Cruce polea alta-abajo" to "$base/pectorals/cable-decline-fly.gif",

        // Piernas
        "Sentadilla con barra" to "$base/glutes/barbell-high-bar-squat.gif",
        "Prensa de piernas" to "$base/glutes/sled-45-leg-press.gif",
        "Peso muerto convencional" to "$base/glutes/barbell-deadlift.gif",
        "Peso muerto rumano" to "$base/glutes/barbell-romanian-deadlift.gif",
        "Zancadas con mancuernas" to "$base/glutes/dumbbell-lunge.gif",
        "Extensión de cuádriceps" to "$base/quads/lever-leg-extension.gif",
        "Curl femoral tumbado" to "$base/hamstrings/lever-lying-leg-curl.gif",
        "Curl femoral sentado" to "$base/hamstrings/lever-seated-leg-curl.gif",
        "Elevación de gemelos de pie" to "$base/calves/lever-standing-calf-raise.gif",
        "Sentadilla búlgara" to "$base/quads/dumbbell-single-leg-split-squat.gif",
        "Hip thrust con barra" to "$base/glutes/barbell-glute-bridge-two-legs-on-bench-male.gif",
        "Abducción en máquina" to "$base/abductors/lever-seated-hip-abduction.gif",

        // Espalda
        "Remo con barra" to "$base/upper-back/barbell-incline-row.gif",
        "Remo con mancuerna a una mano" to "$base/upper-back/dumbbell-one-arm-bent-over-row.gif",
        "Jalón al pecho" to "$base/lats/cable-pulldown.gif",
        "Dominadas" to "$base/lats/pull-up.gif",
        "Remo en polea baja sentado" to "$base/upper-back/cable-seated-row.gif",
        "Remo al cuello" to "$base/delts/barbell-upright-row.gif",
        "Pullover con mancuerna" to "$base/pectorals/dumbbell-pullover.gif",
        "Remo en polea unilateral" to "$base/upper-back/cable-seated-one-arm-alternate-row.gif",
        "Pullover en polea" to "$base/lats/cable-lying-extension-pullover-with-rope-attachment.gif",
        "Jalón cerrado en polea" to "$base/lats/cable-lateral-pulldown-with-v-bar.gif",

        // Hombros
        "Press militar con barra" to "$base/delts/barbell-standing-close-grip-military-press.gif",
        "Press militar con mancuernas" to "$base/delts/dumbbell-seated-shoulder-press.gif",
        "Elevaciones laterales" to "$base/delts/dumbbell-lateral-raise.gif",
        "Elevaciones frontales" to "$base/delts/dumbbell-front-raise.gif",
        "Pájaro en polea" to "$base/delts/cable-standing-rear-delt-row-with-rope.gif",
        "Face pull" to "$base/delts/cable-rear-delt-row-with-rope.gif",
        "Elevación lateral en polea" to "$base/delts/cable-lateral-raise.gif",
        "Rotación externa en polea" to "$base/delts/cable-standing-shoulder-external-rotation.gif",

        // Brazos
        "Curl con barra" to "$base/biceps/barbell-curl.gif",
        "Curl con mancuernas" to "$base/biceps/dumbbell-standing-biceps-curl.gif",
        "Curl martillo" to "$base/biceps/dumbbell-hammer-curl.gif",
        "Extensiones de tríceps en polea" to "$base/triceps/cable-alternate-triceps-extension.gif",
        "Press francés" to "$base/triceps/barbell-lying-triceps-extension-skull-crusher.gif",
        "Fondos en banco" to "$base/triceps/bench-dip-knees-bent.gif",
        "Curl bíceps banco scott" to "$base/biceps/barbell-preacher-curl.gif",
        "Curl bíceps en polea baja" to "$base/biceps/cable-curl.gif",
        "Press tríceps en cuerda" to "$base/triceps/cable-pushdown-with-rope-attachment.gif",

        // Core
        "Plancha abdominal" to "$base/abs/front-plank-with-twist.gif",
        "Crunch en polea" to "$base/abs/cable-kneeling-crunch.gif",
        "Elevación de piernas colgado" to "$base/abs/hanging-leg-raise.gif",
    )
}

fun getExerciseGifUrl(exerciseName: String): String? = gifMap[exerciseName]
