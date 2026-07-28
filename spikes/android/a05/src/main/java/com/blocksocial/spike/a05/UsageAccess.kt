package com.blocksocial.spike.a05

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process

enum class UsageAccessState {
    GRANTED,
    DENIED
}

object UsageAccess {

    fun state(context: Context): UsageAccessState {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return if (mode == AppOpsManager.MODE_ALLOWED) {
            UsageAccessState.GRANTED
        } else {
            UsageAccessState.DENIED
        }
    }
}
