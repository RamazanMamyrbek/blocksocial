package com.blocksocial.lite.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyLimitTest {

    @Test
    fun anApplicationWithoutALimitHasNoStatusAtAll() {
        assertNull(DailyLimit.statusOf(limitMinutes = null, usedMinutes = 40))
    }

    @Test
    fun timeLeftIsTheLimitMinusWhatWasUsed() {
        val status = DailyLimit.statusOf(limitMinutes = 30, usedMinutes = 12)!!

        assertEquals(18, status.remainingMinutes)
        assertFalse(status.reached)
    }

    @Test
    fun theLimitIsReachedOnTheMinuteItIsMet() {
        assertTrue(DailyLimit.statusOf(limitMinutes = 30, usedMinutes = 30)!!.reached)
        assertFalse(DailyLimit.statusOf(limitMinutes = 30, usedMinutes = 29)!!.reached)
    }

    @Test
    fun goingOverTheLimitNeverShowsNegativeTimeLeft() {
        val status = DailyLimit.statusOf(limitMinutes = 30, usedMinutes = 47)!!

        assertEquals(0, status.remainingMinutes)
        assertTrue(status.reached)
    }

    @Test
    fun withoutAMeasurementTheLimitCannotBeReached() {
        val status = DailyLimit.statusOf(limitMinutes = 5, usedMinutes = null)!!

        assertFalse(status.measured)
        assertFalse(status.reached)
        assertNull(status.remainingMinutes)
    }
}
