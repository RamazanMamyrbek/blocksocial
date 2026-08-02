package com.blocksocial.lite.detection

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object SystemPackages {

    private val NEVER_BLOCKED = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.phone",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.android.emergency",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
    )

    val NEVER_TAKES_THE_FOREGROUND = setOf("com.android.systemui")

    fun resolve(context: Context): Set<String> = NEVER_BLOCKED + resolveHomePackages(context)

    private fun resolveHomePackages(context: Context): Set<String> {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return context.packageManager
            .queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            .mapNotNull { it.activityInfo?.packageName }
            .toSet()
    }
}
