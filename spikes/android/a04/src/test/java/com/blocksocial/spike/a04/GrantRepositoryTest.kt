package com.blocksocial.spike.a04

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GrantRepositoryTest {

    private val youtube = "com.google.android.youtube"
    private val chrome = "com.android.chrome"
    private val oneMinute = 60_000L

    private class InMemoryGrantStorage(
        initial: List<TemporaryAccessGrant> = emptyList()
    ) : GrantStorage {
        val stored = initial.associateBy { it.packageName }.toMutableMap()

        override fun readAll() = stored.values.toList()
        override fun write(grant: TemporaryAccessGrant) {
            stored[grant.packageName] = grant
        }

        override fun remove(packageName: String) {
            stored.remove(packageName)
        }
    }

    private val start = DeviceTime(1_700_000_000_000L, 5_000_000L)

    private fun laterBy(millis: Long) = DeviceTime(
        wallClockMillis = start.wallClockMillis + millis,
        elapsedRealtimeMillis = start.elapsedRealtimeMillis + millis
    )

    @Test
    fun `a granted application is suppressed and another is not`() {
        val repository = GrantRepository(InMemoryGrantStorage())
        repository.grant(youtube, oneMinute, start)

        assertEquals(GrantEvaluation.ACTIVE, repository.evaluate(youtube, laterBy(1_000)))
        assertNull(repository.evaluate(chrome, laterBy(1_000)))
    }

    @Test
    fun `an expired grant stops suppressing and is removed from storage`() {
        val storage = InMemoryGrantStorage()
        val repository = GrantRepository(storage)
        repository.grant(youtube, oneMinute, start)

        assertEquals(GrantEvaluation.EXPIRED, repository.evaluate(youtube, laterBy(oneMinute)))
        assertTrue(storage.stored.isEmpty())
        assertNull(repository.evaluate(youtube, laterBy(oneMinute + 1)))
    }

    @Test
    fun `state is recomputed from storage rather than memory`() {
        val storage = InMemoryGrantStorage()
        GrantRepository(storage).grant(youtube, oneMinute, start)

        val afterProcessDeath = GrantRepository(storage)
        afterProcessDeath.loadAndPurge(laterBy(1_000))

        assertEquals(setOf(youtube), afterProcessDeath.activePackages())
        assertEquals(GrantEvaluation.ACTIVE, afterProcessDeath.evaluate(youtube, laterBy(1_000)))
    }

    @Test
    fun `loading purges grants that no longer apply`() {
        val storage = InMemoryGrantStorage()
        GrantRepository(storage).grant(youtube, oneMinute, start)

        val afterProcessDeath = GrantRepository(storage)
        val discarded = afterProcessDeath.loadAndPurge(laterBy(oneMinute * 5))

        assertEquals(listOf(youtube to GrantEvaluation.EXPIRED), discarded)
        assertTrue(storage.stored.isEmpty())
        assertTrue(afterProcessDeath.activePackages().isEmpty())
    }

    @Test
    fun `a grant survives a reboot with its remaining wall clock time`() {
        val storage = InMemoryGrantStorage()
        GrantRepository(storage).grant(youtube, oneMinute, start)

        val afterReboot = GrantRepository(storage)
        val bootTime = DeviceTime(
            wallClockMillis = start.wallClockMillis + oneMinute / 2,
            elapsedRealtimeMillis = 3_000L
        )
        afterReboot.loadAndPurge(bootTime)

        assertEquals(setOf(youtube), afterReboot.activePackages())
        assertEquals(GrantEvaluation.ACTIVE_AFTER_REBOOT, afterReboot.evaluate(youtube, bootTime))
    }

    @Test
    fun `a grant whose window passed during a reboot does not come back`() {
        val storage = InMemoryGrantStorage()
        GrantRepository(storage).grant(youtube, oneMinute, start)

        val afterReboot = GrantRepository(storage)
        val bootTime = DeviceTime(
            wallClockMillis = start.wallClockMillis + oneMinute * 3,
            elapsedRealtimeMillis = 3_000L
        )
        val discarded = afterReboot.loadAndPurge(bootTime)

        assertEquals(listOf(youtube to GrantEvaluation.EXPIRED_AFTER_REBOOT), discarded)
        assertTrue(afterReboot.activePackages().isEmpty())
    }

    @Test
    fun `two applications hold independent grants`() {
        val repository = GrantRepository(InMemoryGrantStorage())
        repository.grant(youtube, oneMinute, start)
        repository.grant(chrome, oneMinute * 10, start)

        val afterOneMinute = laterBy(oneMinute + 1)
        assertEquals(GrantEvaluation.EXPIRED, repository.evaluate(youtube, afterOneMinute))
        assertEquals(GrantEvaluation.ACTIVE, repository.evaluate(chrome, afterOneMinute))
        assertEquals(setOf(chrome), repository.activePackages())
    }

    @Test
    fun `granting the same application again replaces the previous window`() {
        val storage = InMemoryGrantStorage()
        val repository = GrantRepository(storage)
        repository.grant(youtube, oneMinute, start)
        repository.grant(youtube, oneMinute, laterBy(30_000))

        assertEquals(1, storage.stored.size)
        assertEquals(GrantEvaluation.ACTIVE, repository.evaluate(youtube, laterBy(80_000)))
    }
}
