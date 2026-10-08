package com.stoptime.game

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

class StopTimeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppTheme.apply(this)
    }
}

/** Light / Dark setting. Stored separately so "Reset all achievements" doesn't touch it. */
/** Opens the Pro version's page on Google Play. */
object ProUpgrade {
    /** Package name of the Pro app (the free app is this + ".free"). */
    const val PRO_PACKAGE = "com.stoptime.game"

    fun open(context: Context) {
        val market = android.content.Intent(android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse("market://details?id=$PRO_PACKAGE"))
        val web = android.content.Intent(android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse("https://play.google.com/store/apps/details?id=$PRO_PACKAGE"))
        try { context.startActivity(market) }
        catch (e: android.content.ActivityNotFoundException) {
            try { context.startActivity(web) } catch (_: Exception) {}
        }
    }
}

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
