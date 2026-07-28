package com.blocksocial.spike.a03

import org.json.JSONObject

data class CatalogEntry(
    val id: String,
    val displayName: String,
    val packageNames: List<String>
)

data class SupportedAppCatalog(
    val schemaVersion: Int,
    val entries: List<CatalogEntry>
)

class CatalogFormatException(message: String) : Exception(message)

object SupportedAppCatalogParser {

    const val SUPPORTED_SCHEMA_VERSION = 1

    fun parse(json: String): SupportedAppCatalog {
        val root = JSONObject(json)
        val schemaVersion = root.optInt("schemaVersion", -1)
        if (schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            throw CatalogFormatException(
                "catalog schemaVersion $schemaVersion is not supported, expected $SUPPORTED_SCHEMA_VERSION"
            )
        }
        val apps = root.optJSONArray("apps")
            ?: throw CatalogFormatException("catalog has no apps array")

        val entries = (0 until apps.length()).map { index ->
            val app = apps.getJSONObject(index)
            val id = app.optString("id").ifBlank {
                throw CatalogFormatException("catalog entry $index has no id")
            }
            val displayName = app.optString("displayName").ifBlank {
                throw CatalogFormatException("catalog entry $id has no displayName")
            }
            val packages = app.optJSONArray("packageNames")
                ?: throw CatalogFormatException("catalog entry $id has no packageNames")
            val packageNames = (0 until packages.length()).map { packages.getString(it) }
            if (packageNames.isEmpty()) {
                throw CatalogFormatException("catalog entry $id lists no package")
            }
            CatalogEntry(id, displayName, packageNames)
        }

        val duplicateIds = entries.groupBy { it.id }.filterValues { it.size > 1 }.keys
        if (duplicateIds.isNotEmpty()) {
            throw CatalogFormatException("catalog has duplicate ids: ${duplicateIds.joinToString()}")
        }
        val duplicatePackages = entries.flatMap { it.packageNames }
            .groupBy { it }
            .filterValues { it.size > 1 }
            .keys
        if (duplicatePackages.isNotEmpty()) {
            throw CatalogFormatException(
                "catalog maps one package to several entries: ${duplicatePackages.joinToString()}"
            )
        }

        return SupportedAppCatalog(schemaVersion, entries)
    }
}
