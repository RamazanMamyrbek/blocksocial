package com.blocksocial.lite

import android.app.Application
import android.content.Context
import com.blocksocial.lite.data.AppCatalog
import com.blocksocial.lite.data.BatteryExemption
import com.blocksocial.lite.data.GuardSetting
import com.blocksocial.lite.data.LimitStore
import com.blocksocial.lite.detection.DecisionLog
import com.blocksocial.lite.detection.LimitSnapshot
import com.blocksocial.lite.detection.ServiceHeartbeat
import com.blocksocial.lite.usage.UsageStatsReader
import com.blocksocial.lite.usage.UsageToday

class Container(context: Context) {
    val catalog = AppCatalog(context)
    val limits = LimitStore(context)
    val guard = GuardSetting(context)
    val usage = UsageStatsReader(context)
    val battery = BatteryExemption(context)
    val heartbeat = ServiceHeartbeat()
    val decisions = DecisionLog()

    @Volatile
    var lastKnownSnapshot: LimitSnapshot = LimitSnapshot.Empty

    @Volatile
    var lastKnownUsage: UsageToday = UsageToday.Unavailable
}

class LiteApplication : Application() {

    val container: Container by lazy { Container(this) }
}

val Context.container: Container
    get() = (applicationContext as LiteApplication).container
