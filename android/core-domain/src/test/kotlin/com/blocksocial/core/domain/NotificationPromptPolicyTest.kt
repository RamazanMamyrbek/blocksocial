package com.blocksocial.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPromptPolicyTest {

    private fun need(
        notifications: RequirementStatus = RequirementStatus.NOT_ASKED,
        blockingRuns: Boolean = true,
        alreadyAsked: Boolean = false,
    ) = NotificationNeed(notifications, blockingRuns, alreadyAsked)

    @Test
    fun nothingIsAskedWhileProtectionIsWorking() {
        assertEquals(
            NotificationAsk.NOT_NEEDED,
            NotificationPromptPolicy.evaluate(need(blockingRuns = true)),
        )
    }

    @Test
    fun theRequestArrivesTheFirstTimeThereIsSomethingToReport() {
        assertEquals(
            NotificationAsk.ASK_NOW,
            NotificationPromptPolicy.evaluate(need(blockingRuns = false)),
        )
    }

    @Test
    fun theRequestIsNeverRepeated() {
        assertEquals(
            NotificationAsk.NOT_NEEDED,
            NotificationPromptPolicy.evaluate(need(blockingRuns = false, alreadyAsked = true)),
        )
    }

    @Test
    fun anAlreadyGrantedPermissionIsNotAskedForAgain() {
        assertEquals(
            NotificationAsk.NOT_NEEDED,
            NotificationPromptPolicy.evaluate(
                need(notifications = RequirementStatus.HEALTHY, blockingRuns = false),
            ),
        )
    }
}
