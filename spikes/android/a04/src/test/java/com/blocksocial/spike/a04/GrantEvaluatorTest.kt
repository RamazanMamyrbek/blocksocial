package com.blocksocial.spike.a04

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GrantEvaluatorTest {

    private val youtube = "com.google.android.youtube"
    private val oneMinute = 60_000L

    private val grantedAt = DeviceTime(
        wallClockMillis = 1_700_000_000_000L,
        elapsedRealtimeMillis = 5_000_000L
    )

    private val grant = GrantEvaluator.newGrant(youtube, oneMinute, grantedAt)

    private fun laterBy(wallMillis: Long, elapsedMillis: Long) = DeviceTime(
        wallClockMillis = grantedAt.wallClockMillis + wallMillis,
        elapsedRealtimeMillis = grantedAt.elapsedRealtimeMillis + elapsedMillis
    )

    @Test
    fun `a fresh grant is active`() {
        assertEquals(GrantEvaluation.ACTIVE, GrantEvaluator.evaluate(grant, grantedAt))
    }

    @Test
    fun `a grant is active one millisecond before expiry`() {
        val now = laterBy(oneMinute - 1, oneMinute - 1)

        assertEquals(GrantEvaluation.ACTIVE, GrantEvaluator.evaluate(grant, now))
    }

    @Test
    fun `a grant is expired exactly at expiry`() {
        val now = laterBy(oneMinute, oneMinute)

        assertEquals(GrantEvaluation.EXPIRED, GrantEvaluator.evaluate(grant, now))
    }

    @Test
    fun `a grant is expired after expiry`() {
        val now = laterBy(oneMinute * 10, oneMinute * 10)

        assertEquals(GrantEvaluation.EXPIRED, GrantEvaluator.evaluate(grant, now))
    }

    @Test
    fun `moving the clock backwards does not extend a grant`() {
        val halfway = laterBy(wallMillis = -oneMinute * 60, elapsedMillis = oneMinute / 2)
        assertEquals(GrantEvaluation.ACTIVE, GrantEvaluator.evaluate(grant, halfway))

        val afterExpiry = laterBy(wallMillis = -oneMinute * 60, elapsedMillis = oneMinute)
        assertEquals(GrantEvaluation.EXPIRED, GrantEvaluator.evaluate(grant, afterExpiry))
    }

    @Test
    fun `moving the clock backwards by a year does not extend a grant`() {
        val now = laterBy(wallMillis = -365L * 24 * 60 * 60 * 1000, elapsedMillis = oneMinute)

        assertEquals(GrantEvaluation.EXPIRED, GrantEvaluator.evaluate(grant, now))
    }

    @Test
    fun `moving the clock forwards does not shorten a grant`() {
        val now = laterBy(wallMillis = oneMinute * 60, elapsedMillis = oneMinute / 2)

        assertEquals(GrantEvaluation.ACTIVE, GrantEvaluator.evaluate(grant, now))
    }

    @Test
    fun `after a reboot the remaining wall clock time is honoured`() {
        val now = DeviceTime(
            wallClockMillis = grantedAt.wallClockMillis + oneMinute / 2,
            elapsedRealtimeMillis = 4_000L
        )

        assertEquals(GrantEvaluation.ACTIVE_AFTER_REBOOT, GrantEvaluator.evaluate(grant, now))
    }

    @Test
    fun `after a reboot a grant whose wall clock window passed is expired`() {
        val now = DeviceTime(
            wallClockMillis = grantedAt.wallClockMillis + oneMinute * 5,
            elapsedRealtimeMillis = 4_000L
        )

        assertEquals(GrantEvaluation.EXPIRED_AFTER_REBOOT, GrantEvaluator.evaluate(grant, now))
    }

    @Test
    fun `after a reboot a clock set before the grant discards it`() {
        val now = DeviceTime(
            wallClockMillis = grantedAt.wallClockMillis - 1,
            elapsedRealtimeMillis = 4_000L
        )

        assertEquals(
            GrantEvaluation.DISCARDED_CLOCK_MOVED_BEFORE_GRANT,
            GrantEvaluator.evaluate(grant, now)
        )
    }

    @Test
    fun `only active evaluations suppress the block`() {
        assertTrue(GrantEvaluation.ACTIVE.suppressesBlock)
        assertTrue(GrantEvaluation.ACTIVE_AFTER_REBOOT.suppressesBlock)
        assertFalse(GrantEvaluation.EXPIRED.suppressesBlock)
        assertFalse(GrantEvaluation.EXPIRED_AFTER_REBOOT.suppressesBlock)
        assertFalse(GrantEvaluation.DISCARDED_CLOCK_MOVED_BEFORE_GRANT.suppressesBlock)
    }

    @Test
    fun `a grant stores absolute expiry on both clocks`() {
        assertEquals(grantedAt.wallClockMillis + oneMinute, grant.expiresAtWallClockMillis)
        assertEquals(
            grantedAt.elapsedRealtimeMillis + oneMinute,
            grant.expiresAtElapsedRealtimeMillis
        )
    }
}
