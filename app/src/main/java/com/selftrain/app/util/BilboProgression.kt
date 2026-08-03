package com.selftrain.app.util

import kotlin.math.roundToInt

/**
 * Bilbo method progression calculator.
 *
 * Bilbo set rules:
 * - 15-50 reps at ~50% 1RM
 * - Explosive concentric, controlled eccentric
 * - RIR 1-3 (never to failure)
 * - Add reps each session with same weight
 * - At 50 clean reps → increase weight ~10%, reset to 15-20 reps
 *
 * Work set rules:
 * - 3-4 sets at 8-12 reps
 * - ~40% more weight than Bilbo set
 * - Progression thresholds on EFFECTIVE reps (reps + RIR):
 *   if effective <8 → decrease weight next session
 *   if effective >10 → increase weight next session
 */
object BilboProgression {

    /** Effective reps = logged reps + RIR (7 reps RIR 1 = 8, 10 reps RIR 2 = 12) */
    fun effectiveReps(reps: Int, rir: Int): Int = reps + rir

    /** Estimated Bilbo weight: ~50% of estimated 1RM */
    fun bilboWeight(estimated1RM: Float): Float = estimated1RM * 0.50f

    /** Work set weight: ~40% more than Bilbo weight, which is ~70% 1RM */
    fun workWeight(bilboWeightKg: Float): Float = bilboWeightKg * 1.40f

    /** Bilbo reps suggestion: if prev >= 50, reset. Otherwise add 1-3 reps. */
    fun suggestBilboReps(prevReps: Int): Int = when {
        prevReps >= 50 -> (15..20).random()
        prevReps == 0 -> 15   // first session
        else -> (prevReps + 1).coerceAtMost(50)
    }

    /** Check if Bilbo weight should increase */
    fun shouldIncreaseBilboWeight(lastReps: Int): Boolean = lastReps >= 50

    /** New Bilbo weight after reaching 50 reps: ~10% increase (15% if user hit failure at the milestone) */
    fun increasedBilboWeight(currentBilboWeight: Float, rir: Int = 2): Float =
        if (rir == 0) currentBilboWeight * 1.15f else currentBilboWeight * 1.10f

    /**
     * Work set progression suggestion.
     * `workSetReps` son reps EFECTIVAS (reps + RIR) — el caller las calcula.
     */
    enum class WorkProgression { INCREASE, MAINTAIN, DECREASE }

    fun workProgression(workSetReps: List<Int>): WorkProgression = when {
        workSetReps.isEmpty() -> WorkProgression.MAINTAIN
        workSetReps.first() < 8 -> WorkProgression.DECREASE
        workSetReps.all { it > 10 } -> WorkProgression.INCREASE
        else -> WorkProgression.MAINTAIN
    }

    /**
     * Intra-session advice after each work set, based on the last logged set.
     * Thresholds on effective reps (reps + RIR).
     * Returns null when effective reps are in the maintain range (8..10).
     *
     * ponytail: asymmetric factor is intentional — a miss (<8) needs a bigger reset
     * than the bump for exceeding range (>10). Upgrade path: per-exercise adaptive
     * factor if progression data shows 10% is too aggressive.
     */
    fun workSetAdjustment(reps: Int, rir: Int = 0, weightKg: Float): Pair<WorkProgression, Float>? {
        val effective = effectiveReps(reps, rir)
        return when {
            effective < 8 -> WorkProgression.DECREASE to weightKg * 0.90f
            effective > 10 -> WorkProgression.INCREASE to weightKg * 1.05f
            else -> null
        }
    }

    /** Redondea a mancuernas (paso de 2.5kg): al múltiplo más cercano */
    fun roundToDumbbellStep(kg: Float): Float = (kg / 2.5f).roundToInt() * 2.5f

    /** Epley formula for estimated 1RM */
    fun estimated1RM(weightKg: Float, reps: Int): Float {
        if (reps == 1) return weightKg
        if (reps <= 0) return 0f
        return weightKg * (1.0f + reps / 30.0f)
    }

    /** Suggested Bilbo weight from last work set data */
    fun suggestBilboFromWorkSets(lastWorkSetWeight: Float): Float =
        lastWorkSetWeight / 1.40f  // work weight / 1.4 ≈ bilbo weight
}
