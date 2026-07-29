package com.blocksocial.core.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class MotionTokensTest {

    @Test
    fun `full motion uses the documented durations`() {
        assertEquals(120, MotionDurations.Full.blockAppearMillis)
        assertEquals(180, MotionDurations.Full.stateChangeMillis)
        assertEquals(240, MotionDurations.Full.sheetMillis)
    }

    @Test
    fun `a zero animator scale collapses every duration to zero`() {
        val reduced = MotionDurations.forAnimatorDurationScale(0f)
        assertEquals(0, reduced.blockAppearMillis)
        assertEquals(0, reduced.stateChangeMillis)
        assertEquals(0, reduced.sheetMillis)
    }

    @Test
    fun `a non-zero animator scale keeps the full durations`() {
        assertEquals(MotionDurations.Full, MotionDurations.forAnimatorDurationScale(1f))
        assertEquals(MotionDurations.Full, MotionDurations.forAnimatorDurationScale(0.5f))
        assertEquals(MotionDurations.Full, MotionDurations.forAnimatorDurationScale(10f))
    }
}
