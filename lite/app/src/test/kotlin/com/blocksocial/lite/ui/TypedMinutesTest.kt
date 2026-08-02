package com.blocksocial.lite.ui

import com.blocksocial.lite.data.LimitStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TypedMinutesTest {

    @Test
    fun aPlainNumberIsAccepted() {
        assertEquals(45, minutesTypedIn("45"))
    }

    @Test
    fun surroundingSpaceIsForgiven() {
        assertEquals(45, minutesTypedIn(" 45 "))
    }

    @Test
    fun anEmptyFieldIsNotALimit() {
        assertNull(minutesTypedIn(""))
        assertNull(minutesTypedIn("   "))
    }

    @Test
    fun somethingThatIsNotANumberIsNotALimit() {
        assertNull(minutesTypedIn("abc"))
        assertNull(minutesTypedIn("4o"))
    }

    @Test
    fun theEndsOfTheAllowedRangeAreThemselvesAllowed() {
        assertEquals(LimitStore.MINIMUM_MINUTES, minutesTypedIn(LimitStore.MINIMUM_MINUTES.toString()))
        assertEquals(LimitStore.MAXIMUM_MINUTES, minutesTypedIn(LimitStore.MAXIMUM_MINUTES.toString()))
    }

    @Test
    fun aNumberOutsideTheRangeIsRefusedRatherThanQuietlyClamped() {
        assertNull(minutesTypedIn("0"))
        assertNull(minutesTypedIn((LimitStore.MAXIMUM_MINUTES + 1).toString()))
    }

    @Test
    fun anyMinuteFromOneUpwardsCanBeAsked() {
        assertEquals(1, minutesTypedIn("1"))
        assertEquals(7, minutesTypedIn("7"))
        assertEquals(13, minutesTypedIn("13"))
    }
}
