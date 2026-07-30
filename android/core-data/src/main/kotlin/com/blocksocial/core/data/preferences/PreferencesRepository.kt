package com.blocksocial.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {

    val preferences: Flow<UserPreferences> = dataStore.data.map { it.toUserPreferences() }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.OnboardingCompleted] = completed }
    }

    suspend fun setTheme(theme: ThemePreference) {
        dataStore.edit { it[Keys.Theme] = theme.name }
    }

    suspend fun setLanguageTag(languageTag: String?) {
        dataStore.edit { preferences ->
            if (languageTag == null) preferences.remove(Keys.LanguageTag)
            else preferences[Keys.LanguageTag] = languageTag
        }
    }

    suspend fun setAcceptedConsentVersion(version: Int) {
        dataStore.edit { it[Keys.AcceptedConsentVersion] = version }
    }

    suspend fun setNotificationRequestMade(made: Boolean) {
        dataStore.edit { it[Keys.NotificationRequestMade] = made }
    }

    suspend fun setDebugLoggingEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.DebugLoggingEnabled] = enabled }
    }

    suspend fun setPermissionSnapshot(snapshot: PermissionSnapshot) {
        dataStore.edit { preferences ->
            preferences[Keys.AccessibilityServiceEnabled] = snapshot.accessibilityServiceEnabled
            preferences[Keys.UsageAccessGranted] = snapshot.usageAccessGranted
            preferences[Keys.NotificationsGranted] = snapshot.notificationsGranted
            val capturedAt = snapshot.capturedAt
            if (capturedAt == null) preferences.remove(Keys.PermissionsCapturedAt)
            else preferences[Keys.PermissionsCapturedAt] = capturedAt.toEpochMilli()
        }
    }

    private fun Preferences.toUserPreferences(): UserPreferences = UserPreferences(
        onboardingCompleted = this[Keys.OnboardingCompleted] ?: UserPreferences.Default.onboardingCompleted,
        theme = this[Keys.Theme]?.let(ThemePreference::valueOf) ?: UserPreferences.Default.theme,
        languageTag = this[Keys.LanguageTag],
        acceptedConsentVersion = this[Keys.AcceptedConsentVersion]
            ?: UserPreferences.Default.acceptedConsentVersion,
        permissionSnapshot = PermissionSnapshot(
            accessibilityServiceEnabled = this[Keys.AccessibilityServiceEnabled] ?: false,
            usageAccessGranted = this[Keys.UsageAccessGranted] ?: false,
            notificationsGranted = this[Keys.NotificationsGranted] ?: false,
            capturedAt = this[Keys.PermissionsCapturedAt]?.let(Instant::ofEpochMilli),
        ),
        notificationRequestMade = this[Keys.NotificationRequestMade]
            ?: UserPreferences.Default.notificationRequestMade,
        debugLoggingEnabled = this[Keys.DebugLoggingEnabled] ?: UserPreferences.Default.debugLoggingEnabled,
    )

    private object Keys {
        val OnboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val Theme = stringPreferencesKey("theme")
        val LanguageTag = stringPreferencesKey("language_tag")
        val AcceptedConsentVersion = intPreferencesKey("accepted_consent_version")
        val AccessibilityServiceEnabled = booleanPreferencesKey("accessibility_service_enabled")
        val UsageAccessGranted = booleanPreferencesKey("usage_access_granted")
        val NotificationsGranted = booleanPreferencesKey("notifications_granted")
        val PermissionsCapturedAt = longPreferencesKey("permissions_captured_at")
        val NotificationRequestMade = booleanPreferencesKey("notification_request_made")
        val DebugLoggingEnabled = booleanPreferencesKey("debug_logging_enabled")
    }
}
