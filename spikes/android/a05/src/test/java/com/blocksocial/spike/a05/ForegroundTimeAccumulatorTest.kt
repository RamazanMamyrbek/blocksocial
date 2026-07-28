package com.blocksocial.spike.a05

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundTimeAccumulatorTest {

    private val youtube = "com.google.android.youtube"
    private val chrome = "com.android.chrome"
    private val launcher = "com.google.android.apps.nexuslauncher"

    private val windowStart = 1_700_000_000_000L
    private val windowEnd = windowStart + 24 * 60 * 60 * 1000L

    private fun resumed(packageName: String, atMillis: Long) =
        UsageEvent(packageName, atMillis, UsageEventType.MOVED_TO_FOREGROUND)

    private fun screenOff(atMillis: Long) =
        UsageEvent("android", atMillis, UsageEventType.SCREEN_NON_INTERACTIVE)

    @Test
    fun `no events means no usage`() {
        assertTrue(ForegroundTimeAccumulator.accumulate(emptyList(), windowStart, windowEnd).isEmpty())
    }

    @Test
    fun `a session ends when another application is resumed`() {
        val usage = ForegroundTimeAccumulator.accumulate(
            listOf(
                resumed(youtube, windowStart + 10_000),
                resumed(launcher, windowStart + 310_000)
            ),
            windowStart,
            windowEnd
        )

        assertEquals(300_000L, usage.getValue(youtube).totalMillis)
        assertEquals(UsageConfidence.MEASURED, usage.getValue(youtube).confidence)
        assertEquals(1, usage.getValue(youtube).sessionCount)
    }

    @Test
    fun `an application moving between its own activities keeps one session`() {
        val usage = ForegroundTimeAccumulator.accumulate(
            listOf(
                resumed(chrome, windowStart + 1_000),
                resumed(chrome, windowStart + 1_300),
                resumed(chrome, windowStart + 2_200),
                resumed(launcher, windowStart + 41_000)
            ),
            windowStart,
            windowEnd
        )

        assertEquals(40_000L, usage.getValue(chrome).totalMillis)
        assertEquals(1, usage.getValue(chrome).sessionCount)
    }

    @Test
    fun `switching between applications never double counts`() {
        val usage = ForegroundTimeAccumulator.accumulate(
            listOf(
                resumed(youtube, windowStart),
                resumed(chrome, windowStart + 10_000),
                resumed(launcher, windowStart + 25_000)
            ),
            windowStart,
            windowEnd
        )

        assertEquals(10_000L, usage.getValue(youtube).totalMillis)
        assertEquals(15_000L, usage.getValue(chrome).totalMillis)
        assertEquals(UsageConfidence.IN_PROGRESS, usage.getValue(launcher).confidence)
        assertEquals(
            25_000L,
            usage.filterKeys { it != launcher }.values.sumOf { it.totalMillis }
        )
    }

    @Test
    fun `several separate visits are summed and counted`() {
        val usage = ForegroundTimeAccumulator.accumulate(
            listOf(
                resumed(youtube, windowStart + 1_000),
                resumed(launcher, windowStart + 61_000),
                resumed(youtube, windowStart + 120_000),
                resumed(launcher, windowStart + 150_000)
            ),
            windowStart,
            windowEnd
        )

        assertEquals(90_000L, usage.getValue(youtube).totalMillis)
        assertEquals(2, usage.getValue(youtube).sessionCount)
    }

    @Test
    fun `a session still open at the window end is in progress`() {
        val usage = ForegroundTimeAccumulator.accumulate(
            listOf(resumed(youtube, windowEnd - 45_000)),
            windowStart,
            windowEnd
        )

        assertEquals(45_000L, usage.getValue(youtube).totalMillis)
        assertEquals(UsageConfidence.IN_PROGRESS, usage.getValue(youtube).confidence)
    }

    @Test
    fun `a session that began before the window is counted from the window start`() {
        val usage = ForegroundTimeAccumulator.accumulate(
            listOf(
                resumed(youtube, windowStart - 600_000),
                resumed(launcher, windowStart + 30_000)
            ),
            windowStart,
            windowEnd
        )

        assertEquals(30_000L, usage.getValue(youtube).totalMillis)
    }

    @Test
    fun `screen off ends the session and the dark time is not counted`() {
        val usage = ForegroundTimeAccumulator.accumulate(
            listOf(
                resumed(youtube, windowStart + 10_000),
                screenOff(windowStart + 70_000),
                resumed(youtube, windowStart + 600_000),
                resumed(launcher, windowStart + 630_000)
            ),
            windowStart,
            windowEnd
        )

        assertEquals(90_000L, usage.getValue(youtube).totalMillis)
        assertEquals(2, usage.getValue(youtube).sessionCount)
    }

    @Test
    fun `screen off with nothing open changes nothing`() {
        val usage = ForegroundTimeAccumulator.accumulate(
            listOf(screenOff(windowStart + 5_000)),
            windowStart,
            windowEnd
        )

        assertTrue(usage.isEmpty())
    }

    @Test
    fun `events after the window end are ignored`() {
        val usage = ForegroundTimeAccumulator.accumulate(
            listOf(
                resumed(youtube, windowEnd + 1_000),
                resumed(launcher, windowEnd + 5_000)
            ),
            windowStart,
            windowEnd
        )

        assertTrue(usage.isEmpty())
    }

    @Test
    fun `unordered events are handled`() {
        val usage = ForegroundTimeAccumulator.accumulate(
            listOf(
                resumed(launcher, windowStart + 60_000),
                resumed(youtube, windowStart + 10_000)
            ),
            windowStart,
            windowEnd
        )

        assertEquals(50_000L, usage.getValue(youtube).totalMillis)
    }

    @Test
    fun `no total is ever negative`() {
        val usage = ForegroundTimeAccumulator.accumulate(
            listOf(
                resumed(youtube, windowStart - 100_000),
                resumed(launcher, windowStart - 50_000),
                resumed(chrome, windowStart + 1_000)
            ),
            windowStart,
            windowEnd
        )

        assertTrue(usage.values.all { it.totalMillis >= 0 })
    }
}
