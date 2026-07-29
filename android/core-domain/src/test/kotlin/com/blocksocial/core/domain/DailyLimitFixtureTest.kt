package com.blocksocial.core.domain

import com.blocksocial.core.domain.fixtures.FixtureCase
import com.blocksocial.core.domain.fixtures.FixtureCorpus
import com.blocksocial.core.domain.fixtures.FixtureParser
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.RestrictionRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.abs

@RunWith(Parameterized::class)
class DailyLimitFixtureTest(private val case: FixtureCase) {

    @Test
    fun matchesTheSharedContract() {
        val given = case.json.getJSONObject("given")
        val whenJson = case.json.getJSONObject("when")
        val expect = case.json.getJSONObject("expect")

        val decision = DailyLimitEvaluator.evaluate(
            rule = FixtureParser.rule(given.getJSONObject("rule"), case.id) as RestrictionRule.DailyLimit,
            sessions = FixtureParser.sessions(given.optJSONArray("usageSessions")),
            forApp = AppRef(whenJson.getString("forApp")),
            at = FixtureParser.deviceTime(whenJson),
            usageMeasurementAvailable = given.optBoolean("usageMeasurementAvailable", true),
        )

        if (expect.isNull("measuredMinutesToday")) {
            assertNull(case.id, decision.measuredMinutesToday)
        } else {
            val expected = expect.getInt("measuredMinutesToday")
            val measured = decision.measuredMinutesToday
            assertTrue("${case.id}: measured $measured, expected $expected", measured != null)
            assertTrue(
                "${case.id}: measured $measured is outside the ${ACCEPTABLE_ERROR_PERCENT}% tolerance around $expected",
                abs(measured!! - expected) <= expected * ACCEPTABLE_ERROR_PERCENT / 100.0,
            )
        }

        assertEquals(case.id, expect.getBoolean("restrictionActive"), decision.restrictionActive)
        assertEquals(
            case.id,
            MeasurementState.valueOf(expect.getString("measurementState")),
            decision.measurementState,
        )

        if (expect.has("blockPresentedImmediately")) {
            assertEquals(
                case.id,
                expect.getBoolean("blockPresentedImmediately"),
                decision.blockPresentedImmediately,
            )
        }
    }

    companion object {
        const val FILE = "daily-limit-cases.json"

        val ACCEPTABLE_ERROR_PERCENT: Int = FixtureCorpus.envelope(FILE)
            .getJSONObject("measurementPolicy")
            .getInt("acceptableMeasurementErrorPercent")

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<FixtureCase> = FixtureCorpus.cases(FILE)
    }
}
