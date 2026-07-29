package com.blocksocial.core.domain

import com.blocksocial.core.domain.fixtures.FixtureCase
import com.blocksocial.core.domain.fixtures.FixtureCorpus
import com.blocksocial.core.domain.fixtures.FixtureParser
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.RuleMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class RulePriorityFixtureTest(private val case: FixtureCase) {

    @Test
    fun matchesTheSharedContract() {
        val given = case.json.getJSONObject("given")
        val whenJson = case.json.getJSONObject("when")
        val expect = case.json.getJSONObject("expect")

        val decision = RuleEvaluator.evaluate(
            rules = FixtureParser.rules(given.getJSONArray("rules")),
            grant = FixtureParser.grant(given.optJSONObject("grant")),
            usageSessions = FixtureParser.sessions(given.optJSONArray("usageSessions")),
            forApp = AppRef(whenJson.getString("forApp")),
            at = FixtureParser.deviceTime(whenJson),
        )

        assertEquals(case.id, expect.getBoolean("restrictionActive"), decision.restrictionActive)

        val expectedPrimary = expect.optString("primaryReason").takeIf { it.isNotEmpty() }
        if (expectedPrimary == null) {
            assertNull(case.id, decision.primaryReason)
        } else {
            assertEquals(case.id, RuleMode.valueOf(expectedPrimary), decision.primaryReason)
        }

        val expectedAll = expect.getJSONArray("allReasons")
        val expectedReasons = (0 until expectedAll.length()).map { RuleMode.valueOf(expectedAll.getString(it)) }
        assertEquals(case.id, expectedReasons, decision.allReasons)
    }

    companion object {
        const val FILE = "rule-priority-cases.json"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<FixtureCase> = FixtureCorpus.cases(FILE)
    }
}
