package com.stoptime.game

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Choreographer
import android.view.MotionEvent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.stoptime.game.achievements.Achievement
import com.stoptime.game.achievements.AchievementManager
import com.stoptime.game.achievements.Achievements
import android.content.pm.ActivityInfo
import com.stoptime.game.ads.AdsManager
import java.util.Locale
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var timerText: TextView
    private lateinit var resultText: TextView
    private lateinit var statTries: TextView
    private lateinit var statPerfects: TextView
    private lateinit var statStreak: TextView
    private lateinit var banner: TextView
    private lateinit var startStopButton: Button
    private lateinit var targetText: TextView
    private lateinit var easyButton: TextView
    private lateinit var hardButton: TextView
    private lateinit var themeButton: TextView

    private lateinit var achievements: AchievementManager
    private lateinit var ads: AdsManager

    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var locked = false      // briefly blocks taps while an ad is about to show
    private var startTimeMs = 0L    // uses SystemClock.uptimeMillis() time base
    private var returningFromAchievements = false

    /** Redraws the clock on every screen refresh while running. */
    private val ticker = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            // Hard mode: keep the clock hidden while it runs.
            if (!achievements.hardMode) showTime(hundredthsBetween(startTimeMs, SystemClock.uptimeMillis()))
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT   // never rotate
        setContentView(R.layout.activity_main)
        fitToSystemBars(findViewById(R.id.root))

        timerText = findViewById(R.id.timerText)
        resultText = findViewById(R.id.resultText)
        statTries = findViewById(R.id.statTries)
        statPerfects = findViewById(R.id.statPerfects)
        statStreak = findViewById(R.id.statStreak)
        banner = findViewById(R.id.achievementBanner)
        startStopButton = findViewById(R.id.startStopButton)
        targetText = findViewById(R.id.targetText)
        easyButton = findViewById(R.id.easyButton)
        hardButton = findViewById(R.id.hardButton)
        themeButton = findViewById(R.id.themeButton)

        achievements = AchievementManager(this)
        ads = AdsManager(this)
        ads.setup(findViewById<FrameLayout>(R.id.adContainer))

        // React the instant the finger touches the button (not when it lifts),
        // using the touch's own timestamp so timing is as fair as possible.
        startStopButton.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) toggle(event.eventTime)
            true
        }
        // Accessibility (TalkBack etc.) still works through a normal click.
        startStopButton.setOnClickListener { toggle(SystemClock.uptimeMillis()) }

        findViewById<android.view.View>(R.id.achievementsButton).setOnClickListener {
            if (!running && !locked) {
                returningFromAchievements = true
                startActivity(Intent(this, AchievementsActivity::class.java))
            }
        }

        // "Go Pro" link: free version only.
        findViewById<TextView>(R.id.upgradeButton).apply {
            if (BuildConfig.IS_PRO) visibility = android.view.View.GONE
            else setOnClickListener { if (!running && !locked) ProUpgrade.open(this@MainActivity) }
        }

        easyButton.setOnClickListener { setMode(hard = false) }
        hardButton.setOnClickListener { setMode(hard = true) }

        // Light / Dark toggle. Shows the mode you'd switch TO.
        themeButton.text = if (AppTheme.isDark(this)) "☀️" else "🌙"
        themeButton.setOnClickListener {
            if (!running && !locked) {
                val goingDark = !AppTheme.isDark(this)
                if (goingDark) {
                    // The screen redraws after a theme change, so the pop-up is shown after that.
                    achievements.unlockAction(Achievements.DARK_SIDE)?.let { pendingUnlocks += it }
                }
                AppTheme.setDark(this, goingDark)
            }
        }

        // Show any achievement earned just before the screen was redrawn (theme switch).
        if (pendingUnlocks.isNotEmpty()) {
            val list = pendingUnlocks.toList()
            pendingUnlocks.clear()
            banner.post { showUnlocked(list) }
        }

        setButtonStyle(isRunning = false)
        showModeStyle()
        updateStats()
    }

    /** Switch between Easy and Hard (not allowed while the clock is running). */
    private fun setMode(hard: Boolean) {
        if (running || locked || achievements.hardMode == hard) return
        achievements.hardMode = hard
        val earned = achievements.unlockAction(if (hard) Achievements.BRAVERY else Achievements.CHICKENED_OUT)
        if (earned != null) showUnlocked(listOf(earned))
        showModeStyle()
        resultText.text = ""
        showTime(0)
        timerText.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        updateStats()
    }

    private fun showModeStyle() {
        val hard = achievements.hardMode
        styleModeButton(easyButton, selected = !hard, color = R.color.start_green)
        styleModeButton(hardButton, selected = hard, color = R.color.stop_red)
        targetText.text = getString(if (hard) R.string.target_hard else R.string.target)
    }

    private fun styleModeButton(view: TextView, selected: Boolean, color: Int) {
        val c = ContextCompat.getColor(this, color)
        val density = resources.displayMetrics.density
        view.background = GradientDrawable().apply {
            cornerRadius = 22f * density
            if (selected) setColor(c) else setColor(0)
            setStroke((2 * density).toInt(), c)
        }
        view.setTextColor(ContextCompat.getColor(this, if (selected) R.color.on_accent else R.color.text_secondary))
    }

    private fun toggle(timeMs: Long) {
        if (locked) return
        if (!running) startClock(timeMs) else stopClock(timeMs)
    }

    private fun startClock(timeMs: Long) {
        startTimeMs = timeMs
        running = true
        resultText.text = ""
        if (achievements.hardMode) timerText.text = "?.??" else showTime(0)
        setButtonStyle(isRunning = true)
        Choreographer.getInstance().postFrameCallback(ticker)
    }

    private fun stopClock(timeMs: Long) {
        running = false
        Choreographer.getInstance().removeFrameCallback(ticker)

        val hundredths = hundredthsBetween(startTimeMs, timeMs)
        showTime(hundredths)
        showResult(hundredths)
        setButtonStyle(isRunning = false)

        val unlocked = achievements.recordAttempt(hundredths, achievements.hardMode)
        showUnlocked(unlocked)
        updateStats()
    }

    /** Whole hundredths of a second (truncated, so 0.999 shows as 0.99). */
    private fun hundredthsBetween(startMs: Long, endMs: Long): Int =
        ((endMs - startMs).coerceAtLeast(0) / 10).toInt()

    private fun showTime(hundredths: Int) {
        timerText.text = String.format(Locale.US, "%d.%02d", hundredths / 100, hundredths % 100)
    }

    private fun showResult(hundredths: Int) {
        val diff = hundredths - 100
        if (diff == 0) {
            resultText.text = "PERFECT! Exactly 1.00!"
            resultText.setTextColor(ContextCompat.getColor(this, R.color.perfect))
            timerText.setTextColor(ContextCompat.getColor(this, R.color.perfect))
        } else {
            val amount = String.format(Locale.US, "%d.%02d", abs(diff) / 100, abs(diff) % 100)
            resultText.text = if (diff > 0) "$amount too late" else "$amount too early"
            resultText.setTextColor(ContextCompat.getColor(this, R.color.miss))
            timerText.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        }
    }

    private fun showUnlocked(list: List<Achievement>) {
        if (list.isEmpty()) return
        banner.text = if (list.size == 1) "🏆 Achievement unlocked: ${list[0].title}"
                      else "🏆 ${list.size} achievements unlocked: " + list.joinToString(" • ") { it.title }
        banner.alpha = 0f
        banner.visibility = android.view.View.VISIBLE
        banner.animate().alpha(1f).setDuration(250).start()
        handler.removeCallbacksAndMessages(BANNER_TOKEN)
        handler.postAtTime({
            banner.animate().alpha(0f).setDuration(400)
                .withEndAction { banner.visibility = android.view.View.INVISIBLE }.start()
        }, BANNER_TOKEN, SystemClock.uptimeMillis() + BANNER_SHOW_MS)
    }

    /** Shows the stats for the mode currently selected. */
    private fun updateStats() {
        val hard = achievements.hardMode
        statTries.text = String.format(Locale.US, "%,d", achievements.tries(hard))
        statPerfects.text = String.format(Locale.US, "%,d", achievements.perfects(hard))
        statStreak.text = String.format(Locale.US, "%,d", achievements.bestStreak(hard))
    }

    private fun setButtonStyle(isRunning: Boolean) {
        val color = ContextCompat.getColor(this, if (isRunning) R.color.stop_red else R.color.start_green)
        startStopButton.background = GradientDrawable().apply {
            cornerRadius = 48f * resources.displayMetrics.density
            setColor(color)
        }
        startStopButton.text = getString(if (isRunning) R.string.stop else R.string.start)
        if (isRunning) timerText.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
    }

    override fun onResume() {
        super.onResume()
        updateStats()   // in case achievements were reset on the other screen
        ads.resume()

        // Free version: maybe show a full-screen ad when coming back from Achievements.
        if (returningFromAchievements) {
            returningFromAchievements = false
            handler.postDelayed({
                if (!isFinishing && !running) {
                    locked = true
                    ads.onReturnFromAchievements { locked = false }
                }
            }, 300)
        }
    }

    override fun onPause() {
        // If the player leaves the app mid-run, cancel that attempt.
        if (running) {
            running = false
            Choreographer.getInstance().removeFrameCallback(ticker)
            showTime(0)
            resultText.text = ""
            setButtonStyle(isRunning = false)
        }
        ads.pause()
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        ads.destroy()
        super.onDestroy()
    }

    companion object {
        private val BANNER_TOKEN = Any()
        private val pendingUnlocks = mutableListOf<Achievement>()
        private const val BANNER_SHOW_MS = 5000L   // how long "Achievement unlocked" stays up
    }
}
