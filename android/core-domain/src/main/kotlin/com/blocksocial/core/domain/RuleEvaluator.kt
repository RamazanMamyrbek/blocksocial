package com.blocksocial.core.domain

import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.DeviceTime
import com.blocksocial.core.model.RestrictionRule
import com.blocksocial.core.model.RuleMode
import com.blocksocial.core.model.TemporaryAccessGrant
import com.blocksocial.core.model.UsageSession

data class RestrictionDecision(
    val restrictionActive: Boolean,
    val primaryReason: RuleMode?,
    val allReasons: List<RuleMode>,
    val bypass: BypassDecision,
    val dailyLimit: DailyLimitDecision? = null,
    val dailyLimitMinutes: Int? = null,
)

object RuleEvaluator {

    fun evaluate(
        rules: List<RestrictionRule>,
        grant: TemporaryAccessGrant?,
        usageSessions: List<UsageSession>,
        forApp: AppRef,
        at: DeviceTime,
        usageMeasurementAvailable: Boolean = true,
    ): RestrictionDecision {
        val bypass = BypassEvaluator.evaluate(grant, forApp, at)
        if (bypass.suppressesBlock) {
            return RestrictionDecision(
                restrictionActive = false,
                primaryReason = null,
                allReasons = emptyList(),
                bypass = bypass,
            )
        }

        val tightestLimit = rules
            .filterIsInstance<RestrictionRule.DailyLimit>()
            .filter { it.enabled }
            .minByOrNull { it.limitMinutes }

        val limitDecision = tightestLimit?.let {
            DailyLimitEvaluator.evaluate(
                rule = it,
                sessions = usageSessions,
                forApp = forApp,
                at = at,
                usageMeasurementAvailable = usageMeasurementAvailable,
            )
        }

        val reasons = rules
            .filter { applies(it, usageSessions, forApp, at, usageMeasurementAvailable) }
            .map { it.mode }
            .distinct()
            .sortedBy { it.priority }

        return RestrictionDecision(
            restrictionActive = reasons.isNotEmpty(),
            primaryReason = reasons.firstOrNull(),
            allReasons = reasons,
            bypass = bypass,
            dailyLimit = limitDecision,
            dailyLimitMinutes = tightestLimit?.limitMinutes,
        )
    }

    private fun applies(
        rule: RestrictionRule,
        usageSessions: List<UsageSession>,
        forApp: AppRef,
        at: DeviceTime,
        usageMeasurementAvailable: Boolean,
    ): Boolean = when (rule) {
        is RestrictionRule.AlwaysOn -> rule.enabled
        is RestrictionRule.FocusSession -> rule.enabled
        is RestrictionRule.Schedule -> ScheduleEvaluator.isActive(rule, at)
        is RestrictionRule.DailyLimit -> DailyLimitEvaluator.evaluate(
            rule = rule,
            sessions = usageSessions,
            forApp = forApp,
            at = at,
            usageMeasurementAvailable = usageMeasurementAvailable,
        ).restrictionActive
    }
}
