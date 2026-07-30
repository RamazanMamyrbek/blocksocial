package com.blocksocial.catalog

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AccessibilityGuardsTest {

    private val repositoryRoot: File = generateSequence(File(".").absoluteFile) { it.parentFile }
        .first { File(it, "shared/supported-app-catalog/catalog.json").isFile }

    private val shippedSources: List<File> = File(repositoryRoot, "android")
        .listFiles()
        .orEmpty()
        .flatMap { module -> File(module, "src/main/kotlin").walkTopDown().toList() }
        .filter { it.extension == "kt" }

    private fun sourcesMatching(pattern: Regex): List<File> =
        shippedSources.filter { pattern.containsMatchIn(it.readText()) }

    @Test
    fun noTextIsAllowedToTruncate() {
        val found = sourcesMatching(Regex("""\bmaxLines\s*=|TextOverflow|softWrap\s*=\s*false"""))

        assertTrue(
            "text that truncates cannot survive Russian at the largest font scale: " +
                found.map { it.name },
            found.isEmpty(),
        )
    }

    @Test
    fun anythingThatAnimatesReadsTheReduceMotionTokens() {
        val animates = Regex("""\banimate[A-Z]\w*|AnimatedVisibility|Crossfade|\btween\(|\bspring\(""")
        val offenders = sourcesMatching(animates)
            .filterNot { it.readText().contains("LocalMotionDurations") }

        assertTrue(
            "these animate without honouring Reduce Motion: " + offenders.map { it.name },
            offenders.isEmpty(),
        )
    }

    @Test
    fun everyStatusShapeIsDistinctSoMeaningNeverRestsOnColourAlone() {
        val marks = File(
            repositoryRoot,
            "android/feature-onboarding/src/main/kotlin/com/blocksocial/feature/onboarding/" +
                "ProtectionHealthScreen.kt",
        ).readText()
        val statusMark = marks.substringAfter("private fun StatusMark").substringBefore("\n}")

        listOf("RoundedCornerShape", "TriangleShape", "CircleShape", "border").forEach { shape ->
            assertTrue("the status marks no longer use $shape", statusMark.contains(shape))
        }
    }

    @Test
    fun theBlockOverlayKeepsAWindowTitleForScreenReaders() {
        val controller = File(
            repositoryRoot,
            "android/app/src/main/kotlin/com/blocksocial/block/BlockOverlayController.kt",
        ).readText()

        assertTrue(
            "the overlay window must carry a title; accessibilityTitle is not public API",
            controller.contains("setTitle") || controller.contains("title ="),
        )
        assertTrue(
            "the window title must come from resources so a screen reader reads it translated",
            controller.contains("block_window_title"),
        )
    }
}
