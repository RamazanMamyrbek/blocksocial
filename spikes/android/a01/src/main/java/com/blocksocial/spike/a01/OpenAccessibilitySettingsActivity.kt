package com.blocksocial.spike.a01

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings

class OpenAccessibilitySettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        finish()
    }
}
