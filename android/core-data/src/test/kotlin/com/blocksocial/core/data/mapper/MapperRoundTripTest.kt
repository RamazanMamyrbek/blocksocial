package com.blocksocial.core.data.mapper

import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.BlockEvent
import com.blocksocial.core.model.Platform
import com.blocksocial.core.model.RestrictedApp
import com.blocksocial.core.model.RestrictionRule
import com.blocksocial.core.model.RuleMode
import com.blocksocial.core.model.TemporaryAccessGrant
import com.blocksocial.core.model.UsageSession
import com.blocksocial.core.model.UserAction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

class MapperRoundTripTest {

    private val app = AppRef("instagram")
    private val createdAt = Instant.parse("2026-07-27T10:00:00Z")
    private val updatedAt = Instant.parse("2026-07-27T11:00:00Z")

    @Test
    fun aRestrictedAppSurvivesTheRoundTrip() {
        val original = RestrictedApp(ref = app, displayName = "Instagram", selected = true)
        assertEquals(original, original.toEntity(createdAt).toDomain())
    }

    @Test
    fun anAlwaysOnRuleSurvivesTheRoundTrip() {
        val original = RestrictionRule.AlwaysOn(id = "r1", enabled = true)
        assertEquals(original, original.toEntity(app, createdAt, updatedAt).toDomain())
    }

    @Test
    fun aFocusSessionRuleSurvivesTheRoundTrip() {
        val original = RestrictionRule.FocusSession(id = "r2", enabled = false)
        assertEquals(original, original.toEntity(app, createdAt, updatedAt).toDomain())
    }

    @Test
    fun anOvernightScheduleRuleSurvivesTheRoundTrip() {
        val original = RestrictionRule.Schedule(
            id = "r3",
            enabled = true,
            daysOfWeek = setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY),
            startLocalTime = LocalTime.of(22, 0),
            endLocalTime = LocalTime.of(7, 0),
        )
        assertEquals(original, original.toEntity(app, createdAt, updatedAt).toDomain())
    }

    @Test
    fun aDailyLimitRuleSurvivesTheRoundTrip() {
        val original = RestrictionRule.DailyLimit(id = "r4", enabled = true, limitMinutes = 30)
        assertEquals(original, original.toEntity(app, createdAt, updatedAt).toDomain())
    }

    @Test
    fun theStoredRuleKeepsOnlyTheColumnsItsModeUses() {
        val alwaysOn = RestrictionRule.AlwaysOn(id = "r1", enabled = true)
            .toEntity(app, createdAt, updatedAt)
        assertEquals(null, alwaysOn.daysOfWeek)
        assertEquals(null, alwaysOn.startLocalTime)
        assertEquals(null, alwaysOn.endLocalTime)
        assertEquals(null, alwaysOn.dailyLimitMinutes)

        val dailyLimit = RestrictionRule.DailyLimit(id = "r4", enabled = true, limitMinutes = 30)
            .toEntity(app, createdAt, updatedAt)
        assertEquals(null, dailyLimit.daysOfWeek)
        assertEquals(30, dailyLimit.dailyLimitMinutes)
    }

    @Test
    fun aGrantSurvivesTheRoundTripOnBothClocks() {
        val original = TemporaryAccessGrant(
            forApp = app,
            grantedAtWallClock = Instant.parse("2026-07-27T20:00:00Z"),
            grantedAtMonotonicMillis = 5_000_000,
            durationMinutes = 5,
        )
        assertEquals(original, original.toEntity().toDomain())
    }

    @Test
    fun aBlockEventSurvivesTheRoundTrip() {
        val original = BlockEvent(
            id = "event-1",
            restrictedAppRef = app,
            occurredAt = Instant.parse("2026-07-27T20:00:00Z"),
            zone = ZoneId.of("Asia/Tokyo"),
            primaryReason = RuleMode.ALWAYS,
            allReasons = listOf(RuleMode.ALWAYS, RuleMode.SCHEDULE),
            userAction = UserAction.BYPASSED,
            bypassDurationMinutes = 5,
            platform = Platform.ANDROID,
        )
        assertEquals(original, original.toEntity().toDomain())
    }

    @Test
    fun aRunningUsageSessionSurvivesTheRoundTrip() {
        val original = UsageSession(
            forApp = app,
            from = Instant.parse("2026-07-27T09:00:00Z"),
            to = null,
        )
        assertEquals(original, original.toEntity().toDomain())
    }

    @Test
    fun aFinishedUsageSessionSurvivesTheRoundTrip() {
        val original = UsageSession(
            forApp = app,
            from = Instant.parse("2026-07-27T09:00:00Z"),
            to = Instant.parse("2026-07-27T09:20:00Z"),
        )
        assertEquals(original, original.toEntity().toDomain())
    }
}
