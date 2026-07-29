package com.blocksocial.core.domain.fixtures

import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.DeviceTime
import com.blocksocial.core.model.RestrictionRule
import com.blocksocial.core.model.TemporaryAccessGrant
import com.blocksocial.core.model.UsageSession
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId

object FixtureParser {

    fun instant(text: String): Instant = OffsetDateTime.parse(text).toInstant()

    fun deviceTime(whenJson: JSONObject): DeviceTime = DeviceTime(
        wallClock = instant(whenJson.getString("instant")),
        monotonicMillis = whenJson.optLong("monotonicMillis", 0L),
        zone = ZoneId.of(whenJson.getString("zone")),
    )

    fun rule(json: JSONObject, fallbackId: String): RestrictionRule {
        val id = json.optString("id", fallbackId)
        val enabled = json.getBoolean("enabled")
        return when (val mode = json.getString("mode")) {
            "ALWAYS" -> RestrictionRule.AlwaysOn(id, enabled)
            "FOCUS_SESSION" -> RestrictionRule.FocusSession(id, enabled)
            "SCHEDULE" -> RestrictionRule.Schedule(
                id = id,
                enabled = enabled,
                daysOfWeek = daysOfWeek(json.getJSONArray("daysOfWeek")),
                startLocalTime = LocalTime.parse(json.getString("startLocalTime")),
                endLocalTime = LocalTime.parse(json.getString("endLocalTime")),
            )
            "DAILY_LIMIT" -> RestrictionRule.DailyLimit(
                id = id,
                enabled = enabled,
                limitMinutes = json.getInt("dailyLimitMinutes"),
            )
            else -> throw IllegalArgumentException("Unknown rule mode $mode")
        }
    }

    fun rules(array: JSONArray): List<RestrictionRule> =
        (0 until array.length()).map { rule(array.getJSONObject(it), "rule-$it") }

    fun grant(json: JSONObject?): TemporaryAccessGrant? = json?.let {
        TemporaryAccessGrant(
            forApp = AppRef(it.getString("forApp")),
            grantedAtWallClock = instant(it.getString("grantedAtWallClock")),
            grantedAtMonotonicMillis = it.getLong("grantedAtMonotonicMillis"),
            durationMinutes = it.getInt("durationMinutes"),
        )
    }

    fun sessions(array: JSONArray?): List<UsageSession> {
        if (array == null) return emptyList()
        return (0 until array.length()).map { index ->
            val session = array.getJSONObject(index)
            UsageSession(
                forApp = AppRef(session.getString("forApp")),
                from = instant(session.getString("from")),
                to = session.optString("to").takeIf { it.isNotEmpty() }?.let(::instant),
            )
        }
    }

    private fun daysOfWeek(array: JSONArray): Set<DayOfWeek> =
        (0 until array.length()).map { DayOfWeek.valueOf(array.getString(it)) }.toSet()
}
