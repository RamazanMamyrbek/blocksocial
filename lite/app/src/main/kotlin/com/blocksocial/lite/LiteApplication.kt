package com.blocksocial.lite

import android.app.Application
import android.content.Context
import com.blocksocial.lite.data.AppCatalog
import com.blocksocial.lite.data.LimitStore
import com.blocksocial.lite.detection.ServiceHeartbeat
import com.blocksocial.lite.usage.UsageStatsReader

class Container(context: Context) {
    val catalog = AppCatalog(context)
    val limits = LimitStore(context)
    val usage = UsageStatsReader(context)
    val heartbeat = ServiceHeartbeat()
}

class LiteApplication : Application() {

    val container: Container by lazy { Container(this) }
}

val Context.container: Container
    get() = (applicationContext as LiteApplication).container
