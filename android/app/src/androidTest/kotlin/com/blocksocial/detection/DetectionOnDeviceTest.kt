package com.blocksocial.detection

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.StrictMode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.blocksocial.core.data.catalog.SupportedAppCatalog
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.DeviceTime
import com.blocksocial.core.model.RestrictionRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class DetectionOnDeviceTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    private fun pipeline() = DetectionPipeline(
        selfPackage = context.packageName,
        systemPackages = SystemPackages.resolve(context),
        isActivityWindow = { packageName, className ->
            if (className.isNullOrBlank()) {
                false
            } else {
                try {
                    context.packageManager.getActivityInfo(ComponentName(packageName, className), 0)
                    true
                } catch (notAnActivity: PackageManager.NameNotFoundException) {
                    false
                }
            }
        },
        readDeviceTime = {
            DeviceTime(
                wallClock = Instant.parse("2026-07-27T10:30:00Z"),
                monotonicMillis = 5_000_000,
                zone = ZoneId.systemDefault(),
            )
        },
    )

    @Test
    fun theAllowlistResolvesTheLauncherAndSettingsOnThisDevice() {
        val systemPackages = SystemPackages.resolve(context)

        assertTrue("settings missing from the allowlist", "com.android.settings" in systemPackages)
        assertTrue("system ui missing from the allowlist", "com.android.systemui" in systemPackages)
        assertTrue(
            "no launcher resolved, so home would be blockable",
            systemPackages.any { it.contains("launcher", ignoreCase = true) },
        )
    }

    @Test
    fun noAllowlistedPackageIsEverBlockedEvenWhenClaimedByARule() {
        val systemPackages = SystemPackages.resolve(context)
        val alwaysOn = listOf(RestrictionRule.AlwaysOn("always", enabled = true))
        val hostileSnapshot = ProtectionSnapshot(
            packageToApp = systemPackages.associateWith { AppRef(it) },
            rulesByApp = systemPackages.associate { AppRef(it) to alwaysOn },
            grantsByApp = emptyMap(),
        )
        val pipeline = pipeline()

        systemPackages.forEach { packageName ->
            val result = pipeline.onWindowStateChanged(
                packageName = packageName,
                className = "$packageName.MainActivity",
                snapshot = hostileSnapshot,
            )
            assertTrue(
                "$packageName reached rule evaluation as ${result.transition}",
                result.transition in setOf(
                    TransitionOutcome.IGNORED_SYSTEM,
                    TransitionOutcome.IGNORED_OVERLAY,
                ),
            )
            assertFalse("$packageName was blocked", result.shouldBlock)
        }
    }

    @Test
    fun ourOwnPackageIsNeverBlockedEvenWhenClaimedByARule() {
        val self = AppRef(context.packageName)
        val snapshot = ProtectionSnapshot(
            packageToApp = mapOf(context.packageName to self),
            rulesByApp = mapOf(self to listOf(RestrictionRule.AlwaysOn("always", enabled = true))),
            grantsByApp = emptyMap(),
        )

        val result = pipeline().onWindowStateChanged(
            packageName = context.packageName,
            className = "com.blocksocial.debug.TokenPreviewActivity",
            snapshot = snapshot,
        )

        assertEquals(TransitionOutcome.IGNORED_SELF, result.transition)
        assertFalse(result.shouldBlock)
    }

    @Test
    fun theCatalogIsPackagedAndReadable() {
        val entries = SupportedAppCatalog(context).load()

        assertEquals(10, entries.size)
        assertTrue(entries.any { it.ref == AppRef("instagram") })
        assertTrue(
            "TikTok must carry both of its package names",
            entries.single { it.ref == AppRef("tiktok") }.packageNames.size == 2,
        )
    }

    @Test
    fun theCallbackPathReadsNoDiskAndNoDatabase() {
        val pipeline = pipeline()
        val snapshot = ProtectionSnapshot(
            packageToApp = mapOf("com.instagram.android" to AppRef("instagram")),
            rulesByApp = mapOf(AppRef("instagram") to listOf(RestrictionRule.AlwaysOn("r", true))),
            grantsByApp = emptyMap(),
        )

        pipeline.onWindowStateChanged("com.instagram.android", "com.instagram.Main", snapshot)
        pipeline.forgetForeground()

        instrumentation.runOnMainSync {
            val previous = StrictMode.getThreadPolicy()
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .penaltyDeath()
                    .build(),
            )
            try {
                repeat(50) {
                    pipeline.onWindowStateChanged("com.instagram.android", "com.instagram.Main", snapshot)
                    pipeline.forgetForeground()
                }
            } finally {
                StrictMode.setThreadPolicy(previous)
            }
        }
    }
}
