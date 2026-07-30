package com.blocksocial.core.domain

enum class NotificationAsk { NOT_NEEDED, ASK_NOW }

data class NotificationNeed(
    val notifications: RequirementStatus,
    val accessibility: RequirementStatus,
    val setupFinished: Boolean,
    val alreadyAsked: Boolean,
)

object NotificationPromptPolicy {

    fun evaluate(need: NotificationNeed): NotificationAsk = when {
        need.notifications == RequirementStatus.HEALTHY -> NotificationAsk.NOT_NEEDED
        need.alreadyAsked -> NotificationAsk.NOT_NEEDED
        !need.setupFinished -> NotificationAsk.NOT_NEEDED
        need.accessibility == RequirementStatus.HEALTHY -> NotificationAsk.NOT_NEEDED
        need.accessibility == RequirementStatus.NOT_ASKED -> NotificationAsk.NOT_NEEDED
        else -> NotificationAsk.ASK_NOW
    }
}
