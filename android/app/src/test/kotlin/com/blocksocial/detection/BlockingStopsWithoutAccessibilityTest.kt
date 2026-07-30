package com.blocksocial.detection

import com.blocksocial.core.domain.ProtectionBanner
import com.blocksocial.core.domain.ProtectionBannerReducer
import com.blocksocial.core.domain.ProtectionHealthReducer
import com.blocksocial.core.domain.ProtectionRequirement
import com.blocksocial.core.domain.RequirementHealth
import com.blocksocial.core.domain.RequirementStatus
import com.blocksocial.core.domain.RuleEvaluator
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.DeviceTime
import com.blocksocial.core.model.RestrictionRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class BlockingStopsWithoutAccessibilityTest {

    private val savedRule = RestrictionRule.AlwaysOn(id = "always-instagram", enabled = true)

    private val snapshot = ProtectionSnapshot(
        packageToApp = mapOf(PACKAGE to APP),
        rulesByApp = mapOf(APP to listOf(savedRule)),
        grantsByApp = emptyMap(),
    )

    private fun health(accessibility: RequirementStatus) = listOf(
        RequirementHealth(ProtectionRequirement.ACCESSIBILITY_SERVICE, accessibility),
        RequirementHealth(ProtectionRequirement.USAGE_ACCESS, RequirementStatus.HEALTHY),
        RequirementHealth(ProtectionRequirement.NOTIFICATIONS, RequirementStatus.HEALTHY),
    )

    @Test
    fun theSavedRuleStillDecidesToBlockWhenTheServiceIsRunning() {
        val pipeline = DetectionPipeline(
            selfPackage = "com.blocksocial",
            systemPackages = emptySet(),
            isActivityWindow = { _, _ -> true },
            readDeviceTime = ::deviceTime,
        )

        val result = pipeline.onWindowStateChanged(PACKAGE, "com.instagram.MainActivity", snapshot)

        assertTrue("the saved rule is not being applied", result.shouldBlock)
    }

    @Test
    fun nothingIsBlockedWhileAccessibilityIsRefused() {
        listOf(
            RequirementStatus.NOT_ASKED,
            RequirementStatus.DENIED,
            RequirementStatus.ENABLED_BUT_NOT_RUNNING,
            RequirementStatus.RUNNING_BUT_SILENT,
        ).forEach { status ->
            assertFalse(
                "blocking must not be claimed while accessibility is $status",
                ProtectionHealthReducer.blockingRuns(health(status)),
            )
        }
    }

    @Test
    fun theRuleIsUntouchedByTheMissingPermission() {
        val decision = RuleEvaluator.evaluate(
            rules = snapshot.rulesByApp.getValue(APP),
            grant = null,
            usageSessions = emptyList(),
            forApp = APP,
            at = deviceTime(),
            usageMeasurementAvailable = false,
        )

        assertEquals(listOf(savedRule), snapshot.rulesByApp.getValue(APP))
        assertTrue("the rule stays enabled", snapshot.rulesByApp.getValue(APP).single().enabled)
        assertTrue("the rule stays live in the data", decision.restrictionActive)
    }

    @Test
    fun theDashboardSaysBlockingStoppedRatherThanThatTheRulesAreGone() {
        assertEquals(
            ProtectionBanner.STOPPED_SINCE_LAST_OPEN,
            ProtectionBannerReducer.reduce(health(RequirementStatus.DENIED), enabledWhenLastSeen = true),
        )
    }

    private fun deviceTime() = DeviceTime(
        wallClock = Instant.parse("2026-07-30T12:00:00Z"),
        monotonicMillis = 1_000_000,
        zone = ZoneId.of("Europe/Berlin"),
    )

    private companion object {
        const val PACKAGE = "com.instagram.android"
        val APP = AppRef("instagram")
    }
}
