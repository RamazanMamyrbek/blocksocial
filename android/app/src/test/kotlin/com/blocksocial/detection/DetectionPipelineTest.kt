package com.blocksocial.detection

import com.blocksocial.core.domain.GrantEvaluation
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.DeviceTime
import com.blocksocial.core.model.RestrictionRule
import com.blocksocial.core.model.RuleMode
import com.blocksocial.core.model.TemporaryAccessGrant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class DetectionPipelineTest {

    private val instagram = AppRef("instagram")
    private val now = Instant.parse("2026-07-27T10:30:00Z")

    private fun pipeline(activityWindows: Boolean = true) = DetectionPipeline(
        selfPackage = "com.blocksocial",
        systemPackages = setOf("com.android.settings", "com.android.launcher"),
        isActivityWindow = { _, _ -> activityWindows },
        readDeviceTime = {
            DeviceTime(wallClock = now, monotonicMillis = 5_000_000, zone = ZoneId.of("Asia/Tokyo"))
        },
    )

    private fun snapshot(
        rules: List<RestrictionRule> = emptyList(),
        grant: TemporaryAccessGrant? = null,
    ) = ProtectionSnapshot(
        packageToApp = mapOf("com.instagram.android" to instagram),
        rulesByApp = mapOf(instagram to rules),
        grantsByApp = grant?.let { mapOf(instagram to it) }.orEmpty(),
    )

    @Test
    fun anAlwaysOnRuleBlocks() {
        val result = pipeline().onWindowStateChanged(
            packageName = "com.instagram.android",
            className = "com.instagram.MainActivity",
            snapshot = snapshot(rules = listOf(RestrictionRule.AlwaysOn("r1", enabled = true))),
        )

        assertTrue(result.shouldBlock)
        assertEquals(instagram, result.app)
        assertEquals(RuleMode.ALWAYS, result.decision?.primaryReason)
    }

    @Test
    fun anApplicationWithNoRuleIsNotBlocked() {
        val result = pipeline().onWindowStateChanged(
            packageName = "com.instagram.android",
            className = "com.instagram.MainActivity",
            snapshot = snapshot(rules = emptyList()),
        )

        assertEquals(TransitionOutcome.TARGET_ENTERED, result.transition)
        assertFalse(result.shouldBlock)
    }

    @Test
    fun anActiveGrantSuppressesTheBlock() {
        val grant = TemporaryAccessGrant(
            forApp = instagram,
            grantedAtWallClock = now,
            grantedAtMonotonicMillis = 4_900_000,
            durationMinutes = 5,
        )

        val result = pipeline().onWindowStateChanged(
            packageName = "com.instagram.android",
            className = "com.instagram.MainActivity",
            snapshot = snapshot(
                rules = listOf(RestrictionRule.AlwaysOn("r1", enabled = true)),
                grant = grant,
            ),
        )

        assertFalse(result.shouldBlock)
        assertEquals(GrantEvaluation.ACTIVE, result.decision?.bypass?.evaluation)
        assertEquals(emptyList<RuleMode>(), result.decision?.allReasons)
    }

    @Test
    fun anExpiredGrantRestoresTheBlock() {
        val grant = TemporaryAccessGrant(
            forApp = instagram,
            grantedAtWallClock = now,
            grantedAtMonotonicMillis = 4_000_000,
            durationMinutes = 5,
        )

        val result = pipeline().onWindowStateChanged(
            packageName = "com.instagram.android",
            className = "com.instagram.MainActivity",
            snapshot = snapshot(
                rules = listOf(RestrictionRule.AlwaysOn("r1", enabled = true)),
                grant = grant,
            ),
        )

        assertTrue(result.shouldBlock)
        assertEquals(GrantEvaluation.EXPIRED, result.decision?.bypass?.evaluation)
    }

    @Test
    fun aSystemApplicationIsNeverEvaluated() {
        val result = pipeline().onWindowStateChanged(
            packageName = "com.android.settings",
            className = "com.android.settings.Settings",
            snapshot = snapshot(rules = listOf(RestrictionRule.AlwaysOn("r1", enabled = true))),
        )

        assertEquals(TransitionOutcome.IGNORED_SYSTEM, result.transition)
        assertNull(result.decision)
        assertFalse(result.shouldBlock)
    }

    @Test
    fun aDailyLimitRuleCannotFireBeforeUsageMeasurementExists() {
        val result = pipeline().onWindowStateChanged(
            packageName = "com.instagram.android",
            className = "com.instagram.MainActivity",
            snapshot = snapshot(
                rules = listOf(RestrictionRule.DailyLimit("r1", enabled = true, limitMinutes = 0)),
            ),
        )

        assertFalse(result.shouldBlock)
    }

    @Test
    fun theSecondWindowOfTheSameVisitIsNotEvaluatedAgain() {
        val pipeline = pipeline()
        val snapshot = snapshot(rules = listOf(RestrictionRule.AlwaysOn("r1", enabled = true)))

        pipeline.onWindowStateChanged("com.instagram.android", "A", snapshot)
        val second = pipeline.onWindowStateChanged("com.instagram.android", "B", snapshot)

        assertEquals(TransitionOutcome.IGNORED_ALREADY_FOREGROUND, second.transition)
        assertNull(second.decision)
    }
}
