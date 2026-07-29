package com.blocksocial.core.domain

import com.blocksocial.core.domain.fixtures.FixtureCorpus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FixtureCoverageTest {

    private val executed = mapOf(
        ScheduleFixtureTest.FILE to ScheduleFixtureTest.cases(),
        BypassFixtureTest.FILE to BypassFixtureTest.cases(),
        DailyLimitFixtureTest.FILE to DailyLimitFixtureTest.cases(),
        RulePriorityFixtureTest.FILE to RulePriorityFixtureTest.cases(),
    )

    @Test
    fun everyCaseInEveryFileIsExecuted() {
        executed.forEach { (fileName, cases) ->
            val declared = FixtureCorpus.envelope(fileName).getJSONArray("cases").length()
            assertEquals("$fileName is not fully executed", declared, cases.size)
        }
    }

    @Test
    fun theCorpusSizeIsWhatPhaseSevenFroze() {
        assertEquals(58, executed.values.sumOf { it.size })
    }

    @Test
    fun everyFileDeclaresTheSameContractVersion() {
        executed.keys.forEach { fileName ->
            assertEquals(
                fileName,
                FixtureCorpus.CONTRACT_VERSION,
                FixtureCorpus.envelope(fileName).getInt("contractVersion"),
            )
        }
    }

    @Test
    fun noCaseIdentifierIsUsedTwice() {
        val identifiers = executed.values.flatten().map { it.id }
        assertEquals(identifiers.size, identifiers.toSet().size)
    }

    @Test
    fun theReservedFocusSessionCaseIsExecutedRatherThanSkipped() {
        val reserved = RulePriorityFixtureTest.cases().single {
            it.id == "priority-reserved-focus-session-slot"
        }
        assertTrue(reserved.json.has("mvpApplicable"))
        assertEquals(false, reserved.json.getBoolean("mvpApplicable"))
    }
}
