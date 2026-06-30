package com.selftrain.app.util

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
 * - If all sets >12 reps → increase weight next session
 * - If first set <8 reps → decrease weight next session
 */
object BilboProgression {

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

    /** New Bilbo weight after reaching 50 reps: ~10% increase */
    fun increasedBilboWeight(currentBilboWeight: Float): Float = currentBilboWeight * 1.10f

    /** Work set progression suggestion */
    enum class WorkProgression { INCREASE, MAINTAIN, DECREASE }

    fun workProgression(workSetReps: List<Int>): WorkProgression = when {
        workSetReps.isEmpty() -> WorkProgression.MAINTAIN
        workSetReps.first() < 8 -> WorkProgression.DECREASE
        workSetReps.all { it > 12 } -> WorkProgression.INCREASE
        else -> WorkProgression.MAINTAIN
    }

    /** Epley formula for estimated 1RM */
    fun estimated1RM(weightKg: Float, reps: Int): Float {
        if (reps == 1) return weightKg
        if (reps <= 0) return 0f
        return weightKg * (1.0f + reps / 30.0f)
    }

    /** Must apply Bilbo to this exercise? */
    fun appliesTo(exerciseIsCompound: Boolean, method: String): Boolean =
        method.lowercase() == "bilbo" && exerciseIsCompound

    /** Suggested Bilbo weight from last work set data */
    fun suggestBilboFromWorkSets(lastWorkSetWeight: Float): Float =
        lastWorkSetWeight / 1.40f  // work weight / 1.4 ≈ bilbo weight
}
