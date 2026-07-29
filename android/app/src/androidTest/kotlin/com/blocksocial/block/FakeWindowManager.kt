package com.blocksocial.block

import android.view.Display
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager

class FakeWindowManager : WindowManager {

    val added = mutableListOf<View>()
    val removed = mutableListOf<View>()

    val attachedCount: Int get() = added.size - removed.size

    override fun addView(view: View, params: ViewGroup.LayoutParams) {
        added += view
    }

    override fun updateViewLayout(view: View, params: ViewGroup.LayoutParams) = Unit

    override fun removeView(view: View) {
        removed += view
    }

    override fun removeViewImmediate(view: View) {
        removed += view
    }

    @Deprecated("Deprecated in Java", ReplaceWith(""))
    override fun getDefaultDisplay(): Display = throw UnsupportedOperationException()
}
