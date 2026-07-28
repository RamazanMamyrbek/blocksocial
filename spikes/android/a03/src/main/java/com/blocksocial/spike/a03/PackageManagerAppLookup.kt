package com.blocksocial.spike.a03

import android.content.pm.PackageManager

class PackageManagerAppLookup(
    private val packageManager: PackageManager
) : InstalledAppLookup {

    override fun find(packageName: String): InstalledApp? = try {
        val info = packageManager.getApplicationInfo(packageName, 0)
        InstalledApp(
            packageName = packageName,
            label = packageManager.getApplicationLabel(info).toString()
        )
    } catch (notInstalled: PackageManager.NameNotFoundException) {
        null
    }
}
