package com.blocksocial.core.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class PreferencesRepositoryTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var file: File
    private lateinit var repository: PreferencesRepository

    @Before
    fun createStore() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        file = File(context.cacheDir, "preferences-test-${System.nanoTime()}.preferences_pb")
        repository = PreferencesRepository(
            PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }),
        )
    }

    @After
    fun deleteStore() {
        scope.cancel()
        file.delete()
    }

    @Test
    fun anEmptyStoreReadsAsTheDocumentedDefaults() = runTest {
        assertEquals(UserPreferences.Default, repository.preferences.first())
    }

    @Test
    fun everyWrittenPreferenceIsReadBack() = runTest {
        repository.setOnboardingCompleted(true)
        repository.setTheme(ThemePreference.DARK)
        repository.setLanguageTag("ru")
        repository.setAcceptedConsentVersion(3)
        repository.setDebugLoggingEnabled(true)

        val preferences = repository.preferences.first()

        assertEquals(true, preferences.onboardingCompleted)
        assertEquals(ThemePreference.DARK, preferences.theme)
        assertEquals("ru", preferences.languageTag)
        assertEquals(3, preferences.acceptedConsentVersion)
        assertEquals(true, preferences.debugLoggingEnabled)
    }

    @Test
    fun clearingTheLanguageRestoresTheSystemDefault() = runTest {
        repository.setLanguageTag("ru")
        repository.setLanguageTag(null)

        assertNull(repository.preferences.first().languageTag)
    }

    @Test
    fun thePermissionSnapshotIsStoredWholeAndObserved() = runTest {
        val capturedAt = Instant.ofEpochMilli(1_700_000_000_000)

        repository.preferences.test {
            assertEquals(PermissionSnapshot.Unknown, awaitItem().permissionSnapshot)

            repository.setPermissionSnapshot(
                PermissionSnapshot(
                    accessibilityServiceEnabled = true,
                    usageAccessGranted = true,
                    notificationsGranted = false,
                    capturedAt = capturedAt,
                ),
            )

            val updated = awaitItem().permissionSnapshot
            assertEquals(true, updated.accessibilityServiceEnabled)
            assertEquals(true, updated.usageAccessGranted)
            assertEquals(false, updated.notificationsGranted)
            assertEquals(capturedAt, updated.capturedAt)
        }
    }
}
