package com.blocksocial.detection

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.blocksocial.core.data.catalog.SupportedAppCatalog
import com.blocksocial.core.data.database.BlockSocialDatabase
import com.blocksocial.core.data.mapper.toEntity
import com.blocksocial.core.data.repository.BlockEventRepository
import com.blocksocial.core.data.repository.RestrictedAppRepository
import com.blocksocial.core.data.repository.RestrictionRuleRepository
import com.blocksocial.core.data.repository.TemporaryAccessGrantRepository
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.BypassPolicy
import com.blocksocial.core.model.DeviceTime
import com.blocksocial.core.model.RestrictedApp
import com.blocksocial.core.model.RestrictionRule
import com.blocksocial.core.model.TemporaryAccessGrant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class RebootRecoveryTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var database: BlockSocialDatabase
    private lateinit var source: ProtectionSnapshotSource

    @Before
    fun open() {
        database = Room.inMemoryDatabaseBuilder(context, BlockSocialDatabase::class.java).build()
        source = ProtectionSnapshotSource(
            catalog = SupportedAppCatalog(context),
            restrictedApps = RestrictedAppRepository(database.restrictedAppDao()),
            rules = RestrictionRuleRepository(database.restrictionRuleDao()),
            grants = TemporaryAccessGrantRepository(database.temporaryAccessGrantDao()),
            blockEvents = BlockEventRepository(database.blockEventDao()),
        )
    }

    @After
    fun close() = database.close()

    private suspend fun seedARestrictedApplication() {
        database.restrictedAppDao().upsert(
            RestrictedApp(ref = APP, displayName = "YouTube", selected = true).toEntity(AT),
        )
        database.restrictionRuleDao().upsert(
            RestrictionRule.AlwaysOn(id = "always", enabled = true).toEntity(APP, AT, AT),
        )
    }

    @Test
    fun everythingTheServiceNeedsComesBackFromStorageWithNoScreenOpened() = runTest {
        seedARestrictedApplication()

        val snapshot = source.snapshots { ZONE }.first()

        assertTrue(
            "the package the service watches for was not restored",
            snapshot.packageToApp.containsValue(APP),
        )
        assertEquals(listOf("always"), snapshot.rulesByApp.getValue(APP).map { it.id })
        assertEquals("YouTube", snapshot.displayNames[APP])
    }

    @Test
    fun aRestoredRuleStillDecidesToBlock() = runTest {
        seedARestrictedApplication()
        val snapshot = source.snapshots { ZONE }.first()
        val packageName = snapshot.packageToApp.entries.first { it.value == APP }.key

        val pipeline = DetectionPipeline(
            selfPackage = context.packageName,
            systemPackages = emptySet(),
            isActivityWindow = { _, _ -> true },
            readDeviceTime = { deviceTime() },
        )

        val result = pipeline.onWindowStateChanged(packageName, "$packageName.Main", snapshot)

        assertTrue("a rule that survived the reboot did not block", result.shouldBlock)
    }

    @Test
    fun aGrantTakenBeforeARebootIsStillHonouredAfterIt() = runTest {
        seedARestrictedApplication()
        database.temporaryAccessGrantDao().put(
            BypassPolicy.Android.grant(APP, deviceTime()).toEntity(sourceBlockEventId = null),
        )

        val snapshot = source.snapshots { ZONE }.first()
        val packageName = snapshot.packageToApp.entries.first { it.value == APP }.key
        val pipeline = DetectionPipeline(
            selfPackage = context.packageName,
            systemPackages = emptySet(),
            isActivityWindow = { _, _ -> true },
            readDeviceTime = { afterReboot(secondsSinceGrant = 60) },
        )

        val result = pipeline.onWindowStateChanged(packageName, "$packageName.Main", snapshot)

        assertEquals(false, result.shouldBlock)
    }

    @Test
    fun aGrantThatExpiredWhileTheDeviceWasOffBlocksAgainAfterTheReboot() = runTest {
        seedARestrictedApplication()
        database.temporaryAccessGrantDao().put(
            BypassPolicy.Android.grant(APP, deviceTime()).toEntity(sourceBlockEventId = null),
        )

        val snapshot = source.snapshots { ZONE }.first()
        val packageName = snapshot.packageToApp.entries.first { it.value == APP }.key
        val pipeline = DetectionPipeline(
            selfPackage = context.packageName,
            systemPackages = emptySet(),
            isActivityWindow = { _, _ -> true },
            readDeviceTime = { afterReboot(secondsSinceGrant = EXPIRED_SECONDS) },
        )

        val result = pipeline.onWindowStateChanged(packageName, "$packageName.Main", snapshot)

        assertTrue("an expired grant survived a reboot", result.shouldBlock)
    }

    @Test
    fun nothingIsRestoredForAnApplicationTheUserHasDeselected() = runTest {
        seedARestrictedApplication()
        database.restrictedAppDao().upsert(
            RestrictedApp(ref = APP, displayName = "YouTube", selected = false).toEntity(AT),
        )

        val snapshot = source.snapshots { ZONE }.first()

        assertTrue("a deselected application is still watched", snapshot.packageToApp.isEmpty())
        assertNull("a deselected application still has live rules", snapshot.rulesByApp[APP])
    }

    private fun deviceTime() = DeviceTime(
        wallClock = AT,
        monotonicMillis = 5_000_000,
        zone = ZONE,
    )

    private fun afterReboot(secondsSinceGrant: Long) = DeviceTime(
        wallClock = AT.plusSeconds(secondsSinceGrant),
        monotonicMillis = 1_000,
        zone = ZONE,
    )

    private companion object {
        val APP = AppRef("youtube")
        val AT: Instant = Instant.parse("2026-07-30T12:00:00Z")
        val ZONE: ZoneId = ZoneId.of("Europe/Berlin")
        const val EXPIRED_SECONDS = 60L * 60L
    }
}
