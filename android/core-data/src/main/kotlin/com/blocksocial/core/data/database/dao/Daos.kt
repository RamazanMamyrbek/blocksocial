package com.blocksocial.core.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.blocksocial.core.data.database.entity.BlockEventEntity
import com.blocksocial.core.data.database.entity.DailyStatisticsEntity
import com.blocksocial.core.data.database.entity.RestrictedAppEntity
import com.blocksocial.core.data.database.entity.RestrictionRuleEntity
import com.blocksocial.core.data.database.entity.TemporaryAccessGrantEntity
import com.blocksocial.core.data.database.entity.UsageSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RestrictedAppDao {

    @Query("SELECT * FROM restricted_app ORDER BY displayName")
    fun observeAll(): Flow<List<RestrictedAppEntity>>

    @Query("SELECT * FROM restricted_app WHERE selected = 1 ORDER BY displayName")
    fun observeSelected(): Flow<List<RestrictedAppEntity>>

    @Query("SELECT * FROM restricted_app WHERE catalogId = :catalogId")
    suspend fun findByCatalogId(catalogId: String): RestrictedAppEntity?

    @Upsert
    suspend fun upsert(app: RestrictedAppEntity)

    @Query("UPDATE restricted_app SET selected = :selected WHERE catalogId = :catalogId")
    suspend fun setSelected(catalogId: String, selected: Boolean)

    @Delete
    suspend fun delete(app: RestrictedAppEntity)
}

@Dao
interface RestrictionRuleDao {

    @Query("SELECT * FROM restriction_rule WHERE appCatalogId = :appCatalogId AND enabled = 1")
    suspend fun enabledRulesFor(appCatalogId: String): List<RestrictionRuleEntity>

    @Query("SELECT * FROM restriction_rule WHERE appCatalogId = :appCatalogId")
    fun observeRulesFor(appCatalogId: String): Flow<List<RestrictionRuleEntity>>

    @Query("SELECT * FROM restriction_rule")
    fun observeAll(): Flow<List<RestrictionRuleEntity>>

    @Upsert
    suspend fun upsert(rule: RestrictionRuleEntity)

    @Query("DELETE FROM restriction_rule WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface TemporaryAccessGrantDao {

    @Query("SELECT * FROM temporary_access_grant WHERE appCatalogId = :appCatalogId")
    suspend fun findByApp(appCatalogId: String): TemporaryAccessGrantEntity?

    @Query("SELECT * FROM temporary_access_grant")
    suspend fun all(): List<TemporaryAccessGrantEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(grant: TemporaryAccessGrantEntity)

    @Query("DELETE FROM temporary_access_grant WHERE appCatalogId = :appCatalogId")
    suspend fun deleteByApp(appCatalogId: String)
}

@Dao
interface BlockEventDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: BlockEventEntity)

    @Query("SELECT * FROM block_event ORDER BY occurredAtEpochMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<BlockEventEntity>>

    @Query(
        "SELECT * FROM block_event " +
            "WHERE occurredAtEpochMillis >= :fromEpochMillis AND occurredAtEpochMillis < :untilEpochMillis " +
            "ORDER BY occurredAtEpochMillis DESC",
    )
    suspend fun between(fromEpochMillis: Long, untilEpochMillis: Long): List<BlockEventEntity>

    @Query("DELETE FROM block_event WHERE occurredAtEpochMillis < :beforeEpochMillis")
    suspend fun deleteOlderThan(beforeEpochMillis: Long): Int
}

@Dao
interface UsageSessionDao {

    @Insert
    suspend fun insert(session: UsageSessionEntity): Long

    @Query(
        "SELECT * FROM usage_session " +
            "WHERE appCatalogId = :appCatalogId " +
            "AND (toEpochMillis IS NULL OR toEpochMillis > :fromEpochMillis) " +
            "AND fromEpochMillis < :untilEpochMillis " +
            "ORDER BY fromEpochMillis",
    )
    suspend fun overlapping(
        appCatalogId: String,
        fromEpochMillis: Long,
        untilEpochMillis: Long,
    ): List<UsageSessionEntity>

    @Query("DELETE FROM usage_session WHERE toEpochMillis IS NOT NULL AND toEpochMillis < :beforeEpochMillis")
    suspend fun deleteOlderThan(beforeEpochMillis: Long): Int
}

@Dao
interface DailyStatisticsDao {

    @Upsert
    suspend fun upsert(statistics: DailyStatisticsEntity)

    @Query("SELECT * FROM daily_statistics WHERE localDate = :localDate")
    suspend fun forDate(localDate: String): List<DailyStatisticsEntity>

    @Query("SELECT * FROM daily_statistics ORDER BY localDate DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<DailyStatisticsEntity>>
}
