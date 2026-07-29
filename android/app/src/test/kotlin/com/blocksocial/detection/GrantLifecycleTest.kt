package com.blocksocial.detection

import com.blocksocial.core.domain.BypassEvaluator
import com.blocksocial.core.domain.GrantEvaluation
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.BypassPolicy
import com.blocksocial.core.model.DeviceTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class GrantLifecycleTest {

    private val instagram = AppRef("instagram")
    private val tiktok = AppRef("tiktok")
    private val zone = ZoneId.of("Asia/Tokyo")

    private fun at(wallClock: String, monotonicMillis: Long) = DeviceTime(
        wallClock = Instant.parse(wallClock),
        monotonicMillis = monotonicMillis,
        zone = zone,
    )

    private val grantedAt = at("2026-07-27T20:00:00Z", 5_000_000)

    @Test
    fun aGrantRecordsBothClocksAndTheGrantedDuration() {
        val grant = BypassPolicy.Android.grant(instagram, grantedAt)

        assertEquals(instagram, grant.forApp)
        assertEquals(grantedAt.wallClock, grant.grantedAtWallClock)
        assertEquals(grantedAt.monotonicMillis, grant.grantedAtMonotonicMillis)
        assertEquals(BypassPolicy.Android.grantDurationMinutes, grant.durationMinutes)
    }

    @Test
    fun theExpiryIsStoredRatherThanCountedDownInMemory() {
        val grant = BypassPolicy.Android.grant(instagram, grantedAt)

        val justBefore = BypassEvaluator.evaluate(grant, instagram, at("2026-07-27T20:04:59Z", 5_299_000))
        val exactlyAt = BypassEvaluator.evaluate(grant, instagram, at("2026-07-27T20:05:00Z", 5_300_000))

        assertTrue(justBefore.suppressesBlock)
        assertFalse(exactlyAt.suppressesBlock)
        assertEquals(GrantEvaluation.EXPIRED, exactlyAt.evaluation)
    }

    @Test
    fun aGrantNeverLeaksToAnotherApplication() {
        val grant = BypassPolicy.Android.grant(instagram, grantedAt)

        val other = BypassEvaluator.evaluate(grant, tiktok, at("2026-07-27T20:01:00Z", 5_060_000))

        assertFalse(other.suppressesBlock)
        assertEquals(null, other.evaluation)
    }

    @Test
    fun movingTheClockBackwardsDoesNotExtendAGrant() {
        val grant = BypassPolicy.Android.grant(instagram, grantedAt)

        val clockMovedBack = BypassEvaluator.evaluate(grant, instagram, at("2026-07-27T19:06:40Z", 5_400_000))

        assertFalse(clockMovedBack.suppressesBlock)
        assertEquals(GrantEvaluation.EXPIRED, clockMovedBack.evaluation)
    }

    @Test
    fun aGrantSurvivesARebootInsideItsWindow() {
        val grant = BypassPolicy.Android.grant(instagram, grantedAt)

        val afterReboot = BypassEvaluator.evaluate(grant, instagram, at("2026-07-27T20:02:00Z", 3_000))

        assertTrue(afterReboot.suppressesBlock)
        assertEquals(GrantEvaluation.ACTIVE_AFTER_REBOOT, afterReboot.evaluation)
    }

    @Test
    fun onlySpentEvaluationsCauseTheStoredGrantToBeRemoved() {
        val spent = setOf(
            GrantEvaluation.EXPIRED,
            GrantEvaluation.EXPIRED_AFTER_REBOOT,
            GrantEvaluation.DISCARDED_CLOCK_MOVED_BEFORE_GRANT,
        )
        val kept = setOf(GrantEvaluation.ACTIVE, GrantEvaluation.ACTIVE_AFTER_REBOOT)

        assertEquals(GrantEvaluation.entries.toSet(), spent + kept)
    }
}
