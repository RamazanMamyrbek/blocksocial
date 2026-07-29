package com.blocksocial.core.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "restricted_app")
data class RestrictedAppEntity(
    @PrimaryKey val catalogId: String,
    val displayName: String,
    val selected: Boolean,
    val createdAtEpochMillis: Long,
)

@Entity(
    tableName = "restriction_rule",
    foreignKeys = [
        ForeignKey(
            entity = RestrictedAppEntity::class,
            parentColumns = ["catalogId"],
            childColumns = ["appCatalogId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["appCatalogId", "enabled"])],
)
data class RestrictionRuleEntity(
    @PrimaryKey val id: String,
    val appCatalogId: String,
    val mode: String,
    val enabled: Boolean,
    val daysOfWeek: String?,
    val startLocalTime: String?,
    val endLocalTime: String?,
    val dailyLimitMinutes: Int?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(tableName = "temporary_access_grant")
data class TemporaryAccessGrantEntity(
    @PrimaryKey val appCatalogId: String,
    val grantedAtWallClockEpochMillis: Long,
    val grantedAtMonotonicMillis: Long,
    val durationMinutes: Int,
    val sourceBlockEventId: String?,
)

@Entity(
    tableName = "block_event",
    indices = [
        Index(value = ["occurredAtEpochMillis"]),
        Index(value = ["appCatalogId", "occurredAtEpochMillis"]),
    ],
)
data class BlockEventEntity(
    @PrimaryKey val id: String,
    val appCatalogId: String,
    val occurredAtEpochMillis: Long,
    val zoneId: String,
    val primaryReason: String,
    val allReasons: String,
    val userAction: String,
    val bypassDurationMinutes: Int?,
    val platform: String,
    val eventSchemaVersion: Int,
)

@Entity(
    tableName = "usage_session",
    indices = [Index(value = ["appCatalogId", "fromEpochMillis"])],
)
data class UsageSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appCatalogId: String,
    val fromEpochMillis: Long,
    val toEpochMillis: Long?,
)

@Entity(tableName = "daily_statistics", primaryKeys = ["localDate", "appCatalogId"])
data class DailyStatisticsEntity(
    val localDate: String,
    val appCatalogId: String,
    val interventions: Int,
    val stayedFocused: Int,
    val bypassed: Int,
    val measuredMinutes: Int,
)
