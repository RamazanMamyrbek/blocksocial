package com.blocksocial.core.domain

enum class NotificationAsk { NOT_NEEDED, ASK_NOW }

data class NotificationNeed(
    val notifications: RequirementStatus,
    val blockingRuns: Boolean,
    val alreadyAsked: Boolean,
)

object NotificationPromptPolicy {

    fun evaluate(need: NotificationNeed): NotificationAsk = when {
        need.notifications == RequirementStatus.HEALTHY -> NotificationAsk.NOT_NEEDED
        need.alreadyAsked -> NotificationAsk.NOT_NEEDED
        need.blockingRuns -> NotificationAsk.NOT_NEEDED
        else -> NotificationAsk.ASK_NOW
    }
}
