package com.blocksocial.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.blocksocial.core.data.database.BlockSocialDatabase
import com.blocksocial.core.data.mapper.toEntity
import com.blocksocial.core.data.preferences.PermissionSnapshot
import com.blocksocial.core.data.preferences.PreferencesRepository
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.RestrictedApp
import com.blocksocial.core.model.RestrictionRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class RulesSurvivePermissionLossTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var database: BlockSocialDatabase
    private lateinit var file: File
    private lateinit var preferences: PreferencesRepository

    @Before
    fun open() {
        database = Room.inMemoryDatabaseBuilder(context, BlockSocialDatabase::class.java).build()
        file = File(context.cacheDir, "permission-loss-${System.nanoTime()}.preferences_pb")
        preferences = PreferencesRepository(
            PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }),
        )
    }

    @After
    fun close() {
        database.close()
        scope.cancel()
        file.delete()
    }

    @Test
    fun losingEveryPermissionLeavesTheSavedRulesEnabledAndUnchanged() = runTest {
        database.restrictedAppDao().upsert(
            RestrictedApp(ref = APP, displayName = "Instagram", selected = true).toEntity(AT),
        )
        database.restrictionRuleDao().upsert(
            RestrictionRule.AlwaysOn(id = RULE_ID, enabled = true).toEntity(APP, AT, AT),
        )
        preferences.setPermissionSnapshot(
            PermissionSnapshot(
                accessibilityServiceEnabled = true,
                usageAccessGranted = true,
                notificationsGranted = true,
                capturedAt = AT,
            ),
        )

        val before = database.restrictionRuleDao().enabledRulesFor(APP.value)

        preferences.setPermissionSnapshot(
            PermissionSnapshot(
                accessibilityServiceEnabled = false,
                usageAccessGranted = false,
                notificationsGranted = false,
                capturedAt = AT.plusSeconds(60),
            ),
        )

        val after = database.restrictionRuleDao().enabledRulesFor(APP.value)

        assertEquals("the rule was removed when the permission went away", before, after)
        assertEquals(listOf(RULE_ID), after.map { it.id })
        assertTrue("the rule was disabled when the permission went away", after.single().enabled)
    }

    @Test
    fun theRecordedSnapshotIsTheOnlyThingThatChanges() = runTest {
        preferences.setPermissionSnapshot(
            PermissionSnapshot(
                accessibilityServiceEnabled = true,
                usageAccessGranted = true,
                notificationsGranted = true,
                capturedAt = AT,
            ),
        )
        preferences.setPermissionSnapshot(
            PermissionSnapshot(
                accessibilityServiceEnabled = false,
                usageAccessGranted = false,
                notificationsGranted = false,
                capturedAt = AT.plusSeconds(60),
            ),
        )

        val stored = preferences.preferences.first()

        assertEquals(false, stored.permissionSnapshot.accessibilityServiceEnabled)
        assertEquals(AT.plusSeconds(60), stored.permissionSnapshot.capturedAt)
    }

    private companion object {
        const val RULE_ID = "always-on-instagram"
        val APP = AppRef("instagram")
        val AT: Instant = Instant.ofEpochMilli(1_800_000_000_000)
    }
}
