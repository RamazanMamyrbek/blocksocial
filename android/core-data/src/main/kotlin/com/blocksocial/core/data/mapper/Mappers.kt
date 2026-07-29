package com.blocksocial.core.data.mapper

import com.blocksocial.core.data.database.entity.BlockEventEntity
import com.blocksocial.core.data.database.entity.RestrictedAppEntity
import com.blocksocial.core.data.database.entity.RestrictionRuleEntity
import com.blocksocial.core.data.database.entity.TemporaryAccessGrantEntity
import com.blocksocial.core.data.database.entity.UsageSessionEntity
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.BlockEvent
import com.blocksocial.core.model.Platform
import com.blocksocial.core.model.RestrictedApp
import com.blocksocial.core.model.RestrictionRule
import com.blocksocial.core.model.RuleMode
import com.blocksocial.core.model.TemporaryAccessGrant
import com.blocksocial.core.model.UsageSession
import com.blocksocial.core.model.UserAction
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

fun RestrictedAppEntity.toDomain(): RestrictedApp = RestrictedApp(
    ref = AppRef(catalogId),
    displayName = displayName,
    selected = selected,
)

fun RestrictedApp.toEntity(createdAt: Instant): RestrictedAppEntity = RestrictedAppEntity(
    catalogId = ref.value,
    displayName = displayName,
    selected = selected,
    createdAtEpochMillis = createdAt.toEpochMilli(),
)

fun RestrictionRuleEntity.toDomain(): RestrictionRule = when (RuleMode.valueOf(mode)) {
    RuleMode.ALWAYS -> RestrictionRule.AlwaysOn(id = id, enabled = enabled)
    RuleMode.FOCUS_SESSION -> RestrictionRule.FocusSession(id = id, enabled = enabled)
    RuleMode.SCHEDULE -> RestrictionRule.Schedule(
        id = id,
        enabled = enabled,
        daysOfWeek = requireNotNull(daysOfWeek) { "schedule rule $id has no days" }
            .split(",")
            .filter { it.isNotBlank() }
            .map { DayOfWeek.valueOf(it) }
            .toSet(),
        startLocalTime = LocalTime.parse(requireNotNull(startLocalTime) { "schedule rule $id has no start" }),
        endLocalTime = LocalTime.parse(requireNotNull(endLocalTime) { "schedule rule $id has no end" }),
    )
    RuleMode.DAILY_LIMIT -> RestrictionRule.DailyLimit(
        id = id,
        enabled = enabled,
        limitMinutes = requireNotNull(dailyLimitMinutes) { "daily limit rule $id has no limit" },
    )
}

fun RestrictionRule.toEntity(
    forApp: AppRef,
    createdAt: Instant,
    updatedAt: Instant,
): RestrictionRuleEntity = RestrictionRuleEntity(
    id = id,
    appCatalogId = forApp.value,
    mode = mode.name,
    enabled = enabled,
    daysOfWeek = (this as? RestrictionRule.Schedule)
        ?.daysOfWeek
        ?.sortedBy { it.value }
        ?.joinToString(",") { it.name },
    startLocalTime = (this as? RestrictionRule.Schedule)?.startLocalTime?.toString(),
    endLocalTime = (this as? RestrictionRule.Schedule)?.endLocalTime?.toString(),
    dailyLimitMinutes = (this as? RestrictionRule.DailyLimit)?.limitMinutes,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
)

fun TemporaryAccessGrantEntity.toDomain(): TemporaryAccessGrant = TemporaryAccessGrant(
    forApp = AppRef(appCatalogId),
    grantedAtWallClock = Instant.ofEpochMilli(grantedAtWallClockEpochMillis),
    grantedAtMonotonicMillis = grantedAtMonotonicMillis,
    durationMinutes = durationMinutes,
)

fun TemporaryAccessGrant.toEntity(sourceBlockEventId: String? = null): TemporaryAccessGrantEntity =
    TemporaryAccessGrantEntity(
        appCatalogId = forApp.value,
        grantedAtWallClockEpochMillis = grantedAtWallClock.toEpochMilli(),
        grantedAtMonotonicMillis = grantedAtMonotonicMillis,
        durationMinutes = durationMinutes,
        sourceBlockEventId = sourceBlockEventId,
    )

fun BlockEventEntity.toDomain(): BlockEvent = BlockEvent(
    id = id,
    restrictedAppRef = AppRef(appCatalogId),
    occurredAt = Instant.ofEpochMilli(occurredAtEpochMillis),
    zone = ZoneId.of(zoneId),
    primaryReason = RuleMode.valueOf(primaryReason),
    allReasons = allReasons.split(",").filter { it.isNotBlank() }.map { RuleMode.valueOf(it) },
    userAction = UserAction.valueOf(userAction),
    bypassDurationMinutes = bypassDurationMinutes,
    platform = Platform.valueOf(platform),
    eventSchemaVersion = eventSchemaVersion,
)

fun BlockEvent.toEntity(): BlockEventEntity = BlockEventEntity(
    id = id,
    appCatalogId = restrictedAppRef.value,
    occurredAtEpochMillis = occurredAt.toEpochMilli(),
    zoneId = zone.id,
    primaryReason = primaryReason.name,
    allReasons = allReasons.joinToString(",") { it.name },
    userAction = userAction.name,
    bypassDurationMinutes = bypassDurationMinutes,
    platform = platform.name,
    eventSchemaVersion = eventSchemaVersion,
)

fun UsageSessionEntity.toDomain(): UsageSession = UsageSession(
    forApp = AppRef(appCatalogId),
    from = Instant.ofEpochMilli(fromEpochMillis),
    to = toEpochMillis?.let(Instant::ofEpochMilli),
)

fun UsageSession.toEntity(): UsageSessionEntity = UsageSessionEntity(
    appCatalogId = forApp.value,
    fromEpochMillis = from.toEpochMilli(),
    toEpochMillis = to?.toEpochMilli(),
)
