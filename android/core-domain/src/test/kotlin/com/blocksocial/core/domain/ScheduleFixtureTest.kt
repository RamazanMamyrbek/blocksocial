package com.blocksocial.core.domain

import com.blocksocial.core.domain.fixtures.FixtureCase
import com.blocksocial.core.domain.fixtures.FixtureCorpus
import com.blocksocial.core.domain.fixtures.FixtureParser
import com.blocksocial.core.model.AppRef
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ScheduleFixtureTest(private val case: FixtureCase) {

    @Test
    fun matchesTheSharedContract() {
        val given = case.json.getJSONObject("given")
        val rule = FixtureParser.rule(given.getJSONObject("rule"), case.id)
        val at = FixtureParser.deviceTime(case.json.getJSONObject("when"))

        val decision = RuleEvaluator.evaluate(
            rules = listOf(rule),
            grant = null,
            usageSessions = emptyList(),
            forApp = AppRef("app-a"),
            at = at,
        )

        assertEquals(
            case.id,
            case.json.getJSONObject("expect").getBoolean("restrictionActive"),
            decision.restrictionActive,
        )
    }

    companion object {
        const val FILE = "schedule-cases.json"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<FixtureCase> = FixtureCorpus.cases(FILE)
    }
}
