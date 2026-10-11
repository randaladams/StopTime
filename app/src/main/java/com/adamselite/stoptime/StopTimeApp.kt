package com.adamselite.stoptime

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

class StopTimeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppTheme.apply(this)
        Sounds.init(this)
        Premium.init(this)
    }
}

/** Light / Dark setting. Stored separately so "Reset all achievements" doesn't touch it. */
object AppTheme {
    private const val PREFS = "stoptime_settings"
    private const val KEY_DARK = "dark_mode"

    fun isDark(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DARK, false) // light is default

    fun setDark(context: Context, dark: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_DARK, dark).apply()
        apply(context)   // Android redraws the open screens automatically
    }

    fun apply(context: Context) {
        AppCompatDelegate.setDefaultNightMode(
            if (isDark(context)) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
