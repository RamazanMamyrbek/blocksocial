package com.blocksocial.detection

import android.util.Log
import com.blocksocial.BuildConfig

object DetectionLog {

    const val TAG = "BlockSocialDetection"

    fun lifecycle(state: String, detail: String = "") {
        write("event=lifecycle state=$state${if (detail.isEmpty()) "" else " $detail"}")
    }

    fun decision(result: DetectionResult, latencyMillis: Long) {
        write(decisionLine(result, latencyMillis))
    }

    internal fun decisionLine(result: DetectionResult, latencyMillis: Long): String {
        val app = result.app?.value ?: "none"
        val reasons = result.decision?.allReasons?.joinToString("|") { it.name } ?: "none"
        val primary = result.decision?.primaryReason?.name ?: "none"
        val grant = result.decision?.bypass?.evaluation?.name ?: "none"
        return "event=decision transition=${result.transition} app=$app block=${result.shouldBlock} " +
            "primaryReason=$primary allReasons=$reasons grant=$grant latencyMillis=$latencyMillis"
    }

    private fun write(line: String) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, line)
        }
    }
}
