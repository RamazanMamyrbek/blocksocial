package com.blocksocial.core.data.catalog

import android.content.Context
import com.blocksocial.core.model.AppRef
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class SupportedApp(
    val ref: AppRef,
    val displayName: String,
    val packageNames: List<String>,
)

@Singleton
class SupportedAppCatalog @Inject constructor(@ApplicationContext private val context: Context) {

    fun load(): List<SupportedApp> {
        val document = JSONObject(context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() })
        val apps = document.getJSONArray("apps")
        return (0 until apps.length()).map { index ->
            val app = apps.getJSONObject(index)
            val packageNames = app.getJSONArray("packageNames")
            SupportedApp(
                ref = AppRef(app.getString("id")),
                displayName = app.getString("displayName"),
                packageNames = (0 until packageNames.length()).map { packageNames.getString(it) },
            )
        }
    }

    private companion object {
        const val ASSET_NAME = "catalog.json"
    }
}
