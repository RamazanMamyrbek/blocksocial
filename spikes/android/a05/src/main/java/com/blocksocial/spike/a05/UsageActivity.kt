package com.blocksocial.spike.a05

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.ZoneId

class UsageActivity : ComponentActivity() {

    private var state by mutableStateOf(UsageScreenState(UsageAccessState.DENIED, emptyList(), null))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UsageScreen(
                state = state,
                onOpenSettings = {
                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        state = measure()
    }

    private fun measure(): UsageScreenState {
        val access = UsageAccess.state(this)
        if (access == UsageAccessState.DENIED) {
            Log.i(TAG, "usageAccess=DENIED")
            return UsageScreenState(access, emptyList(), null)
        }

        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val window = LocalDayWindows.containing(now, zone)
        val events = runCatching {
            UsageStatsReader.from(this).readEvents(window.startMillis - LOOKBACK_MILLIS, now)
        }.getOrElse { failure ->
            Log.i(TAG, "usageAccess=GRANTED readFailed=${failure.javaClass.simpleName}")
            emptyList()
        }
        val usage = ForegroundTimeAccumulator
            .accumulate(events, window.startMillis, now)
            .values
            .filter { it.totalMillis > 0 }
            .sortedByDescending { it.totalMillis }

        Log.i(
            TAG,
            "usageAccess=GRANTED zone=$zone windowStart=${window.startMillis} now=$now " +
                "events=${events.size} packagesWithTime=${usage.size}"
        )
        val bucketed = runCatching {
            UsageStatsReader.from(this).readBucketedForegroundMillis(window.startMillis, now)
        }.getOrDefault(emptyMap())

        usage.forEach {
            Log.i(
                TAG,
                "package=${it.packageName} totalMillis=${it.totalMillis} " +
                    "bucketedMillis=${bucketed[it.packageName] ?: -1} " +
                    "confidence=${it.confidence} sessions=${it.sessionCount}"
            )
        }

        intent?.getStringExtra(EXTRA_TRACE_PACKAGE)?.let { traced ->
            UsageStatsReader.from(this)
                .readRawEventTypes(window.startMillis, now, traced)
                .takeLast(40)
                .forEach { (timestamp, type) ->
                    Log.i(TAG, "raw package=$traced at=$timestamp eventType=$type")
                }
        }

        return UsageScreenState(access, usage, window)
    }

    private companion object {
        const val TAG = "SpikeA05"
        const val EXTRA_TRACE_PACKAGE = "trace_package"
        const val LOOKBACK_MILLIS = 24 * 60 * 60 * 1000L
    }
}
