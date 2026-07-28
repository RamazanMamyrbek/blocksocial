package com.blocksocial.spike.a03

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogResolverTest {

    private val instagram = CatalogEntry("instagram", "Instagram", listOf("com.instagram.android"))
    private val tiktok = CatalogEntry(
        "tiktok",
        "TikTok",
        listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")
    )
    private val catalog = SupportedAppCatalog(1, listOf(instagram, tiktok))

    private fun lookupOf(vararg installed: Pair<String, String>) = InstalledAppLookup { packageName ->
        installed.firstOrNull { it.first == packageName }
            ?.let { InstalledApp(it.first, it.second) }
    }

    @Test
    fun `an installed entry carries the package and the label the device reports`() {
        val resolved = CatalogResolver(lookupOf("com.instagram.android" to "Instagram"))
            .resolve(catalog)

        val state = resolved.first { it.entry.id == "instagram" }.state
        assertEquals(CatalogItemState.Installed("com.instagram.android", "Instagram"), state)
    }

    @Test
    fun `a missing package resolves to not installed rather than failing`() {
        val resolved = CatalogResolver(lookupOf()).resolve(catalog)

        assertTrue(resolved.all { it.state == CatalogItemState.NotInstalled })
        assertEquals(catalog.entries.size, resolved.size)
    }

    @Test
    fun `an alias package satisfies the entry`() {
        val resolved = CatalogResolver(lookupOf("com.ss.android.ugc.trill" to "TikTok Lite"))
            .resolve(catalog)

        val state = resolved.first { it.entry.id == "tiktok" }.state
        assertEquals(CatalogItemState.Installed("com.ss.android.ugc.trill", "TikTok Lite"), state)
    }

    @Test
    fun `the first matching alias wins`() {
        val resolved = CatalogResolver(
            lookupOf(
                "com.zhiliaoapp.musically" to "TikTok",
                "com.ss.android.ugc.trill" to "TikTok Lite"
            )
        ).resolve(catalog)

        val state = resolved.first { it.entry.id == "tiktok" }.state
        assertEquals(CatalogItemState.Installed("com.zhiliaoapp.musically", "TikTok"), state)
    }

    @Test
    fun `an installed application outside the catalog is never returned`() {
        val resolved = CatalogResolver(lookupOf("com.android.chrome" to "Chrome")).resolve(catalog)

        assertTrue(resolved.none { it.state is CatalogItemState.Installed })
        assertTrue(resolved.map { it.entry.id }.toSet() == setOf("instagram", "tiktok"))
    }

    @Test
    fun `resolution preserves catalog order`() {
        val resolved = CatalogResolver(lookupOf()).resolve(catalog)

        assertEquals(listOf("instagram", "tiktok"), resolved.map { it.entry.id })
    }
}
