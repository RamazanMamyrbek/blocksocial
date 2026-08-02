package com.blocksocial.lite.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class ForegroundSessionsTest {

    private val youtube = "youtube"
    private val packageToApp = mapOf("com.google.android.youtube" to youtube)

    private val dayStart = at("2026-07-28T00:00:00+09:00")
    private val now = at("2026-07-28T10:00:00+09:00")
    private val bootedYesterday = at("2026-07-27T08:00:00+09:00")

    private fun at(text: String) = Instant.parse(text).toEpochMilli()

    private fun foreground(packageName: String, text: String) =
        UsageEvent(packageName, at(text), UsageEventType.MOVED_TO_FOREGROUND)

    private fun leftForeground(packageName: String, text: String) =
        UsageEvent(packageName, at(text), UsageEventType.LEFT_FOREGROUND)

    private fun screenOff(text: String) =
        UsageEvent("android", at(text), UsageEventType.SCREEN_NON_INTERACTIVE)

    private fun minutes(events: List<UsageEvent>, bootedAt: Long = bootedYesterday) =
        ForegroundSessions.minutesByApp(events, packageToApp, dayStart, now, bootedAt)

    @Test
    fun aVisitIsMeasuredFromEntryToTheNextForegroundChange() {
        val measured = minutes(
            listOf(
                foreground("com.google.android.youtube", "2026-07-28T09:00:00+09:00"),
                foreground("com.android.chrome", "2026-07-28T09:20:00+09:00"),
            ),
        )

        assertEquals(20, measured[youtube])
    }

    @Test
    fun theScreenGoingOffEndsTheVisit() {
        val measured = minutes(
            listOf(
                foreground("com.google.android.youtube", "2026-07-28T09:00:00+09:00"),
                screenOff("2026-07-28T09:15:00+09:00"),
            ),
        )

        assertEquals(15, measured[youtube])
    }

    @Test
    fun leavingTheForegroundEndsTheVisitWithoutWaitingForAnotherApplication() {
        val measured = minutes(
            listOf(
                foreground("com.google.android.youtube", "2026-07-28T09:00:00+09:00"),
                leftForeground("com.google.android.youtube", "2026-07-28T09:10:00+09:00"),
            ),
        )

        assertEquals(10, measured[youtube])
    }

    @Test
    fun anotherApplicationLeavingTheForegroundDoesNotEndThisVisit() {
        val measured = minutes(
            listOf(
                foreground("com.google.android.youtube", "2026-07-28T09:00:00+09:00"),
                leftForeground("com.android.chrome", "2026-07-28T09:10:00+09:00"),
            ),
        )

        assertEquals(60, measured[youtube])
    }

    @Test
    fun aVisitStillRunningIsCountedUpToNow() {
        val measured = minutes(listOf(foreground("com.google.android.youtube", "2026-07-28T09:40:00+09:00")))

        assertEquals(20, measured[youtube])
    }

    @Test
    fun aVisitThatStartedYesterdayCountsOnlyFromLocalMidnight() {
        val measured = minutes(
            listOf(
                foreground("com.google.android.youtube", "2026-07-27T23:00:00+09:00"),
                foreground("com.android.chrome", "2026-07-28T00:30:00+09:00"),
            ),
        )

        assertEquals(30, measured[youtube])
    }

    @Test
    fun timeWithTheDeviceSwitchedOffIsNotCountedAsTimeInTheApplication() {
        val measured = minutes(
            listOf(foreground("com.google.android.youtube", "2026-07-27T23:50:00+09:00")),
            bootedAt = at("2026-07-28T09:30:00+09:00"),
        )

        assertEquals(30, measured[youtube])
    }

    @Test
    fun aVisitInterruptedByAShutdownEndsAtTheShutdownRatherThanAtTheNextUnlock() {
        val measured = minutes(
            listOf(
                foreground("com.google.android.youtube", "2026-07-27T23:50:00+09:00"),
                foreground("com.android.chrome", "2026-07-28T09:35:00+09:00"),
            ),
            bootedAt = at("2026-07-28T09:30:00+09:00"),
        )

        assertEquals(5, measured[youtube])
    }

    @Test
    fun repeatedEntriesIntoTheSameApplicationAreOneVisit() {
        val measured = minutes(
            listOf(
                foreground("com.google.android.youtube", "2026-07-28T09:00:00+09:00"),
                foreground("com.google.android.youtube", "2026-07-28T09:05:00+09:00"),
                foreground("com.android.chrome", "2026-07-28T09:30:00+09:00"),
            ),
        )

        assertEquals(30, measured[youtube])
    }

    @Test
    fun anApplicationOutsideTheCatalogIsNotMeasured() {
        val measured = minutes(
            listOf(
                foreground("com.android.chrome", "2026-07-28T09:00:00+09:00"),
                foreground("com.google.android.youtube", "2026-07-28T09:30:00+09:00"),
            ),
        )

        assertEquals(setOf(youtube), measured.keys)
    }

    @Test
    fun nothingRecordedMeansNothingMeasured() {
        assertNull(minutes(emptyList())[youtube])
    }
}
