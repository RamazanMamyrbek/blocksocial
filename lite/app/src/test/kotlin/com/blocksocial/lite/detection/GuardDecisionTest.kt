package com.blocksocial.lite.detection

import com.blocksocial.lite.data.GuardSetting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuardDecisionTest {

    @Test
    fun theGuardRunsWhileALimitExistsAndTheSwitchIsOn() {
        assertEquals(GuardDecision.RUN, guardDecisionFor(limitCount = 1, guardEnabled = true))
    }

    @Test
    fun removingTheLastLimitTakesTheGuardAndItsNotificationAway() {
        assertEquals(GuardDecision.STOP, guardDecisionFor(limitCount = 0, guardEnabled = true))
    }

    @Test
    fun switchingTheGuardOffStopsItEvenWhileLimitsAreSet() {
        assertEquals(GuardDecision.STOP, guardDecisionFor(limitCount = 3, guardEnabled = false))
    }

    @Test
    fun theGuardIsOnUntilTheUserTurnsItOff() {
        assertTrue(GuardSetting.DEFAULT_ENABLED)
    }
}
