package com.blocksocial.spike.a04

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object SystemPackages {

    private val CRITICAL = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.phone",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.android.emergency",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller"
    )

    fun resolve(context: Context): Set<String> = CRITICAL + resolveHomePackages(context)

    private fun resolveHomePackages(context: Context): Set<String> {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return context.packageManager
            .queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            .mapNotNull { it.activityInfo?.packageName }
            .toSet()
    }
}
