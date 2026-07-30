package com.blocksocial.core.data.preferences

import java.time.Instant

enum class ThemePreference { SYSTEM, LIGHT, DARK }

data class PermissionSnapshot(
    val accessibilityServiceEnabled: Boolean,
    val usageAccessGranted: Boolean,
    val notificationsGranted: Boolean,
    val capturedAt: Instant?,
) {
    companion object {
        val Unknown = PermissionSnapshot(
            accessibilityServiceEnabled = false,
            usageAccessGranted = false,
            notificationsGranted = false,
            capturedAt = null,
        )
    }
}

data class UserPreferences(
    val onboardingCompleted: Boolean,
    val theme: ThemePreference,
    val languageTag: String?,
    val acceptedConsentVersion: Int,
    val permissionSnapshot: PermissionSnapshot,
    val accessibilityEverEnabled: Boolean,
    val notificationRequestMade: Boolean,
    val debugLoggingEnabled: Boolean,
) {
    companion object {
        val Default = UserPreferences(
            onboardingCompleted = false,
            theme = ThemePreference.SYSTEM,
            languageTag = null,
            acceptedConsentVersion = 0,
            permissionSnapshot = PermissionSnapshot.Unknown,
            accessibilityEverEnabled = false,
            notificationRequestMade = false,
            debugLoggingEnabled = false,
        )
    }
}
