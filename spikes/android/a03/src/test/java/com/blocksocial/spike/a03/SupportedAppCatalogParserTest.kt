package com.blocksocial.spike.a03

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportedAppCatalogParserTest {

    private fun shippedCatalogJson(): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream("catalog.json")) {
            "catalog.json is not on the test classpath"
        }.bufferedReader().use { it.readText() }

    @Test
    fun `the shipped catalog parses`() {
        val catalog = SupportedAppCatalogParser.parse(shippedCatalogJson())

        assertEquals(SupportedAppCatalogParser.SUPPORTED_SCHEMA_VERSION, catalog.schemaVersion)
        assertTrue(catalog.entries.size >= 10)
    }

    @Test
    fun `the shipped catalog covers the applications the plan requires`() {
        val ids = SupportedAppCatalogParser.parse(shippedCatalogJson()).entries.map { it.id }.toSet()

        val required = setOf(
            "instagram", "tiktok", "youtube", "facebook", "x",
            "reddit", "snapchat", "telegram", "discord", "vk"
        )
        assertTrue("missing: ${required - ids}", ids.containsAll(required))
    }

    @Test
    fun `every entry carries an id, a display name and at least one package`() {
        SupportedAppCatalogParser.parse(shippedCatalogJson()).entries.forEach { entry ->
            assertTrue(entry.id.isNotBlank())
            assertTrue(entry.displayName.isNotBlank())
            assertTrue(entry.packageNames.isNotEmpty())
        }
    }

    @Test
    fun `package aliases are supported`() {
        val entries = SupportedAppCatalogParser.parse(shippedCatalogJson()).entries

        assertTrue(entries.any { it.packageNames.size > 1 })
    }

    @Test
    fun `an unsupported schema version is rejected`() {
        val json = """{"schemaVersion": 99, "apps": []}"""

        val error = assertThrows(CatalogFormatException::class.java) {
            SupportedAppCatalogParser.parse(json)
        }
        assertTrue(error.message!!.contains("99"))
    }

    @Test
    fun `a missing apps array is rejected`() {
        assertThrows(CatalogFormatException::class.java) {
            SupportedAppCatalogParser.parse("""{"schemaVersion": 1}""")
        }
    }

    @Test
    fun `an entry without a package is rejected`() {
        val json = """
            {"schemaVersion": 1, "apps": [
              {"id": "x", "displayName": "X", "packageNames": []}
            ]}
        """.trimIndent()

        assertThrows(CatalogFormatException::class.java) {
            SupportedAppCatalogParser.parse(json)
        }
    }

    @Test
    fun `duplicate ids are rejected`() {
        val json = """
            {"schemaVersion": 1, "apps": [
              {"id": "x", "displayName": "X", "packageNames": ["a"]},
              {"id": "x", "displayName": "X again", "packageNames": ["b"]}
            ]}
        """.trimIndent()

        assertThrows(CatalogFormatException::class.java) {
            SupportedAppCatalogParser.parse(json)
        }
    }

    @Test
    fun `one package mapped to two entries is rejected`() {
        val json = """
            {"schemaVersion": 1, "apps": [
              {"id": "one", "displayName": "One", "packageNames": ["com.example"]},
              {"id": "two", "displayName": "Two", "packageNames": ["com.example"]}
            ]}
        """.trimIndent()

        assertThrows(CatalogFormatException::class.java) {
            SupportedAppCatalogParser.parse(json)
        }
    }
}
