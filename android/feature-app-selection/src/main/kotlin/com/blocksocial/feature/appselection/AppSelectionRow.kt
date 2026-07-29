package com.blocksocial.feature.appselection

import com.blocksocial.core.model.AppRef

enum class InstallState { INSTALLED, NOT_INSTALLED }

data class AppSelectionRow(
    val ref: AppRef,
    val displayName: String,
    val installState: InstallState,
    val selected: Boolean,
) {
    val canBeSelected: Boolean = installState == InstallState.INSTALLED
}
