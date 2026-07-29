package com.blocksocial.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.blocksocial.core.data.database.BlockSocialDatabase
import com.blocksocial.core.data.database.dao.BlockEventDao
import com.blocksocial.core.data.database.dao.DailyStatisticsDao
import com.blocksocial.core.data.database.dao.RestrictedAppDao
import com.blocksocial.core.data.database.dao.RestrictionRuleDao
import com.blocksocial.core.data.database.dao.TemporaryAccessGrantDao
import com.blocksocial.core.data.database.dao.UsageSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    private const val PREFERENCES_NAME = "blocksocial"

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): BlockSocialDatabase =
        Room.databaseBuilder(context, BlockSocialDatabase::class.java, BlockSocialDatabase.NAME)
            .addMigrations(*BlockSocialDatabase.MIGRATIONS)
            .build()

    @Provides
    fun restrictedAppDao(database: BlockSocialDatabase): RestrictedAppDao = database.restrictedAppDao()

    @Provides
    fun restrictionRuleDao(database: BlockSocialDatabase): RestrictionRuleDao = database.restrictionRuleDao()

    @Provides
    fun temporaryAccessGrantDao(database: BlockSocialDatabase): TemporaryAccessGrantDao =
        database.temporaryAccessGrantDao()

    @Provides
    fun blockEventDao(database: BlockSocialDatabase): BlockEventDao = database.blockEventDao()

    @Provides
    fun usageSessionDao(database: BlockSocialDatabase): UsageSessionDao = database.usageSessionDao()

    @Provides
    fun dailyStatisticsDao(database: BlockSocialDatabase): DailyStatisticsDao = database.dailyStatisticsDao()

    @Provides
    @Singleton
    fun preferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { context.preferencesDataStoreFile(PREFERENCES_NAME) },
        )
}
