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
import com.stoptime.game.ads.AdsManager
import java.util.Locale
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var timerText: TextView
    private lateinit var resultText: TextView
    private lateinit var statsText: TextView
    private lateinit var banner: TextView
    private lateinit var startStopButton: Button

    private lateinit var achievements: AchievementManager
    private lateinit var ads: AdsManager

    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var locked = false      // briefly blocks taps while an ad is about to show
    private var startTimeMs = 0L    // uses SystemClock.uptimeMillis() time base

    /** Redraws the clock on every screen refresh while running. */
    private val ticker = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            showTime(hundredthsBetween(startTimeMs, SystemClock.uptimeMillis()))
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fitToSystemBars(findViewById(R.id.root))

        timerText = findViewById(R.id.timerText)
        resultText = findViewById(R.id.resultText)
        statsText = findViewById(R.id.statsText)
        banner = findViewById(R.id.achievementBanner)
        startStopButton = findViewById(R.id.startStopButton)

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

        findViewById<Button>(R.id.achievementsButton).setOnClickListener {
            if (!running) startActivity(Intent(this, AchievementsActivity::class.java))
        }

        setButtonStyle(isRunning = false)
        updateStats()
    }

    private fun toggle(timeMs: Long) {
        if (locked) return
        if (!running) startClock(timeMs) else stopClock(timeMs)
    }

    private fun startClock(timeMs: Long) {
        startTimeMs = timeMs
        running = true
        resultText.text = ""
        showTime(0)
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

        val unlocked = achievements.recordAttempt(hundredths)
        showUnlocked(unlocked)
        updateStats()

        // Free version: full-screen ad every 4th try (pro version does nothing here).
        if (ads.shouldShowFullScreenAd(achievements.totalTries)) {
            locked = true
            handler.postDelayed({
                ads.showFullScreenAd { locked = false }
            }, if (unlocked.isEmpty()) 1200L else 2500L)
        }
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
        banner.text = list.joinToString("\n") { "🏆 Achievement unlocked: ${it.title}" }
        banner.alpha = 0f
        banner.visibility = android.view.View.VISIBLE
        banner.animate().alpha(1f).setDuration(250).start()
        handler.removeCallbacksAndMessages(BANNER_TOKEN)
        handler.postAtTime({
            banner.animate().alpha(0f).setDuration(400)
                .withEndAction { banner.visibility = android.view.View.INVISIBLE }.start()
        }, BANNER_TOKEN, SystemClock.uptimeMillis() + 3000)
    }

    private fun updateStats() {
        statsText.text = String.format(
            Locale.US, "Tries: %d   •   Perfect: %d   •   Best streak: %d",
            achievements.totalTries, achievements.totalPerfects, achievements.bestStreak
        )
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
        ads.resume()
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
    }
}
