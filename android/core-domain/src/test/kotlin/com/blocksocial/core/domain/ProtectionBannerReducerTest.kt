package com.blocksocial.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ProtectionBannerReducerTest {

    @Test
    fun aWorkingServiceReadsAsRunning() {
        assertEquals(
            ProtectionBanner.RUNNING,
            ProtectionBannerReducer.reduce(RequirementStatus.HEALTHY, enabledWhenLastSeen = true),
        )
    }

    @Test
    fun aServiceThatWasNeverAskedForIsNotReportedAsStopped() {
        assertEquals(
            ProtectionBanner.NEVER_SET_UP,
            ProtectionBannerReducer.reduce(RequirementStatus.NOT_ASKED, enabledWhenLastSeen = false),
        )
    }

    @Test
    fun everyBrokenStateIsCalledOutAsAChangeWhenItWasWorkingLastTime() {
        listOf(
            RequirementStatus.DENIED,
            RequirementStatus.ENABLED_BUT_NOT_RUNNING,
            RequirementStatus.RUNNING_BUT_SILENT,
        ).forEach { status ->
            assertEquals(
                "$status should be reported as a change",
                ProtectionBanner.STOPPED_SINCE_LAST_OPEN,
                ProtectionBannerReducer.reduce(status, enabledWhenLastSeen = true),
            )
        }
    }

    @Test
    fun aStateThatWasAlreadyBrokenIsNotReportedAsANewChange() {
        listOf(
            RequirementStatus.DENIED,
            RequirementStatus.ENABLED_BUT_NOT_RUNNING,
            RequirementStatus.RUNNING_BUT_SILENT,
        ).forEach { status ->
            assertEquals(
                "$status should not be reported as a change",
                ProtectionBanner.STOPPED,
                ProtectionBannerReducer.reduce(status, enabledWhenLastSeen = false),
            )
        }
    }

    @Test
    fun onlyAccessibilityDecidesTheBanner() {
        val items = listOf(
            RequirementHealth(ProtectionRequirement.ACCESSIBILITY_SERVICE, RequirementStatus.HEALTHY),
            RequirementHealth(ProtectionRequirement.USAGE_ACCESS, RequirementStatus.DENIED),
            RequirementHealth(ProtectionRequirement.NOTIFICATIONS, RequirementStatus.DENIED),
        )

        assertEquals(
            ProtectionBanner.RUNNING,
            ProtectionBannerReducer.reduce(items, enabledWhenLastSeen = true),
        )
    }

    @Test
    fun anEmptyReadingClaimsNothing() {
        assertEquals(
            ProtectionBanner.NEVER_SET_UP,
            ProtectionBannerReducer.reduce(emptyList(), enabledWhenLastSeen = true),
        )
    }
}
