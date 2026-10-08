package com.adamselite.stoptime

import android.app.Activity
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** Keeps content (and the banner ad) out from under the status bar and navigation bar. */
fun Activity.fitToSystemBars(root: View) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    // Dark icons on the status/nav bar in Light mode, white icons in Dark mode.
    WindowInsetsControllerCompat(window, window.decorView).apply {
        val light = !AppTheme.isDark(this@fitToSystemBars)
        isAppearanceLightStatusBars = light
        isAppearanceLightNavigationBars = light
    }
    ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
        insets
    }
}
