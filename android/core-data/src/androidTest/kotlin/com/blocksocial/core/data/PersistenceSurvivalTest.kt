package com.blocksocial.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.blocksocial.core.data.database.BlockSocialDatabase
import com.blocksocial.core.data.mapper.toEntity
import com.blocksocial.core.data.preferences.PreferencesRepository
import com.blocksocial.core.data.preferences.ThemePreference
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.BlockEvent
import com.blocksocial.core.model.Platform
import com.blocksocial.core.model.RestrictedApp
import com.blocksocial.core.model.RestrictionRule
import com.blocksocial.core.model.RuleMode
import com.blocksocial.core.model.TemporaryAccessGrant
import com.blocksocial.core.model.UserAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class PersistenceSurvivalTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun database(): BlockSocialDatabase = Room.databaseBuilder(
        context,
        BlockSocialDatabase::class.java,
        BlockSocialDatabase.NAME,
    ).addMigrations(*BlockSocialDatabase.MIGRATIONS).build()

    private fun preferences() = PreferencesRepository(
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = { context.preferencesDataStoreFile(PREFERENCES_NAME) },
        ),
    )

    @Test
    fun seedTheDatabase() = runTest {
        val database = database()
        try {
            database.clearAllTables()
            database.restrictedAppDao().upsert(
                RestrictedApp(ref = APP, displayName = "Instagram", selected = true)
                    .toEntity(GRANTED_AT),
            )
            database.restrictionRuleDao().upsert(
                RestrictionRule.Schedule(
                    id = RULE_ID,
                    enabled = true,
                    daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
                    startLocalTime = LocalTime.of(22, 0),
                    endLocalTime = LocalTime.of(7, 0),
                ).toEntity(APP, GRANTED_AT, GRANTED_AT),
            )
            database.temporaryAccessGrantDao().put(
                TemporaryAccessGrant(
                    forApp = APP,
                    grantedAtWallClock = GRANTED_AT,
                    grantedAtMonotonicMillis = 5_000_000,
                    durationMinutes = 5,
                ).toEntity(sourceBlockEventId = EVENT_ID),
            )
            database.blockEventDao().insert(
                BlockEvent(
                    id = EVENT_ID,
                    restrictedAppRef = APP,
                    occurredAt = GRANTED_AT,
                    zone = ZoneId.of("Asia/Tokyo"),
                    primaryReason = RuleMode.SCHEDULE,
                    allReasons = listOf(RuleMode.SCHEDULE),
                    userAction = UserAction.BYPASSED,
                    bypassDurationMinutes = 5,
                    platform = Platform.ANDROID,
                ).toEntity(),
            )
        } finally {
            database.close()
        }

        preferences().apply {
            setOnboardingCompleted(true)
            setTheme(ThemePreference.DARK)
            setAcceptedConsentVersion(SEEDED_CONSENT_VERSION)
        }
    }

    @Test
    fun theSeededStateIsStillThere() = runTest {
        val database = database()
        try {
            val app = database.restrictedAppDao().findByCatalogId(APP.value)
            assertNotNull("the restricted application is gone", app)
            assertEquals("Instagram", app!!.displayName)

            val rules = database.restrictionRuleDao().enabledRulesFor(APP.value)
            assertEquals(listOf(RULE_ID), rules.map { it.id })
            assertEquals("MONDAY,FRIDAY", rules.single().daysOfWeek)
            assertEquals("22:00", rules.single().startLocalTime)

            val grant = database.temporaryAccessGrantDao().findByApp(APP.value)
            assertNotNull("the grant is gone", grant)
            assertEquals(5_000_000, grant!!.grantedAtMonotonicMillis)
            assertEquals(GRANTED_AT.toEpochMilli(), grant.grantedAtWallClockEpochMillis)

            val events = database.blockEventDao().observeRecent(10).first()
            assertEquals(listOf(EVENT_ID), events.map { it.id })
            assertEquals(5, events.single().bypassDurationMinutes)
        } finally {
            database.close()
        }

        val stored = preferences().preferences.first()
        assertEquals(true, stored.onboardingCompleted)
        assertEquals(ThemePreference.DARK, stored.theme)
        assertEquals(SEEDED_CONSENT_VERSION, stored.acceptedConsentVersion)
    }

    private companion object {
        const val PREFERENCES_NAME = "blocksocial"
        const val RULE_ID = "seeded-overnight-rule"
        const val EVENT_ID = "seeded-block-event"
        const val SEEDED_CONSENT_VERSION = 7
        val APP = AppRef("instagram")
        val GRANTED_AT: Instant = Instant.ofEpochMilli(1_800_000_000_000)
    }
}
