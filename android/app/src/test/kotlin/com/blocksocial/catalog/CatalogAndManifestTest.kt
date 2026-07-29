package com.blocksocial.catalog

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class CatalogAndManifestTest {

    private val repositoryRoot: File = generateSequence(File(".").absoluteFile) { it.parentFile }
        .first { File(it, "shared/supported-app-catalog/catalog.json").isFile }

    private val catalog = JSONObject(
        File(repositoryRoot, "shared/supported-app-catalog/catalog.json").readText(),
    )

    private fun catalogPackages(): Set<String> {
        val apps = catalog.getJSONArray("apps")
        return (0 until apps.length()).flatMap { index ->
            val packageNames = apps.getJSONObject(index).getJSONArray("packageNames")
            (0 until packageNames.length()).map { packageNames.getString(it) }
        }.toSet()
    }

    private fun manifestQueriedPackages(): Set<String> {
        val manifest = File(repositoryRoot, "android/app/src/main/AndroidManifest.xml")
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(manifest)
        val nodes = document.getElementsByTagName("package")
        return (0 until nodes.length)
            .mapNotNull { nodes.item(it).attributes.getNamedItem("android:name")?.nodeValue }
            .toSet()
    }

    @Test
    fun everyCatalogPackageIsDeclaredInTheManifest() {
        val missing = catalogPackages() - manifestQueriedPackages()
        assertTrue(
            "these catalog packages resolve to not-installed on every device: $missing",
            missing.isEmpty(),
        )
    }

    @Test
    fun theManifestDeclaresNothingBeyondTheCatalog() {
        val extra = manifestQueriedPackages() - catalogPackages()
        assertTrue("these packages are queried but are not in the catalog: $extra", extra.isEmpty())
    }

    @Test
    fun theCatalogUsesStableIdentifiersRatherThanPackageNames() {
        val apps = catalog.getJSONArray("apps")
        (0 until apps.length()).forEach { index ->
            val app = apps.getJSONObject(index)
            val id = app.getString("id")
            assertTrue("catalog id $id looks like a package name", !id.contains('.'))
        }
    }

    @Test
    fun noPackageIsClaimedByTwoCatalogEntries() {
        val apps = catalog.getJSONArray("apps")
        val declared = (0 until apps.length()).flatMap { index ->
            val packageNames = apps.getJSONObject(index).getJSONArray("packageNames")
            (0 until packageNames.length()).map { packageNames.getString(it) }
        }
        assertEquals(declared.size, declared.toSet().size)
    }
}
