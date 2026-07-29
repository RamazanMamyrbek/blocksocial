package com.blocksocial.block

import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.RuleMode
import java.time.Instant
import java.time.ZoneId

data class BlockPresentation(
    val app: AppRef,
    val appDisplayName: String,
    val primaryReason: RuleMode,
    val activeUntil: Instant?,
    val zone: ZoneId,
    val opensToday: Int,
    val endedHereToday: Int,
)
