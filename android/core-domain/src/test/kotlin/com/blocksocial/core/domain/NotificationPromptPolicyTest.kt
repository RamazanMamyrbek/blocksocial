package com.blocksocial.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPromptPolicyTest {

    private fun need(
        notifications: RequirementStatus = RequirementStatus.NOT_ASKED,
        accessibility: RequirementStatus = RequirementStatus.HEALTHY,
        setupFinished: Boolean = true,
        alreadyAsked: Boolean = false,
    ) = NotificationNeed(notifications, accessibility, setupFinished, alreadyAsked)

    @Test
    fun nothingIsAskedWhileProtectionIsWorking() {
        assertEquals(
            NotificationAsk.NOT_NEEDED,
            NotificationPromptPolicy.evaluate(need(accessibility = RequirementStatus.HEALTHY)),
        )
    }

    @Test
    fun theRequestArrivesTheFirstTimeThereIsSomethingToReport() {
        listOf(
            RequirementStatus.DENIED,
            RequirementStatus.ENABLED_BUT_NOT_RUNNING,
            RequirementStatus.RUNNING_BUT_SILENT,
        ).forEach { broken ->
            assertEquals(
                "protection is $broken and nobody would be told",
                NotificationAsk.ASK_NOW,
                NotificationPromptPolicy.evaluate(need(accessibility = broken)),
            )
        }
    }

    @Test
    fun nothingIsAskedDuringSetup() {
        assertEquals(
            NotificationAsk.NOT_NEEDED,
            NotificationPromptPolicy.evaluate(
                need(accessibility = RequirementStatus.DENIED, setupFinished = false),
            ),
        )
    }

    @Test
    fun aProtectionThatWasNeverSetUpIsNotAFailureToReport() {
        assertEquals(
            NotificationAsk.NOT_NEEDED,
            NotificationPromptPolicy.evaluate(need(accessibility = RequirementStatus.NOT_ASKED)),
        )
    }

    @Test
    fun theRequestIsNeverRepeated() {
        assertEquals(
            NotificationAsk.NOT_NEEDED,
            NotificationPromptPolicy.evaluate(
                need(accessibility = RequirementStatus.DENIED, alreadyAsked = true),
            ),
        )
    }

    @Test
    fun anAlreadyGrantedPermissionIsNotAskedForAgain() {
        assertEquals(
            NotificationAsk.NOT_NEEDED,
            NotificationPromptPolicy.evaluate(
                need(
                    notifications = RequirementStatus.HEALTHY,
                    accessibility = RequirementStatus.DENIED,
                ),
            ),
        )
    }
}
