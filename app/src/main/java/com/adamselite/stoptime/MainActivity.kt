package com.adamselite.stoptime

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
import com.adamselite.stoptime.achievements.Achievement
import com.adamselite.stoptime.achievements.AchievementManager
import com.adamselite.stoptime.achievements.Achievements
import android.content.pm.ActivityInfo
import com.adamselite.stoptime.ads.AdsManager
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
    private lateinit var soundButton: TextView

    private lateinit var achievements: AchievementManager
    private lateinit var ads: AdsManager

    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var locked = false      // briefly blocks taps while an ad is about to show
    private var startTimeMs = 0L    // uses SystemClock.uptimeMillis() time base
    private var returningFromAchievements = false
    private lateinit var upgradeButton: TextView
    private val premiumListener: () -> Unit = { onPremiumChanged() }

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
        soundButton = findViewById(R.id.soundButton)

        achievements = AchievementManager(this)
        ads = AdsManager(this)
        ads.setup(findViewById<FrameLayout>(R.id.adContainer))
        // Ask for ad consent where the law requires it (Europe/UK...), then start ads.
        if (!Premium.adsRemoved) Consent.gather(this) { if (!isFinishing) ads.applyPremium() }

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
                Sounds.click()
                returningFromAchievements = true
                startActivity(Intent(this, AchievementsActivity::class.java))
            }
        }

        // "Remove ads" purchase link (hidden once bought).
        upgradeButton = findViewById(R.id.upgradeButton)
        upgradeButton.setOnClickListener {
            if (!running && !locked) { Sounds.click(); Premium.buy(this) }
        }
        Premium.addListener(premiumListener)
        updateUpgradeButton()

        easyButton.setOnClickListener { setMode(hard = false) }
        hardButton.setOnClickListener { setMode(hard = true) }

        // Sound on / off. Shows the CURRENT state.
        soundButton.text = if (Sounds.muted) "🔇" else "🔊"
        soundButton.setOnClickListener {
            if (running || locked) return@setOnClickListener
            val turnOff = !Sounds.muted
            Sounds.setMuted(this, turnOff)
            soundButton.text = if (turnOff) "🔇" else "🔊"
            if (turnOff) {
                showUnlocked(achievements.unlockAction(Achievements.SILENCE))
            } else {
                Sounds.click()   // a little confirmation that sound is back
            }
        }

        // Light / Dark toggle. Shows the mode you'd switch TO.
        themeButton.text = if (AppTheme.isDark(this)) "☀️" else "🌙"
        themeButton.setOnClickListener {
            if (!running && !locked) {
                Sounds.click()
                val goingDark = !AppTheme.isDark(this)
                if (goingDark) {
                    // The screen redraws after a theme change, so the pop-up is shown after that.
                    pendingUnlocks += achievements.unlockAction(Achievements.DARK_SIDE)
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
        Sounds.click()
        achievements.hardMode = hard
        showUnlocked(achievements.unlockAction(if (hard) Achievements.BRAVERY else Achievements.CHICKENED_OUT))
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
        Sounds.start()
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
        if (hundredths == 100) Sounds.perfect() else Sounds.stop()
        showTime(hundredths)
        showResult(hundredths)
        setButtonStyle(isRunning = false)

        val unlocked = achievements.recordAttempt(hundredths, achievements.hardMode)
        if (unlocked.isNotEmpty()) {
            handler.postDelayed({ showUnlocked(unlocked) }, if (hundredths == 100) 700L else 250L)
        }
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
        val master = list.firstOrNull { it.master }
        banner.text = if (list.size == 1) "🏆 Achievement unlocked: ${list[0].title}"
                      else "🏆 ${list.size} achievements unlocked: " + list.joinToString(" • ") { it.title }
        styleBanner(special = master != null)
        if (master != null) {
            // Top achievement: fireworks + the big fanfare.
            Sounds.celebrate()
            FireworksView.play(this,
                if (master.hardMode) FireworksView.Style.RED_WHITE else FireworksView.Style.GOLD_GREEN)
        } else {
            Sounds.achievement()
        }
        banner.alpha = 0f
        banner.visibility = android.view.View.VISIBLE
        banner.animate().alpha(1f).setDuration(250).start()
        handler.removeCallbacksAndMessages(BANNER_TOKEN)
        handler.postAtTime({
            banner.animate().alpha(0f).setDuration(400)
                .withEndAction { banner.visibility = android.view.View.INVISIBLE }.start()
        }, BANNER_TOKEN, SystemClock.uptimeMillis() + BANNER_SHOW_MS)
    }

    /** Gold-outlined pop-up for the top achievements, plain one for the rest. */
    private fun styleBanner(special: Boolean) {
        val density = resources.displayMetrics.density
        banner.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(ContextCompat.getColor(this@MainActivity, R.color.surface))
            cornerRadius = 10 * density
            if (special) setStroke((2 * density).toInt(), ContextCompat.getColor(this@MainActivity, R.color.gold))
        }
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
        Premium.refresh()   // catches purchases finished outside the app
        onPremiumChanged()
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

    /** Called when the purchase state or price changes. */
    private fun onPremiumChanged() {
        updateUpgradeButton()
        ads.applyPremium()
    }

    private fun updateUpgradeButton() {
        upgradeButton.visibility = if (Premium.adsRemoved) android.view.View.GONE else android.view.View.VISIBLE
        upgradeButton.text = Premium.price?.let { "${getString(R.string.go_pro)} – $it" } ?: getString(R.string.go_pro)
    }

    override fun onDestroy() {
        Premium.removeListener(premiumListener)
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
