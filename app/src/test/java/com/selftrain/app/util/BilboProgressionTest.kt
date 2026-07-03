package com.selftrain.app.util

import com.selftrain.app.util.BilboProgression.WorkProgression
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class BilboProgressionTest {

    // --- weight derivation ---

    @Test fun bilboWeight_isHalfOf1RM() {
        assertEquals(50f, BilboProgression.bilboWeight(100f), 0.001f)
        assertEquals(0f, BilboProgression.bilboWeight(0f), 0.001f)
    }

    @Test fun workWeight_isFortyPercentAboveBilbo() {
        assertEquals(70f, BilboProgression.workWeight(50f), 0.001f)
    }

    @Test fun suggestBilboFromWorkSets_invertsWorkFactor() {
        assertEquals(50f, BilboProgression.suggestBilboFromWorkSets(70f), 0.001f)
    }

    // --- bilbo reps progression ---

    @Test fun suggestBilboReps_at50ResetsTo15to20() {
        val r = BilboProgression.suggestBilboReps(50)
        assertTrue("expected 15..20, got $r", r in 15..20)
    }

    @Test fun suggestBilboReps_firstSessionIs15() {
        assertEquals(15, BilboProgression.suggestBilboReps(0))
    }

    @Test fun suggestBilboReps_addsOneCappedAt50() {
        assertEquals(16, BilboProgression.suggestBilboReps(15))
        assertEquals(50, BilboProgression.suggestBilboReps(49))
        // at 50 it resets to 15..20 (covered by suggestBilboReps_at50ResetsTo15to20)
    }

    // --- bilbo weight increase ---

    @Test fun shouldIncreaseBilboWeight_atOrAbove50() {
        assertTrue(BilboProgression.shouldIncreaseBilboWeight(50))
        assertTrue(BilboProgression.shouldIncreaseBilboWeight(55))
        assertTrue(!BilboProgression.shouldIncreaseBilboWeight(49))
    }

    @Test fun increasedBilboWeight_isTenPercent() {
        assertEquals(44f, BilboProgression.increasedBilboWeight(40f), 0.001f)
    }

    // --- work progression (between sessions) ---

    @Test fun workProgression_emptyIsMaintain() {
        assertEquals(WorkProgression.MAINTAIN, BilboProgression.workProgression(emptyList()))
    }

    @Test fun workProgression_firstBelow8Decreases() {
        assertEquals(WorkProgression.DECREASE, BilboProgression.workProgression(listOf(7, 10, 10)))
    }

    @Test fun workProgression_allAbove10Increases() {
        assertEquals(WorkProgression.INCREASE, BilboProgression.workProgression(listOf(11, 12)))
    }

    @Test fun workProgression_mixedIsMaintain() {
        assertEquals(WorkProgression.MAINTAIN, BilboProgression.workProgression(listOf(9, 12)))
        assertEquals(WorkProgression.MAINTAIN, BilboProgression.workProgression(listOf(8, 10)))
    }

    // --- work set adjustment (intra-session) ---

    @Test fun workSetAdjustment_below8Decreases10percent() {
        val (prog, w) = BilboProgression.workSetAdjustment(7, 20f)!!
        assertEquals(WorkProgression.DECREASE, prog)
        assertEquals(18f, w, 0.001f)
    }

    @Test fun workSetAdjustment_above10Increases5percent() {
        val (prog, w) = BilboProgression.workSetAdjustment(12, 20f)!!
        assertEquals(WorkProgression.INCREASE, prog)
        assertEquals(21f, w, 0.001f)
        val (p2, w2) = BilboProgression.workSetAdjustment(11, 20f)!!
        assertEquals(WorkProgression.INCREASE, p2)
        assertEquals(21f, w2, 0.001f)
    }

    @Test fun workSetAdjustment_maintainRangeReturnsNull() {
        assertNull(BilboProgression.workSetAdjustment(8, 20f))
        assertNull(BilboProgression.workSetAdjustment(9, 20f))
        assertNull(BilboProgression.workSetAdjustment(10, 20f))
    }

    // --- estimated 1RM (Epley) ---

    @Test fun estimated1RM_rep1IsWeight() {
        assertEquals(100f, BilboProgression.estimated1RM(100f, 1), 0.001f)
    }

    @Test fun estimated1RM_nonPositiveRepsIsZero() {
        assertEquals(0f, BilboProgression.estimated1RM(100f, 0), 0.001f)
        assertEquals(0f, BilboProgression.estimated1RM(100f, -3), 0.001f)
    }

    @Test fun estimated1RM_epleyFormula() {
        // 100 * (1 + 10/30) = 133.33
        val got = BilboProgression.estimated1RM(100f, 10)
        assertTrue("expected ~133.33, got $got", abs(got - 133.3333f) < 0.01f)
    }

    // --- appliesTo ---

    @Test fun appliesTo_bilboCompoundTrue() {
        assertTrue(BilboProgression.appliesTo(true, "bilbo"))
        assertTrue(BilboProgression.appliesTo(true, "Bilbo"))
    }

    @Test fun appliesTo_otherwiseFalse() {
        assertTrue(!BilboProgression.appliesTo(false, "bilbo"))   // isolation
        assertTrue(!BilboProgression.appliesTo(true, "ppl"))      // non-bilbo method
        assertTrue(!BilboProgression.appliesTo(true, "full body"))
    }
}
