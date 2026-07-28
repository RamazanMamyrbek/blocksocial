package com.blocksocial.spike.a05

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class LocalDayWindowsTest {

    private val almaty = ZoneId.of("Asia/Almaty")
    private val berlin = ZoneId.of("Europe/Berlin")
    private val utc = ZoneId.of("UTC")

    private fun localMidnight(date: String, zone: ZoneId) =
        LocalDate.parse(date).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun localTime(date: String, hour: Int, zone: ZoneId) =
        LocalDate.parse(date).atStartOfDay(zone).plusHours(hour.toLong()).toInstant().toEpochMilli()

    @Test
    fun `a window starts at local midnight and ends at the next one`() {
        val noon = localTime("2026-07-28", 12, almaty)
        val window = LocalDayWindows.containing(noon, almaty)

        assertEquals(localMidnight("2026-07-28", almaty), window.startMillis)
        assertEquals(localMidnight("2026-07-29", almaty), window.endMillis)
    }

    @Test
    fun `the window start reads back as local midnight in that zone`() {
        val window = LocalDayWindows.containing(localTime("2026-07-28", 12, almaty), almaty)
        val asLocal = Instant.ofEpochMilli(window.startMillis).atZone(almaty).toLocalTime()

        assertEquals(0, asLocal.hour)
        assertEquals(0, asLocal.minute)
        assertEquals(0, asLocal.second)
    }

    @Test
    fun `the instant of local midnight belongs to the day that begins`() {
        val midnight = localMidnight("2026-07-28", almaty)
        val window = LocalDayWindows.containing(midnight, almaty)

        assertEquals(midnight, window.startMillis)
    }

    @Test
    fun `the last millisecond of a day belongs to that day`() {
        val lastMillis = localMidnight("2026-07-29", almaty) - 1
        val window = LocalDayWindows.containing(lastMillis, almaty)

        assertEquals(localMidnight("2026-07-28", almaty), window.startMillis)
        assertEquals(lastMillis + 1, window.endMillis)
    }

    @Test
    fun `crossing local midnight moves to the next window`() {
        val before = localMidnight("2026-07-29", almaty) - 1
        val after = before + 1

        val previous = LocalDayWindows.containing(before, almaty)
        val next = LocalDayWindows.containing(after, almaty)

        assertEquals(previous.endMillis, next.startMillis)
        assertTrue(next.startMillis > previous.startMillis)
    }

    @Test
    fun `an ordinary day is twenty four hours long`() {
        val window = LocalDayWindows.containing(localTime("2026-07-28", 12, almaty), almaty)

        assertEquals(24 * 60 * 60 * 1000L, window.lengthMillis)
    }

    @Test
    fun `a spring forward day is twenty three hours long`() {
        val window = LocalDayWindows.containing(localTime("2026-03-29", 12, berlin), berlin)

        assertEquals(23 * 60 * 60 * 1000L, window.lengthMillis)
    }

    @Test
    fun `an autumn back day is twenty five hours long`() {
        val window = LocalDayWindows.containing(localTime("2026-10-25", 12, berlin), berlin)

        assertEquals(25 * 60 * 60 * 1000L, window.lengthMillis)
    }

    @Test
    fun `the same instant can belong to different days in different zones`() {
        val instant = localTime("2026-07-28", 20, utc)

        val inAlmaty = LocalDayWindows.containing(instant, almaty)
        val inUtc = LocalDayWindows.containing(instant, utc)

        assertNotEquals(inUtc.startMillis, inAlmaty.startMillis)
    }
}
