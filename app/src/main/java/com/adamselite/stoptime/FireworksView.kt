package com.adamselite.stoptime

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.SystemClock
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Full-screen fireworks for the top achievements (Master of Time, Blind Grandmaster).
 * Drawn in code: rockets rise, burst into sparks, fall with gravity and fade.
 * Tapping the screen ends it early. While it plays it covers the screen,
 * so START can't be pressed by accident.
 * If the phone's "Remove animations" setting is on, a soft gold glow is shown instead.
 */
@SuppressLint("ViewConstructor")
class FireworksView private constructor(
    context: Context,
    private val colors: IntArray,
    private val glowMode: Boolean,
    private val onDone: () -> Unit
) : View(context) {

    enum class Style { GOLD_GREEN, RED_WHITE }

    companion object {
        private const val SHOW_MS = 4200L
        private const val GLOW_MS = 2500L

        /** Plays fireworks over the whole screen of [activity]. */
        fun play(activity: Activity, style: Style, onDone: () -> Unit = {}) {
            val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
            // Only one show at a time.
            for (i in 0 until root.childCount) if (root.getChildAt(i) is FireworksView) return
            val dark = AppTheme.isDark(activity)
            val colors = when (style) {
                Style.GOLD_GREEN -> if (dark) longArrayOf(0xFFFFC107, 0xFF66BB6A, 0xFFFFE082, 0xFFA5D6A7, 0xFFFFFFFF)
                                    else longArrayOf(0xFFB26A00, 0xFF2E7D32, 0xFFE09F00, 0xFF43A047, 0xFFC62828)
                Style.RED_WHITE -> if (dark) longArrayOf(0xFFEF5350, 0xFFFFC107, 0xFFFFFFFF, 0xFFFF8A80, 0xFFCE93D8)
                                   else longArrayOf(0xFFC62828, 0xFFB26A00, 0xFF6A1B9A, 0xFFE53935, 0xFF37474F)
            }.map { it.toInt() }.toIntArray()
            val noAnimations = Settings.Global.getFloat(
                activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
            val view = FireworksView(activity, colors, noAnimations, onDone)
            view.dark = dark
            root.addView(view, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
    }

    private class Rocket(var x: Float, var y: Float, val vx: Float, var vy: Float, val color: Int) {
        val trailX = FloatArray(8); val trailY = FloatArray(8); var trailCount = 0
    }
    private class Spark(var x: Float, var y: Float, var vx: Float, var vy: Float,
                        val decay: Float, val color: Int, val size: Float) {
        var px = x; var py = y; var life = 1f
    }

    private var dark = false
    private val rockets = mutableListOf<Rocket>()
    private val sparks = mutableListOf<Spark>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var startMs = 0L
    private var lastMs = 0L
    private var nextLaunchMs = 0L
    private var launching = true
    private var finished = false
    private val dp = resources.displayMetrics.density

    init {
        isClickable = true
        isFocusable = false
        contentDescription = "Fireworks. Tap to continue."
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startMs = SystemClock.uptimeMillis()
        lastMs = startMs
        nextLaunchMs = startMs
        postInvalidateOnAnimation()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) finish()
        return true   // swallow all touches while showing
    }

    private fun finish() {
        if (finished) return
        finished = true
        (parent as? ViewGroup)?.removeView(this)
        onDone()
    }

    private fun launch() {
        val h = height.toFloat()
        rockets += Rocket(
            x = width * (0.15f + Random.nextFloat() * 0.7f), y = h + 4 * dp,
            vx = (Random.nextFloat() - 0.5f) * 0.8f * dp,
            vy = -(h * 0.0105f + Random.nextFloat() * h * 0.003f),
            color = colors.random()
        )
    }

    private fun explode(r: Rocket) {
        val n = 55 + Random.nextInt(30)
        val alt = colors.random()
        val ring = Random.nextFloat() < 0.35f
        for (i in 0 until n) {
            val a = (i.toFloat() / n) * 2f * PI.toFloat() + Random.nextFloat() * 0.08f
            val speed = (if (ring) 2.6f else 0.6f + Random.nextFloat() * 2.6f) * dp
            sparks += Spark(r.x, r.y, cos(a) * speed, sin(a) * speed,
                decay = 0.010f + Random.nextFloat() * 0.010f,
                color = if (i % 3 == 0) alt else r.color,
                size = (1.4f + Random.nextFloat() * 1.2f) * dp)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (finished) return
        val now = SystemClock.uptimeMillis()
        val elapsed = now - startMs

        if (glowMode) { drawGlow(canvas, elapsed); return }

        // Physics is tuned for 60 fps; scale steps so it looks the same at 90/120 Hz.
        val steps = max(1f, min(3f, (now - lastMs) / 16.67f))
        lastMs = now
        if (launching && now >= nextLaunchMs && elapsed < SHOW_MS - 900) {
            launch(); if (Random.nextFloat() < 0.35f) launch()
            nextLaunchMs = now + 260 + Random.nextLong(260)
        }
        if (elapsed >= SHOW_MS) launching = false

        paint.xfermode = if (dark) PorterDuffXfermode(PorterDuff.Mode.ADD) else null
        paint.style = Paint.Style.STROKE

        val rIt = rockets.iterator()
        while (rIt.hasNext()) {
            val r = rIt.next()
            if (r.trailCount < r.trailX.size) r.trailCount++
            System.arraycopy(r.trailX, 0, r.trailX, 1, r.trailX.size - 1)
            System.arraycopy(r.trailY, 0, r.trailY, 1, r.trailY.size - 1)
            r.trailX[0] = r.x; r.trailY[0] = r.y
            r.x += r.vx * steps; r.y += r.vy * steps; r.vy += 0.09f * dp * steps
            paint.color = r.color; paint.alpha = 230; paint.strokeWidth = 2 * dp
            val last = r.trailCount - 1
            canvas.drawLine(r.trailX[last], r.trailY[last], r.x, r.y, paint)
            if (r.vy >= -0.6f * dp || r.y < height * 0.12f) { explode(r); rIt.remove() }
        }

        val sIt = sparks.iterator()
        while (sIt.hasNext()) {
            val s = sIt.next()
            s.px = s.x; s.py = s.y
            val drag = Math.pow(0.975, steps.toDouble()).toFloat()
            s.vx *= drag; s.vy = s.vy * drag + 0.035f * dp * steps
            s.x += s.vx * steps; s.y += s.vy * steps
            s.life -= s.decay * steps
            if (s.life <= 0f) { sIt.remove(); continue }
            paint.color = s.color; paint.alpha = (255 * s.life).toInt().coerceIn(0, 255)
            paint.strokeWidth = s.size
            canvas.drawLine(s.px, s.py, s.x, s.y, paint)
        }

        if (!launching && rockets.isEmpty() && sparks.isEmpty()) finish()
        else postInvalidateOnAnimation()
    }

    /** Reduced-motion version: a soft gold glow that fades in and out. */
    private fun drawGlow(canvas: Canvas, elapsed: Long) {
        if (elapsed >= GLOW_MS) { finish(); return }
        val t = elapsed.toFloat() / GLOW_MS
        val alpha = (if (t < 0.25f) t / 0.25f else if (t > 0.7f) (1f - t) / 0.3f else 1f).coerceIn(0f, 1f)
        val radius = max(width, height) * 0.65f
        glowPaint.shader = RadialGradient(width / 2f, height * 0.45f, radius,
            Color.argb((140 * alpha).toInt(), 255, 193, 7), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), glowPaint)
        postInvalidateOnAnimation()
    }
}
