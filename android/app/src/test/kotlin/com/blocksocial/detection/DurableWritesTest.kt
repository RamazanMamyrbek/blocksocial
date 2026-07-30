package com.blocksocial.detection

import com.blocksocial.core.data.database.dao.BlockEventDao
import com.blocksocial.core.data.database.dao.TemporaryAccessGrantDao
import com.blocksocial.core.data.database.entity.BlockEventEntity
import com.blocksocial.core.data.database.entity.TemporaryAccessGrantEntity
import com.blocksocial.core.data.repository.BlockEventRepository
import com.blocksocial.core.data.repository.TemporaryAccessGrantRepository
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.BlockEvent
import com.blocksocial.core.model.Platform
import com.blocksocial.core.model.RuleMode
import com.blocksocial.core.model.TemporaryAccessGrant
import com.blocksocial.core.model.UserAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class DurableWritesTest {

    private val recorded = mutableListOf<String>()

    private val blockEventDao = object : BlockEventDao {
        override suspend fun insert(event: BlockEventEntity) {
            recorded += "event:${event.id}"
        }

        override fun observeRecent(limit: Int): Flow<List<BlockEventEntity>> = emptyFlow()

        override suspend fun between(fromEpochMillis: Long, untilEpochMillis: Long) = emptyList<BlockEventEntity>()

        override suspend fun deleteOlderThan(beforeEpochMillis: Long): Int = 0
    }

    private val grantDao = object : TemporaryAccessGrantDao {
        override suspend fun findByApp(appCatalogId: String): TemporaryAccessGrantEntity? = null

        override suspend fun all(): List<TemporaryAccessGrantEntity> = emptyList()

        override fun observeAll(): Flow<List<TemporaryAccessGrantEntity>> = emptyFlow()

        override suspend fun put(grant: TemporaryAccessGrantEntity) {
            recorded += "grant:${grant.appCatalogId}"
        }

        override suspend fun deleteByApp(appCatalogId: String) {
            recorded += "cleared:$appCatalogId"
        }
    }

    private fun writes(scope: CoroutineScope) = DurableWrites(
        blockEvents = BlockEventRepository(blockEventDao),
        grants = TemporaryAccessGrantRepository(grantDao),
        scope = scope,
    )

    @Test
    fun aWriteLaunchedOnTheServiceOwnScopeIsLostWhenTheServiceStops() = runTest {
        val serviceScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))

        serviceScope.launch { BlockEventRepository(blockEventDao).record(EVENT) }
        serviceScope.cancel()
        advanceUntilIdle()

        assertEquals(emptyList<String>(), recorded)
    }

    @Test
    fun aBlockIsStillRecordedAfterTheServiceThatSawItHasBeenTornDown() = runTest {
        val serviceScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val writes = writes(CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler)))

        writes.record(EVENT)
        serviceScope.cancel()
        advanceUntilIdle()

        assertEquals(listOf("event:${EVENT.id}"), recorded)
    }

    @Test
    fun aGrantAndItsRemovalOutliveTheServiceToo() = runTest {
        val writeScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val writes = writes(writeScope)

        writes.keep(GRANT)
        writes.forget(APP)
        advanceUntilIdle()

        assertEquals(listOf("grant:${APP.value}", "cleared:${APP.value}"), recorded)
    }

    @Test
    fun aFailingWriteIsSwallowedRatherThanKillingEveryLaterWrite() = runTest {
        val exploding = object : BlockEventDao {
            override suspend fun insert(event: BlockEventEntity) = error("disk is on fire")

            override fun observeRecent(limit: Int): Flow<List<BlockEventEntity>> = emptyFlow()

            override suspend fun between(fromEpochMillis: Long, untilEpochMillis: Long) = emptyList<BlockEventEntity>()

            override suspend fun deleteOlderThan(beforeEpochMillis: Long): Int = 0
        }
        val writeScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val writes = DurableWrites(
            blockEvents = BlockEventRepository(exploding),
            grants = TemporaryAccessGrantRepository(grantDao),
            scope = writeScope,
        )

        writes.record(EVENT)
        advanceUntilIdle()
        writes.keep(GRANT)
        advanceUntilIdle()

        assertEquals(listOf("grant:${APP.value}"), recorded)
    }

    private companion object {
        val APP = AppRef("youtube")
        val AT: Instant = Instant.parse("2026-07-30T12:00:00Z")

        val EVENT = BlockEvent(
            id = "block-1",
            restrictedAppRef = APP,
            occurredAt = AT,
            zone = ZoneId.of("Europe/Berlin"),
            primaryReason = RuleMode.ALWAYS,
            allReasons = listOf(RuleMode.ALWAYS),
            userAction = UserAction.DISMISSED_BY_SYSTEM,
            bypassDurationMinutes = null,
            platform = Platform.ANDROID,
        )

        val GRANT = TemporaryAccessGrant(
            forApp = APP,
            grantedAtWallClock = AT,
            grantedAtMonotonicMillis = 1_000,
            durationMinutes = 5,
        )
    }
}
