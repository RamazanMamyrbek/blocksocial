package com.blocksocial.feature.appselection

import android.content.Context
import android.content.pm.PackageManager
import com.blocksocial.core.data.catalog.SupportedApp
import com.blocksocial.core.data.catalog.SupportedAppCatalog
import com.blocksocial.core.data.repository.RestrictedAppRepository
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.RestrictedApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSelectionController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val catalog: SupportedAppCatalog,
    private val restrictedApps: RestrictedAppRepository,
) {

    fun rows(): Flow<List<AppSelectionRow>> = restrictedApps.observeAll()
        .map { stored -> buildRows(stored.associateBy { it.ref }) }
        .flowOn(Dispatchers.IO)

    suspend fun setSelected(ref: AppRef, selected: Boolean) {
        val known = restrictedApps.find(ref)
        if (known == null) {
            val entry = catalog.load().firstOrNull { it.ref == ref } ?: return
            restrictedApps.save(
                RestrictedApp(ref = ref, displayName = installedLabel(entry) ?: entry.displayName, selected = selected),
                Instant.now(),
            )
        } else {
            restrictedApps.setSelected(ref, selected)
        }
    }

    private fun buildRows(stored: Map<AppRef, RestrictedApp>): List<AppSelectionRow> =
        catalog.load().map { entry ->
            val label = installedLabel(entry)
            AppSelectionRow(
                ref = entry.ref,
                displayName = label ?: entry.displayName,
                installState = if (label == null) InstallState.NOT_INSTALLED else InstallState.INSTALLED,
                selected = stored[entry.ref]?.selected == true && label != null,
            )
        }.sortedWith(compareBy({ it.installState != InstallState.INSTALLED }, { it.displayName }))

    private fun installedLabel(entry: SupportedApp): String? {
        val packageManager = context.packageManager
        entry.packageNames.forEach { packageName ->
            runCatching {
                val info = packageManager.getApplicationInfo(packageName, 0)
                return packageManager.getApplicationLabel(info).toString()
            }
        }
        return null
    }
}
