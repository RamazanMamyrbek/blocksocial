package com.blocksocial.spike.a03

data class InstalledApp(
    val packageName: String,
    val label: String
)

fun interface InstalledAppLookup {
    fun find(packageName: String): InstalledApp?
}

sealed interface CatalogItemState {
    data class Installed(val packageName: String, val label: String) : CatalogItemState
    data object NotInstalled : CatalogItemState
}

data class ResolvedCatalogItem(
    val entry: CatalogEntry,
    val state: CatalogItemState
)

class CatalogResolver(private val lookup: InstalledAppLookup) {

    fun resolve(catalog: SupportedAppCatalog): List<ResolvedCatalogItem> =
        catalog.entries.map { entry ->
            val installed = entry.packageNames.firstNotNullOfOrNull(lookup::find)
            ResolvedCatalogItem(
                entry = entry,
                state = installed
                    ?.let { CatalogItemState.Installed(it.packageName, it.label) }
                    ?: CatalogItemState.NotInstalled
            )
        }
}
