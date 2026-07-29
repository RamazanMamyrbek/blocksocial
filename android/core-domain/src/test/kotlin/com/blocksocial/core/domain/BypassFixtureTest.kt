package com.blocksocial.core.domain

import com.blocksocial.core.domain.fixtures.FixtureCase
import com.blocksocial.core.domain.fixtures.FixtureCorpus
import com.blocksocial.core.domain.fixtures.FixtureParser
import com.blocksocial.core.model.AppRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class BypassFixtureTest(private val case: FixtureCase) {

    @Test
    fun matchesTheSharedContract() {
        val given = case.json.getJSONObject("given")
        val whenJson = case.json.getJSONObject("when")
        val expect = case.json.getJSONObject("expect")

        val decision = BypassEvaluator.evaluate(
            grant = FixtureParser.grant(given.optJSONObject("grant")),
            forApp = AppRef(whenJson.getString("forApp")),
            at = FixtureParser.deviceTime(whenJson),
        )

        assertEquals(case.id, expect.getBoolean("suppressesBlock"), decision.suppressesBlock)

        val expectedEvaluation = expect.optString("evaluation").takeIf { it.isNotEmpty() }
        if (expectedEvaluation == null) {
            assertNull(case.id, decision.evaluation)
        } else {
            assertEquals(case.id, GrantEvaluation.valueOf(expectedEvaluation), decision.evaluation)
        }
    }

    companion object {
        const val FILE = "bypass-cases.json"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<FixtureCase> = FixtureCorpus.cases(FILE)
    }
}
