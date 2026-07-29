package com.blocksocial.core.data.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.blocksocial.core.data.database.entity.BlockEventEntity
import com.blocksocial.core.data.database.entity.RestrictedAppEntity
import com.blocksocial.core.data.database.entity.RestrictionRuleEntity
import com.blocksocial.core.data.database.entity.TemporaryAccessGrantEntity
import com.blocksocial.core.data.database.entity.UsageSessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlockSocialDaoTest {

    private lateinit var database: BlockSocialDatabase

    @Before
    fun openInMemoryDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            BlockSocialDatabase::class.java,
        ).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private suspend fun givenApp(catalogId: String = "instagram", selected: Boolean = true) {
        database.restrictedAppDao().upsert(
            RestrictedAppEntity(
                catalogId = catalogId,
                displayName = catalogId,
                selected = selected,
                createdAtEpochMillis = 0,
            ),
        )
    }

    private fun rule(id: String, appCatalogId: String = "instagram", enabled: Boolean = true) =
        RestrictionRuleEntity(
            id = id,
            appCatalogId = appCatalogId,
            mode = "ALWAYS",
            enabled = enabled,
            daysOfWeek = null,
            startLocalTime = null,
            endLocalTime = null,
            dailyLimitMinutes = null,
            createdAtEpochMillis = 0,
            updatedAtEpochMillis = 0,
        )

    @Test
    fun onlySelectedApplicationsAreObserved() = runTest {
        givenApp("instagram", selected = true)
        givenApp("tiktok", selected = false)

        val selected = database.restrictedAppDao().observeSelected().first()

        assertEquals(listOf("instagram"), selected.map { it.catalogId })
    }

    @Test
    fun theDetectionPathReadsOnlyEnabledRules() = runTest {
        givenApp()
        database.restrictionRuleDao().upsert(rule("enabled-rule", enabled = true))
        database.restrictionRuleDao().upsert(rule("disabled-rule", enabled = false))

        val rules = database.restrictionRuleDao().enabledRulesFor("instagram")

        assertEquals(listOf("enabled-rule"), rules.map { it.id })
    }

    @Test
    fun deletingAnApplicationRemovesItsRules() = runTest {
        givenApp()
        database.restrictionRuleDao().upsert(rule("r1"))

        database.restrictedAppDao().delete(
            RestrictedAppEntity("instagram", "instagram", true, 0),
        )

        assertEquals(emptyList<RestrictionRuleEntity>(), database.restrictionRuleDao().enabledRulesFor("instagram"))
    }

    @Test
    fun anApplicationCannotHoldTwoGrantsAtOnce() = runTest {
        val dao = database.temporaryAccessGrantDao()
        dao.put(TemporaryAccessGrantEntity("instagram", 1_000, 5_000_000, 5, null))
        dao.put(TemporaryAccessGrantEntity("instagram", 2_000, 6_000_000, 15, "event-1"))

        val all = dao.all()

        assertEquals(1, all.size)
        assertEquals(15, all.single().durationMinutes)
        assertEquals(6_000_000, all.single().grantedAtMonotonicMillis)
    }

    @Test
    fun clearingAGrantLeavesNothingBehind() = runTest {
        val dao = database.temporaryAccessGrantDao()
        dao.put(TemporaryAccessGrantEntity("instagram", 1_000, 5_000_000, 5, null))

        dao.deleteByApp("instagram")

        assertNull(dao.findByApp("instagram"))
    }

    @Test
    fun blockEventsAreReadNewestFirst() = runTest {
        val dao = database.blockEventDao()
        dao.insert(blockEvent("older", occurredAt = 1_000))
        dao.insert(blockEvent("newer", occurredAt = 2_000))

        val recent = dao.observeRecent(10).first()

        assertEquals(listOf("newer", "older"), recent.map { it.id })
    }

    @Test
    fun blockEventsAreFilteredByAHalfOpenWindow() = runTest {
        val dao = database.blockEventDao()
        dao.insert(blockEvent("before", occurredAt = 999))
        dao.insert(blockEvent("start", occurredAt = 1_000))
        dao.insert(blockEvent("end", occurredAt = 2_000))

        val inside = dao.between(1_000, 2_000)

        assertEquals(listOf("start"), inside.map { it.id })
    }

    @Test
    fun aRunningSessionIsReturnedByTheDayWindowQuery() = runTest {
        val dao = database.usageSessionDao()
        dao.insert(UsageSessionEntity(appCatalogId = "instagram", fromEpochMillis = 500, toEpochMillis = null))
        dao.insert(UsageSessionEntity(appCatalogId = "instagram", fromEpochMillis = 100, toEpochMillis = 200))
        dao.insert(UsageSessionEntity(appCatalogId = "tiktok", fromEpochMillis = 600, toEpochMillis = null))

        val overlapping = dao.overlapping("instagram", fromEpochMillis = 400, untilEpochMillis = 1_000)

        assertEquals(listOf(500L), overlapping.map { it.fromEpochMillis })
    }

    @Test
    fun aSessionThatStartedBeforeTheWindowStillCounts() = runTest {
        val dao = database.usageSessionDao()
        dao.insert(UsageSessionEntity(appCatalogId = "instagram", fromEpochMillis = 100, toEpochMillis = 900))

        val overlapping = dao.overlapping("instagram", fromEpochMillis = 400, untilEpochMillis = 1_000)

        assertEquals(1, overlapping.size)
    }

    private fun blockEvent(id: String, occurredAt: Long) = BlockEventEntity(
        id = id,
        appCatalogId = "instagram",
        occurredAtEpochMillis = occurredAt,
        zoneId = "Asia/Tokyo",
        primaryReason = "ALWAYS",
        allReasons = "ALWAYS",
        userAction = "STAYED_FOCUSED",
        bypassDurationMinutes = null,
        platform = "ANDROID",
        eventSchemaVersion = 1,
    )
}
