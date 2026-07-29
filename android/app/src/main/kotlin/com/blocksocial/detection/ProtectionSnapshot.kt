package com.blocksocial.detection

import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.RestrictionRule
import com.blocksocial.core.model.TemporaryAccessGrant

data class BlockCounts(val opens: Int, val endedHere: Int) {
    companion object {
        val None = BlockCounts(opens = 0, endedHere = 0)
    }
}

data class ProtectionSnapshot(
    val packageToApp: Map<String, AppRef>,
    val rulesByApp: Map<AppRef, List<RestrictionRule>>,
    val grantsByApp: Map<AppRef, TemporaryAccessGrant>,
    val displayNames: Map<AppRef, String> = emptyMap(),
    val todayCounts: Map<AppRef, BlockCounts> = emptyMap(),
) {
    companion object {
        val Empty = ProtectionSnapshot(
            packageToApp = emptyMap(),
            rulesByApp = emptyMap(),
            grantsByApp = emptyMap(),
        )
    }
}
