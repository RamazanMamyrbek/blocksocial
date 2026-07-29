package com.blocksocial.core.domain

import com.blocksocial.core.domain.fixtures.FixtureParser
import com.blocksocial.core.model.AppRef
import org.junit.Assert.assertEquals
import org.junit.Test

class EvaluationPurityTest {

    @Test
    fun repeatedEvaluationOfTheSameInputsGivesTheSameResult() {
        RulePriorityFixtureTest.cases().forEach { case ->
            val given = case.json.getJSONObject("given")
            val whenJson = case.json.getJSONObject("when")

            val decisions = (1..3).map {
                RuleEvaluator.evaluate(
                    rules = FixtureParser.rules(given.getJSONArray("rules")),
                    grant = FixtureParser.grant(given.optJSONObject("grant")),
                    usageSessions = FixtureParser.sessions(given.optJSONArray("usageSessions")),
                    forApp = AppRef(whenJson.getString("forApp")),
                    at = FixtureParser.deviceTime(whenJson),
                )
            }

            assertEquals(case.id, 1, decisions.toSet().size)
        }
    }
}
