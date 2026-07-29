package com.blocksocial.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.blocksocial.core.data.database.dao.BlockEventDao
import com.blocksocial.core.data.database.dao.DailyStatisticsDao
import com.blocksocial.core.data.database.dao.RestrictedAppDao
import com.blocksocial.core.data.database.dao.RestrictionRuleDao
import com.blocksocial.core.data.database.dao.TemporaryAccessGrantDao
import com.blocksocial.core.data.database.dao.UsageSessionDao
import com.blocksocial.core.data.database.entity.BlockEventEntity
import com.blocksocial.core.data.database.entity.DailyStatisticsEntity
import com.blocksocial.core.data.database.entity.RestrictedAppEntity
import com.blocksocial.core.data.database.entity.RestrictionRuleEntity
import com.blocksocial.core.data.database.entity.TemporaryAccessGrantEntity
import com.blocksocial.core.data.database.entity.UsageSessionEntity

@Database(
    entities = [
        RestrictedAppEntity::class,
        RestrictionRuleEntity::class,
        TemporaryAccessGrantEntity::class,
        BlockEventEntity::class,
        UsageSessionEntity::class,
        DailyStatisticsEntity::class,
    ],
    version = BlockSocialDatabase.VERSION,
    exportSchema = true,
)
abstract class BlockSocialDatabase : RoomDatabase() {

    abstract fun restrictedAppDao(): RestrictedAppDao

    abstract fun restrictionRuleDao(): RestrictionRuleDao

    abstract fun temporaryAccessGrantDao(): TemporaryAccessGrantDao

    abstract fun blockEventDao(): BlockEventDao

    abstract fun usageSessionDao(): UsageSessionDao

    abstract fun dailyStatisticsDao(): DailyStatisticsDao

    companion object {
        const val VERSION = 1
        const val NAME = "blocksocial.db"

        val MIGRATIONS: Array<androidx.room.migration.Migration> = emptyArray()
    }
}
