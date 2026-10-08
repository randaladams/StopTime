package com.stoptime.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/**
 * All game sounds. Loaded once when the app starts (low delay, good for timing games).
 * The sound files live in app/src/main/res/raw/ - replace any .ogg there to change a sound.
 * Volume follows the phone's media volume.
 */
object Sounds {
    private var pool: SoundPool? = null
    private var start = 0
    private var stop = 0
    private var click = 0
    private var perfect = 0
    private var achievement = 0

    fun init(context: Context) {
        if (pool != null) return
        val p = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            ).build()
        start = p.load(context, R.raw.snd_start, 1)
        stop = p.load(context, R.raw.snd_stop, 1)
        click = p.load(context, R.raw.snd_click, 1)
        perfect = p.load(context, R.raw.snd_perfect, 1)
        achievement = p.load(context, R.raw.snd_achievement, 1)
        pool = p
    }

    private fun play(id: Int, volume: Float = 1f) {
        pool?.play(id, volume, volume, 1, 0, 1f)
    }

    fun start() = play(start)
    fun stop() = play(stop)
    fun click() = play(click, 0.8f)       // menu buttons: upgrade, theme, achievements, back...
    fun perfect() = play(perfect)
    fun achievement() = play(achievement)
}
