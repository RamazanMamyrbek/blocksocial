package com.blocksocial.catalog

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NoHardcodedTextTest {

    private val repositoryRoot: File = generateSequence(File(".").absoluteFile) { it.parentFile }
        .first { File(it, "shared/supported-app-catalog/catalog.json").isFile }

    private val shippedSources: List<File> = File(repositoryRoot, "android")
        .listFiles()
        .orEmpty()
        .flatMap { module -> File(module, "src/main/kotlin").walkTopDown().toList() }
        .filter { it.extension == "kt" }

    private fun offenders(pattern: Regex): List<String> = shippedSources.flatMap { source ->
        source.readLines()
            .withIndex()
            .filter { (_, line) -> pattern.containsMatchIn(line) }
            .map { (index, line) -> "${source.name}:${index + 1}  ${line.trim()}" }
    }

    @Test
    fun theSourcesAreNotEmptyOrThisTestProvesNothing() {
        assertTrue("no shipped Kotlin sources were found", shippedSources.size > 20)
    }

    @Test
    fun noTextComposableIsGivenALiteral() {
        val found = offenders(Regex("""\bText\(\s*"[^"]"""))

        assertTrue(
            "user-facing text belongs in strings.xml:\n" + found.joinToString("\n"),
            found.isEmpty(),
        )
    }

    @Test
    fun noContentDescriptionIsGivenALiteral() {
        val found = offenders(Regex("""contentDescription\s*=\s*"[^"]"""))

        assertTrue(
            "a screen reader reads this, so it must be translatable:\n" + found.joinToString("\n"),
            found.isEmpty(),
        )
    }

    @Test
    fun noLabelOrPlaceholderIsGivenALiteral() {
        val found = offenders(Regex("""\b(label|placeholder|title)\s*=\s*"[^"]"""))

        assertTrue(
            "labels are read aloud and translated:\n" + found.joinToString("\n"),
            found.isEmpty(),
        )
    }

    @Test
    fun everyShippedStringHasARussianTranslation() {
        val defaults = stringFiles("values")
        assertTrue("no shipped string resources were found", defaults.size >= 6)

        defaults.forEach { default ->
            val translated = File(default.parentFile.parentFile, "values-ru/${default.name}")
            assertTrue("${default.path} has no Russian counterpart", translated.isFile)

            val missing = namesIn(default) - namesIn(translated)
            assertTrue("${translated.path} is missing: ${missing.sorted()}", missing.isEmpty())

            val extra = namesIn(translated) - namesIn(default)
            assertTrue(
                "${translated.path} translates strings that no longer exist: ${extra.sorted()}",
                extra.isEmpty(),
            )
        }
    }

    @Test
    fun theRussianPluralsCoverTheCategoriesRussianActuallyUses() {
        val required = setOf("one", "few", "many", "other")

        stringFiles("values-ru").forEach { file ->
            Regex("""<plurals name="([^"]+)">(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL)
                .findAll(file.readText())
                .forEach { plural ->
                    val quantities = Regex("""quantity="([^"]+)"""")
                        .findAll(plural.groupValues[2])
                        .map { it.groupValues[1] }
                        .toSet()
                    assertTrue(
                        "${plural.groupValues[1]} in ${file.path} covers only $quantities",
                        quantities.containsAll(required),
                    )
                }
        }
    }

    private fun stringFiles(qualifier: String): List<File> = File(repositoryRoot, "android")
        .listFiles()
        .orEmpty()
        .flatMap { module ->
            File(module, "src/main/res/$qualifier").listFiles().orEmpty().toList()
        }
        .filter { it.name.startsWith("strings") && it.extension == "xml" }

    private fun namesIn(file: File): Set<String> =
        Regex("""<(?:string|plurals)([^>]*)>""")
            .findAll(file.readText())
            .filterNot { it.groupValues[1].contains("translatable=\"false\"") }
            .mapNotNull { Regex("""name="([^"]+)"""").find(it.groupValues[1])?.groupValues?.get(1) }
            .toSet()
}
