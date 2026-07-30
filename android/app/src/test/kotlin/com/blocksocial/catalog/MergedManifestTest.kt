package com.blocksocial.catalog

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MergedManifestTest {

    private val repositoryRoot: File = generateSequence(File(".").absoluteFile) { it.parentFile }
        .first { File(it, "shared/supported-app-catalog/catalog.json").isFile }

    private fun mergedManifests(): List<File> {
        val merged = File(repositoryRoot, "android/app/build/intermediates/merged_manifests")
        return merged.walkTopDown().filter { it.name == "AndroidManifest.xml" }.toList()
    }

    @Test
    fun theMergedManifestNeverAsksToSeeEveryInstalledApplication() {
        val manifests = mergedManifests()
        assertTrue("no merged manifest found; build the app before running this", manifests.isNotEmpty())

        manifests.forEach { manifest ->
            val text = manifest.readText()
            assertFalse(
                "${manifest.path} requests QUERY_ALL_PACKAGES",
                text.contains("QUERY_ALL_PACKAGES"),
            )
            assertFalse(
                "${manifest.path} requests SYSTEM_ALERT_WINDOW",
                text.contains("SYSTEM_ALERT_WINDOW"),
            )
        }
    }

    @Test
    fun nothingRunsAsAPermanentForegroundService() {
        val manifests = mergedManifests()
        assertTrue("no merged manifest found; build the app before running this", manifests.isNotEmpty())

        manifests.forEach { manifest ->
            val text = manifest.readText()
            listOf(
                "android.permission.FOREGROUND_SERVICE",
                "foregroundServiceType",
            ).forEach { marker ->
                assertFalse("${manifest.path} declares $marker", text.contains(marker))
            }
        }

        val sources = File(repositoryRoot, "android/app/src/main/kotlin")
            .walkTopDown()
            .filter { it.extension == "kt" }
            .toList()

        sources.forEach { source ->
            assertFalse(
                "${source.name} calls startForeground",
                source.readText().contains("startForeground"),
            )
        }
    }
}
