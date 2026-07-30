package com.blocksocial.core.data

import com.blocksocial.core.data.mapper.toDomain
import com.blocksocial.core.data.mapper.toEntity
import com.blocksocial.core.domain.RuleEvaluator
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.DeviceTime
import com.blocksocial.core.model.RestrictionRule
import com.blocksocial.core.model.RuleMode
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId

class StoredRuleFixtureTest {

    private val app = AppRef("app-a")
    private val storedAt = Instant.parse("2026-07-01T00:00:00Z")

    private val cases: List<JSONObject> = run {
        val file = generateSequence(File(".").absoluteFile) { it.parentFile }
            .map { File(it, "shared/fixtures/schedule-cases.json") }
            .first { it.isFile }
        val array = JSONObject(file.readText()).getJSONArray("cases")
        (0 until array.length()).map { array.getJSONObject(it) }
    }

    private fun ruleOf(json: JSONObject, id: String): RestrictionRule {
        val enabled = json.getBoolean("enabled")
        return when (json.getString("mode")) {
            "ALWAYS" -> RestrictionRule.AlwaysOn(id, enabled)
            "SCHEDULE" -> {
                val days = json.getJSONArray("daysOfWeek")
                RestrictionRule.Schedule(
                    id = id,
                    enabled = enabled,
                    daysOfWeek = (0 until days.length()).map { DayOfWeek.valueOf(days.getString(it)) }.toSet(),
                    startLocalTime = LocalTime.parse(json.getString("startLocalTime")),
                    endLocalTime = LocalTime.parse(json.getString("endLocalTime")),
                )
            }
            else -> throw IllegalArgumentException("unexpected mode in schedule corpus")
        }
    }

    @Test
    fun everyScheduleCaseStillHoldsAfterTheRuleHasBeenThroughStorage() {
        assertTrue(cases.isNotEmpty())

        cases.forEach { case ->
            val id = case.getString("id")
            val original = ruleOf(case.getJSONObject("given").getJSONObject("rule"), id)

            val restored = original.toEntity(app, storedAt, storedAt).toDomain()

            val whenJson = case.getJSONObject("when")
            val decision = RuleEvaluator.evaluate(
                rules = listOf(restored),
                grant = null,
                usageSessions = emptyList(),
                forApp = app,
                at = DeviceTime(
                    wallClock = OffsetDateTime.parse(whenJson.getString("instant")).toInstant(),
                    monotonicMillis = 0,
                    zone = ZoneId.of(whenJson.getString("zone")),
                ),
            )

            assertEquals(id, original, restored)
            assertEquals(
                id,
                case.getJSONObject("expect").getBoolean("restrictionActive"),
                decision.restrictionActive,
            )
        }
    }

    @Test
    fun theCorpusStillCoversBothModesTheEditorCanProduce() {
        val modes = cases.map { it.getJSONObject("given").getJSONObject("rule").getString("mode") }.toSet()

        assertEquals(setOf(RuleMode.ALWAYS.name, RuleMode.SCHEDULE.name), modes)
    }
}
