package com.blocksocial.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ProtectionHealthReducerTest {

    private val probeStartedAt = Instant.parse("2026-07-30T10:00:00Z")
    private val afterGrace = probeStartedAt.plus(ProtectionHealthReducer.PROBE_GRACE).plusMillis(1)

    private fun observation(
        enabledInSettings: Boolean = true,
        everAsked: Boolean = true,
        connectedInThisProcess: Boolean = true,
        connectedAt: Instant? = this.probeStartedAt.minusSeconds(60),
        lastEventAt: Instant? = null,
        probeStartedAt: Instant? = this.probeStartedAt,
        now: Instant = afterGrace,
    ) = AccessibilityObservation(
        enabledInSettings = enabledInSettings,
        everAsked = everAsked,
        connectedInThisProcess = connectedInThisProcess,
        connectedAt = connectedAt,
        lastEventAt = lastEventAt,
        probeStartedAt = probeStartedAt,
        now = now,
    )

    @Test
    fun aServiceThatAnswersTheProbeIsHealthy() {
        val status = ProtectionHealthReducer.accessibility(
            observation(lastEventAt = probeStartedAt.plusSeconds(1)),
        )

        assertEquals(RequirementStatus.HEALTHY, status)
    }

    @Test
    fun forceStopLooksLikeEnabledInSettingsButNotRunning() {
        val status = ProtectionHealthReducer.accessibility(
            observation(enabledInSettings = true, connectedInThisProcess = false),
        )

        assertEquals(RequirementStatus.ENABLED_BUT_NOT_RUNNING, status)
    }

    @Test
    fun aConnectedServiceThatDeliversNothingIsReportedBrokenRatherThanHealthy() {
        val status = ProtectionHealthReducer.accessibility(
            observation(connectedInThisProcess = true, lastEventAt = null),
        )

        assertEquals(RequirementStatus.RUNNING_BUT_SILENT, status)
    }

    @Test
    fun anEventFromBeforeTheProbeDoesNotCountAsAnAnswer() {
        val status = ProtectionHealthReducer.accessibility(
            observation(lastEventAt = probeStartedAt.minusSeconds(30)),
        )

        assertEquals(RequirementStatus.RUNNING_BUT_SILENT, status)
    }

    @Test
    fun theProbeIsGivenTimeToAnswerBeforeAnythingIsCalledBroken() {
        val status = ProtectionHealthReducer.accessibility(
            observation(lastEventAt = null, now = probeStartedAt.plusSeconds(1)),
        )

        assertEquals(RequirementStatus.HEALTHY, status)
    }

    @Test
    fun withNoProbeRunningAConnectedServiceIsNotAccused() {
        val status = ProtectionHealthReducer.accessibility(
            observation(probeStartedAt = null, lastEventAt = null),
        )

        assertEquals(RequirementStatus.HEALTHY, status)
    }

    @Test
    fun aServiceThatRebindsDuringTheProbeIsNotAccusedOfSilence() {
        val status = ProtectionHealthReducer.accessibility(
            observation(connectedAt = probeStartedAt.plusSeconds(1), lastEventAt = null),
        )

        assertEquals(RequirementStatus.HEALTHY, status)
    }

    @Test
    fun aRebindDoesNotExcuseSilenceOnceTheNextProbeHasRun() {
        val rebindAt = probeStartedAt.plusSeconds(1)
        val status = ProtectionHealthReducer.accessibility(
            observation(
                connectedAt = rebindAt,
                lastEventAt = null,
                probeStartedAt = rebindAt.plusSeconds(1),
                now = rebindAt.plusSeconds(1).plus(ProtectionHealthReducer.PROBE_GRACE).plusMillis(1),
            ),
        )

        assertEquals(RequirementStatus.RUNNING_BUT_SILENT, status)
    }

    @Test
    fun neverAskedIsDistinctFromDenied() {
        assertEquals(
            RequirementStatus.NOT_ASKED,
            ProtectionHealthReducer.accessibility(observation(enabledInSettings = false, everAsked = false)),
        )
        assertEquals(
            RequirementStatus.DENIED,
            ProtectionHealthReducer.accessibility(observation(enabledInSettings = false, everAsked = true)),
        )
        assertEquals(RequirementStatus.NOT_ASKED, ProtectionHealthReducer.granted(false, everAsked = false))
        assertEquals(RequirementStatus.DENIED, ProtectionHealthReducer.granted(false, everAsked = true))
        assertEquals(RequirementStatus.HEALTHY, ProtectionHealthReducer.granted(true, everAsked = true))
    }

    @Test
    fun onlyTheAccessibilityServiceCanStopBlockingAltogether() {
        val silentService = RequirementHealth(
            ProtectionRequirement.ACCESSIBILITY_SERVICE,
            RequirementStatus.RUNNING_BUT_SILENT,
        )
        val deniedUsage = RequirementHealth(
            ProtectionRequirement.USAGE_ACCESS,
            RequirementStatus.DENIED,
        )

        assertTrue(silentService.blocksProtection)
        assertFalse(deniedUsage.blocksProtection)
        assertFalse(ProtectionHealthReducer.blockingRuns(listOf(silentService, deniedUsage)))
        assertTrue(ProtectionHealthReducer.blockingRuns(listOf(deniedUsage)))
    }

    @Test
    fun everyStatusIsAccountedForWhenDecidingWhetherBlockingRuns() {
        RequirementStatus.entries.forEach { status ->
            val service = RequirementHealth(ProtectionRequirement.ACCESSIBILITY_SERVICE, status)
            assertEquals(status != RequirementStatus.HEALTHY, service.blocksProtection)
        }
    }
}
