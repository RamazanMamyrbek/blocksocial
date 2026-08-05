package com.blocksocial.lite.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsageTodayTest {

    @Test
    fun withoutUsageAccessThereIsNoFigureAtAllRatherThanAZero() {
        assertNull(UsageToday.Unavailable.minutesFor("youtube"))
    }

    @Test
    fun anApplicationNotOpenedTodayHasSpentNoMinutes() {
        val measured = UsageToday(minutesByApp = emptyMap(), measurementAvailable = true)

        assertEquals(0, measured.minutesFor("youtube"))
    }

    @Test
    fun aMeasuredApplicationReportsTheMinutesItWasInFront() {
        val measured = UsageToday(mapOf("youtube" to 87), measurementAvailable = true)

        assertEquals(87, measured.minutesFor("youtube"))
    }
}
