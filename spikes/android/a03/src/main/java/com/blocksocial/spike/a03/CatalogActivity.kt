package com.blocksocial.spike.a03

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.core.graphics.drawable.toBitmap

class CatalogActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val catalog = SupportedAppCatalogParser.parse(
            assets.open(CATALOG_ASSET).bufferedReader().use { it.readText() }
        )
        val resolved = CatalogResolver(PackageManagerAppLookup(packageManager)).resolve(catalog)

        resolved.forEach { item ->
            Log.i(TAG, "id=${item.entry.id} state=${item.state.javaClass.simpleName}")
        }

        setContent {
            CatalogScreen(items = resolved, iconFor = ::iconPainter)
        }
    }

    private fun iconPainter(packageName: String): Painter? = runCatching {
        BitmapPainter(packageManager.getApplicationIcon(packageName).toBitmap().asImageBitmap())
    }.getOrNull()

    private companion object {
        const val TAG = "SpikeA03"
        const val CATALOG_ASSET = "catalog.json"
    }
}
