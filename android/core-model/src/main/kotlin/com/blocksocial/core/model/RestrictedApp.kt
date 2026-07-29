package com.blocksocial.core.model

data class RestrictedApp(
    val ref: AppRef,
    val displayName: String,
    val selected: Boolean,
)
