package com.blocksocial.lite.data

import android.content.Context
import android.content.pm.PackageManager
import org.json.JSONObject

data class CatalogApp(
    val id: String,
    val displayName: String,
    val packageNames: List<String>,
)

class AppCatalog(private val context: Context) {

    private val all: List<CatalogApp> by lazy { read() }

    fun installed(): List<CatalogApp> = all.filter { app ->
        app.packageNames.any { isInstalled(it) }
    }

    fun packageToApp(): Map<String, String> = all
        .flatMap { app -> app.packageNames.map { it to app.id } }
        .toMap()

    fun displayNames(): Map<String, String> = all.associate { it.id to it.displayName }

    private fun read(): List<CatalogApp> {
        val document = JSONObject(context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() })
        val apps = document.getJSONArray("apps")
        return (0 until apps.length()).map { index ->
            val app = apps.getJSONObject(index)
            val packageNames = app.getJSONArray("packageNames")
            CatalogApp(
                id = app.getString("id"),
                displayName = app.getString("displayName"),
                packageNames = (0 until packageNames.length()).map { packageNames.getString(it) },
            )
        }
    }

    private fun isInstalled(packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (missing: PackageManager.NameNotFoundException) {
        false
    }

    private companion object {
        const val ASSET_NAME = "catalog.json"
    }
}
