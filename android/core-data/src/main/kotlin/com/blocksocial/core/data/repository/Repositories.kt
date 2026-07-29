package com.blocksocial.core.data.repository

import com.blocksocial.core.data.database.dao.BlockEventDao
import com.blocksocial.core.data.database.dao.DailyStatisticsDao
import com.blocksocial.core.data.database.dao.RestrictedAppDao
import com.blocksocial.core.data.database.dao.RestrictionRuleDao
import com.blocksocial.core.data.database.dao.TemporaryAccessGrantDao
import com.blocksocial.core.data.database.dao.UsageSessionDao
import com.blocksocial.core.data.database.entity.DailyStatisticsEntity
import com.blocksocial.core.data.mapper.toDomain
import com.blocksocial.core.data.mapper.toEntity
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.BlockEvent
import com.blocksocial.core.model.RestrictedApp
import com.blocksocial.core.model.RestrictionRule
import com.blocksocial.core.model.TemporaryAccessGrant
import com.blocksocial.core.model.UsageSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestrictedAppRepository @Inject constructor(private val dao: RestrictedAppDao) {

    fun observeAll(): Flow<List<RestrictedApp>> = dao.observeAll().map { it.map { entity -> entity.toDomain() } }

    fun observeSelected(): Flow<List<RestrictedApp>> =
        dao.observeSelected().map { it.map { entity -> entity.toDomain() } }

    suspend fun find(ref: AppRef): RestrictedApp? = dao.findByCatalogId(ref.value)?.toDomain()

    suspend fun save(app: RestrictedApp, createdAt: Instant) = dao.upsert(app.toEntity(createdAt))

    suspend fun setSelected(ref: AppRef, selected: Boolean) = dao.setSelected(ref.value, selected)
}

@Singleton
class RestrictionRuleRepository @Inject constructor(private val dao: RestrictionRuleDao) {

    suspend fun enabledRulesFor(ref: AppRef): List<RestrictionRule> =
        dao.enabledRulesFor(ref.value).map { it.toDomain() }

    fun observeRulesFor(ref: AppRef): Flow<List<RestrictionRule>> =
        dao.observeRulesFor(ref.value).map { it.map { entity -> entity.toDomain() } }

    fun observeRulesByApp(): Flow<Map<AppRef, List<RestrictionRule>>> =
        dao.observeAll().map { entities ->
            entities.groupBy({ AppRef(it.appCatalogId) }, { it.toDomain() })
        }

    suspend fun save(rule: RestrictionRule, forApp: AppRef, createdAt: Instant, updatedAt: Instant) =
        dao.upsert(rule.toEntity(forApp, createdAt, updatedAt))

    suspend fun delete(ruleId: String) = dao.deleteById(ruleId)
}

@Singleton
class TemporaryAccessGrantRepository @Inject constructor(private val dao: TemporaryAccessGrantDao) {

    suspend fun grantFor(ref: AppRef): TemporaryAccessGrant? = dao.findByApp(ref.value)?.toDomain()

    suspend fun all(): List<TemporaryAccessGrant> = dao.all().map { it.toDomain() }

    fun observeAll(): Flow<List<TemporaryAccessGrant>> =
        dao.observeAll().map { it.map { entity -> entity.toDomain() } }

    suspend fun put(grant: TemporaryAccessGrant, sourceBlockEventId: String? = null) =
        dao.put(grant.toEntity(sourceBlockEventId))

    suspend fun clear(ref: AppRef) = dao.deleteByApp(ref.value)
}

@Singleton
class BlockEventRepository @Inject constructor(private val dao: BlockEventDao) {

    suspend fun record(event: BlockEvent) = dao.insert(event.toEntity())

    fun observeRecent(limit: Int): Flow<List<BlockEvent>> =
        dao.observeRecent(limit).map { it.map { entity -> entity.toDomain() } }

    suspend fun between(from: Instant, until: Instant): List<BlockEvent> =
        dao.between(from.toEpochMilli(), until.toEpochMilli()).map { it.toDomain() }

    suspend fun deleteOlderThan(before: Instant): Int = dao.deleteOlderThan(before.toEpochMilli())
}

@Singleton
class UsageSessionRepository @Inject constructor(private val dao: UsageSessionDao) {

    suspend fun record(session: UsageSession): Long = dao.insert(session.toEntity())

    suspend fun overlapping(ref: AppRef, from: Instant, until: Instant): List<UsageSession> =
        dao.overlapping(ref.value, from.toEpochMilli(), until.toEpochMilli()).map { it.toDomain() }

    suspend fun deleteOlderThan(before: Instant): Int = dao.deleteOlderThan(before.toEpochMilli())
}

data class DailyStatistics(
    val localDate: LocalDate,
    val forApp: AppRef,
    val interventions: Int,
    val stayedFocused: Int,
    val bypassed: Int,
    val measuredMinutes: Int,
)

@Singleton
class DailyStatisticsRepository @Inject constructor(private val dao: DailyStatisticsDao) {

    suspend fun save(statistics: DailyStatistics) = dao.upsert(
        DailyStatisticsEntity(
            localDate = statistics.localDate.toString(),
            appCatalogId = statistics.forApp.value,
            interventions = statistics.interventions,
            stayedFocused = statistics.stayedFocused,
            bypassed = statistics.bypassed,
            measuredMinutes = statistics.measuredMinutes,
        ),
    )

    suspend fun forDate(localDate: LocalDate): List<DailyStatistics> =
        dao.forDate(localDate.toString()).map { it.toDomain() }

    fun observeRecent(limit: Int): Flow<List<DailyStatistics>> =
        dao.observeRecent(limit).map { it.map { entity -> entity.toDomain() } }

    private fun DailyStatisticsEntity.toDomain(): DailyStatistics = DailyStatistics(
        localDate = LocalDate.parse(localDate),
        forApp = AppRef(appCatalogId),
        interventions = interventions,
        stayedFocused = stayedFocused,
        bypassed = bypassed,
        measuredMinutes = measuredMinutes,
    )
}
