package com.blocksocial.detection

import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.RestrictionRule
import com.blocksocial.core.model.TemporaryAccessGrant

data class ProtectionSnapshot(
    val packageToApp: Map<String, AppRef>,
    val rulesByApp: Map<AppRef, List<RestrictionRule>>,
    val grantsByApp: Map<AppRef, TemporaryAccessGrant>,
) {
    companion object {
        val Empty = ProtectionSnapshot(emptyMap(), emptyMap(), emptyMap())
    }
}
