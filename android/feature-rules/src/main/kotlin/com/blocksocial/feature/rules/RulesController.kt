package com.blocksocial.feature.rules

import com.blocksocial.core.data.repository.RestrictionRuleRepository
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.RestrictionRule
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RulesController @Inject constructor(private val rules: RestrictionRuleRepository) {

    fun rulesFor(app: AppRef): Flow<List<RestrictionRule>> = rules.observeRulesFor(app)

    fun newDraft(mode: com.blocksocial.core.model.RuleMode): RuleDraft =
        RuleDraft(id = UUID.randomUUID().toString(), mode = mode)

    suspend fun save(draft: RuleDraft, forApp: AppRef) {
        val rule = draft.toRule() ?: return
        val now = Instant.now()
        rules.save(rule, forApp, now, now)
    }

    suspend fun setEnabled(rule: RestrictionRule, forApp: AppRef, enabled: Boolean) {
        val now = Instant.now()
        rules.save(RuleDraft.of(rule).copy(enabled = enabled).toRule() ?: return, forApp, now, now)
    }

    suspend fun delete(ruleId: String) = rules.delete(ruleId)
}
