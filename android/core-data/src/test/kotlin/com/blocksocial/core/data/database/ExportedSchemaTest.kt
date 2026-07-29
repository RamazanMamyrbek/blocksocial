package com.blocksocial.core.data.database

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExportedSchemaTest {

    private val schemaFile = generateSequence(File(".").absoluteFile) { it.parentFile }
        .map { File(it, "android/core-data/schemas/${BlockSocialDatabase::class.java.name}/1.json") }
        .first { it.isFile }

    private val schema = JSONObject(schemaFile.readText()).getJSONObject("database")

    private fun tables(): Map<String, JSONObject> {
        val entities = schema.getJSONArray("entities")
        return (0 until entities.length())
            .map { entities.getJSONObject(it) }
            .associateBy { it.getString("tableName") }
    }

    private fun indexedColumnsOf(tableName: String): List<List<String>> {
        val indices = tables().getValue(tableName).optJSONArray("indices") ?: return emptyList()
        return (0 until indices.length()).map { index ->
            val columns = indices.getJSONObject(index).getJSONArray("columnNames")
            (0 until columns.length()).map { columns.getString(it) }
        }
    }

    @Test
    fun theSchemaIsExportedForTheDeclaredVersion() {
        assertEquals(BlockSocialDatabase.VERSION, schema.getInt("version"))
    }

    @Test
    fun everyTableTheApplicationNeedsIsPresent() {
        assertEquals(
            setOf(
                "restricted_app",
                "restriction_rule",
                "temporary_access_grant",
                "block_event",
                "usage_session",
                "daily_statistics",
            ),
            tables().keys,
        )
    }

    @Test
    fun theDetectionPathQueriesAreIndexed() {
        assertTrue(
            "rules are looked up per application on every detection",
            listOf("appCatalogId", "enabled") in indexedColumnsOf("restriction_rule"),
        )
        assertTrue(
            "usage is summed per application over a day window",
            listOf("appCatalogId", "fromEpochMillis") in indexedColumnsOf("usage_session"),
        )
        assertTrue(
            "history is read newest first",
            listOf("occurredAtEpochMillis") in indexedColumnsOf("block_event"),
        )
    }

    @Test
    fun aGrantIsKeyedByItsApplicationSoItCannotBeDuplicated() {
        val primaryKey = tables().getValue("temporary_access_grant").getJSONObject("primaryKey")
        val columns = primaryKey.getJSONArray("columnNames")
        assertEquals(1, columns.length())
        assertEquals("appCatalogId", columns.getString(0))
    }

    @Test
    fun nothingStoresUserContent() {
        val allowed = Regex(
            "catalogId|appCatalogId|displayName|selected|createdAtEpochMillis|updatedAtEpochMillis|" +
                "id|mode|enabled|daysOfWeek|startLocalTime|endLocalTime|dailyLimitMinutes|" +
                "grantedAtWallClockEpochMillis|grantedAtMonotonicMillis|durationMinutes|sourceBlockEventId|" +
                "occurredAtEpochMillis|zoneId|primaryReason|allReasons|userAction|bypassDurationMinutes|" +
                "platform|eventSchemaVersion|fromEpochMillis|toEpochMillis|localDate|interventions|" +
                "stayedFocused|bypassed|measuredMinutes",
        )
        tables().forEach { (tableName, table) ->
            val fields = table.getJSONArray("fields")
            (0 until fields.length()).forEach { index ->
                val columnName = fields.getJSONObject(index).getString("columnName")
                assertTrue(
                    "$tableName.$columnName is not an approved column",
                    allowed.matches(columnName),
                )
            }
        }
    }
}
