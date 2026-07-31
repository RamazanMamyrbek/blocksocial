package com.blocksocial.usage

import com.blocksocial.core.domain.DailyLimitEvaluator
import com.blocksocial.core.domain.MeasurementState
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.DeviceTime
import com.blocksocial.core.model.RestrictionRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ForegroundSessionsTest {

    private val youtube = AppRef("youtube")
    private val zone = ZoneId.of("Asia/Tokyo")
    private val packageToApp = mapOf("com.google.android.youtube" to youtube)

    private val dayStart = Instant.parse("2026-07-28T00:00:00+09:00").toEpochMilli()
    private val now = Instant.parse("2026-07-28T10:00:00+09:00").toEpochMilli()
    private val bootedYesterday = Instant.parse("2026-07-27T08:00:00+09:00").toEpochMilli()

    private fun at(text: String) = Instant.parse(text).toEpochMilli()

    private fun foreground(packageName: String, text: String) =
        UsageEvent(packageName, at(text), UsageEventType.MOVED_TO_FOREGROUND)

    private fun leftForeground(packageName: String, text: String) =
        UsageEvent(packageName, at(text), UsageEventType.LEFT_FOREGROUND)

    private fun screenOff(text: String) =
        UsageEvent("android", at(text), UsageEventType.SCREEN_NON_INTERACTIVE)

    private fun accumulate(events: List<UsageEvent>, bootedAt: Long = bootedYesterday) =
        ForegroundSessions.accumulate(events, packageToApp, dayStart, now, bootedAt)

    private fun minutesOf(sessions: List<com.blocksocial.core.model.UsageSession>): Int =
        DailyLimitEvaluator.evaluate(
            rule = RestrictionRule.DailyLimit("r", enabled = true, limitMinutes = 30),
            sessions = sessions,
            forApp = youtube,
            at = DeviceTime(Instant.ofEpochMilli(now), 0, zone),
            usageMeasurementAvailable = true,
        ).measuredMinutesToday ?: -1

    @Test
    fun aClosedSessionIsMeasuredFromEntryToTheNextForegroundChange() {
        val sessions = accumulate(
            listOf(
                foreground("com.google.android.youtube", "2026-07-28T09:00:00+09:00"),
                foreground("com.android.chrome", "2026-07-28T09:20:00+09:00"),
            ),
        )

        assertEquals(20, minutesOf(sessions.getValue(youtube)))
        assertEquals(1, sessions.getValue(youtube).size)
    }

    @Test
    fun theScreenGoingOffClosesTheSession() {
        val sessions = accumulate(
            listOf(
                foreground("com.google.android.youtube", "2026-07-28T09:00:00+09:00"),
                screenOff("2026-07-28T09:15:00+09:00"),
            ),
        )

        assertEquals(15, minutesOf(sessions.getValue(youtube)))
    }

    @Test
    fun aSessionStillRunningIsOpenEndedRatherThanClosedAtNow() {
        val sessions = accumulate(
            listOf(foreground("com.google.android.youtube", "2026-07-28T09:40:00+09:00")),
        )

        assertNull(sessions.getValue(youtube).single().to)
        assertEquals(20, minutesOf(sessions.getValue(youtube)))
    }

    @Test
    fun aSessionThatStartedYesterdayIsCountedOnlyFromLocalMidnight() {
        val sessions = accumulate(
            listOf(
                foreground("com.google.android.youtube", "2026-07-27T23:00:00+09:00"),
                foreground("com.android.chrome", "2026-07-28T00:30:00+09:00"),
            ),
        )

        assertEquals(30, minutesOf(sessions.getValue(youtube)))
    }

    @Test
    fun repeatedForegroundEventsForTheSameApplicationAreOneSession() {
        val sessions = accumulate(
            listOf(
                foreground("com.google.android.youtube", "2026-07-28T09:00:00+09:00"),
                foreground("com.google.android.youtube", "2026-07-28T09:05:00+09:00"),
                foreground("com.android.chrome", "2026-07-28T09:30:00+09:00"),
            ),
        )

        assertEquals(1, sessions.getValue(youtube).size)
        assertEquals(30, minutesOf(sessions.getValue(youtube)))
    }

    @Test
    fun anApplicationOutsideTheSelectionIsNotAccumulated() {
        val sessions = accumulate(
            listOf(
                foreground("com.android.chrome", "2026-07-28T09:00:00+09:00"),
                foreground("com.google.android.youtube", "2026-07-28T09:30:00+09:00"),
            ),
        )

        assertEquals(setOf(youtube), sessions.keys)
    }

    @Test
    fun leavingTheForegroundClosesTheSessionWithoutWaitingForAnotherApplication() {
        val sessions = accumulate(
            listOf(
                foreground("com.google.android.youtube", "2026-07-28T09:00:00+09:00"),
                leftForeground("com.google.android.youtube", "2026-07-28T09:10:00+09:00"),
            ),
        )

        assertEquals(10, minutesOf(sessions.getValue(youtube)))
    }

    @Test
    fun anotherApplicationLeavingTheForegroundDoesNotCloseThisSession() {
        val sessions = accumulate(
            listOf(
                foreground("com.google.android.youtube", "2026-07-28T09:00:00+09:00"),
                leftForeground("com.android.chrome", "2026-07-28T09:10:00+09:00"),
            ),
        )

        assertEquals(60, minutesOf(sessions.getValue(youtube)))
    }

    @Test
    fun timeWithTheDeviceSwitchedOffIsNotCountedAsTimeInTheApplication() {
        val sessions = accumulate(
            listOf(foreground("com.google.android.youtube", "2026-07-27T23:50:00+09:00")),
            bootedAt = at("2026-07-28T09:30:00+09:00"),
        )

        assertEquals(30, minutesOf(sessions[youtube].orEmpty()))
    }

    @Test
    fun aVisitInterruptedByAShutdownEndsAtTheShutdownRatherThanAtTheNextUnlock() {
        val sessions = accumulate(
            listOf(
                foreground("com.google.android.youtube", "2026-07-27T23:50:00+09:00"),
                foreground("com.android.chrome", "2026-07-28T09:35:00+09:00"),
            ),
            bootedAt = at("2026-07-28T09:30:00+09:00"),
        )

        assertEquals(5, minutesOf(sessions[youtube].orEmpty()))
    }

    @Test
    fun theRecognisedEventTypesAreTheOnesThatOpenOrCloseAVisit() {
        assertEquals(
            setOf(
                UsageEventType.MOVED_TO_FOREGROUND,
                UsageEventType.LEFT_FOREGROUND,
                UsageEventType.SCREEN_NON_INTERACTIVE,
            ),
            UsageEventType.entries.toSet(),
        )
    }

    @Test
    fun anEmptyResultIsToleratedRatherThanTreatedAsZeroUsage() {
        val sessions = accumulate(emptyList())

        assertTrue(sessions.isEmpty())

        val decision = DailyLimitEvaluator.evaluate(
            rule = RestrictionRule.DailyLimit("r", enabled = true, limitMinutes = 30),
            sessions = emptyList(),
            forApp = youtube,
            at = DeviceTime(Instant.ofEpochMilli(now), 0, zone),
            usageMeasurementAvailable = false,
        )

        assertNull(decision.measuredMinutesToday)
        assertFalse(decision.restrictionActive)
        assertEquals(MeasurementState.UNAVAILABLE, decision.measurementState)
    }
}
