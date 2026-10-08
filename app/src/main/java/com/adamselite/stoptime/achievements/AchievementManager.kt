package com.adamselite.stoptime.achievements

import android.content.Context

/** Saves stats, the Easy/Hard setting and unlocked achievements on the phone. */
class AchievementManager(context: Context) {

    private val prefs = context.getSharedPreferences("stoptime_stats", Context.MODE_PRIVATE)

    init {
        // Version 1 only had Easy mode, so its totals ARE the Easy totals.
        if (!prefs.contains("easy_tries")) {
            prefs.edit()
                .putInt("easy_tries", prefs.getInt(KEY_TRIES, 0))
                .putInt("easy_perfects", prefs.getInt(KEY_PERFECTS, 0))
                .putInt("easy_streak", prefs.getInt(KEY_STREAK, 0))
                .putInt("easy_best_streak", prefs.getInt(KEY_BEST_STREAK, 0))
                .apply()
        }
    }

    // ---- Easy / Hard setting (not affected by reset) ----
    var hardMode: Boolean
        get() = prefs.getBoolean(KEY_HARD_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_HARD_MODE, value).apply()

    // ---- All modes combined ----
    val totalTries get() = prefs.getInt(KEY_TRIES, 0)
    val totalPerfects get() = prefs.getInt(KEY_PERFECTS, 0)
    val perfectStreak get() = prefs.getInt(KEY_STREAK, 0)

    // ---- Per mode ("easy" or "hard") ----
    fun tries(hard: Boolean) = prefs.getInt(modeKey(hard, "tries"), 0)
    fun perfects(hard: Boolean) = prefs.getInt(modeKey(hard, "perfects"), 0)
    fun streak(hard: Boolean) = prefs.getInt(modeKey(hard, "streak"), 0)
    fun bestStreak(hard: Boolean) = prefs.getInt(modeKey(hard, "best_streak"), 0)

    fun isUnlocked(id: String) = unlockedIds().contains(id)
    fun unlockedCount() = Achievements.ALL.count { isUnlocked(it.id) }

    /**
     * Record one finished attempt.
     * @return achievements that were unlocked by THIS attempt (usually empty).
     */
    fun recordAttempt(hundredths: Int, hard: Boolean): List<Achievement> {
        val perfect = hundredths == 100

        val tries = totalTries + 1
        val perfects = totalPerfects + if (perfect) 1 else 0
        val streak = if (perfect) perfectStreak + 1 else 0

        val modeTries = tries(hard) + 1
        val modePerfects = perfects(hard) + if (perfect) 1 else 0
        val modeStreak = if (perfect) streak(hard) + 1 else 0

        val info = AttemptInfo(
            hundredths = hundredths,
            hard = hard,
            totalTries = tries,
            totalPerfects = perfects,
            perfectStreak = streak,
            hardTries = if (hard) modeTries else tries(true),
            hardPerfects = if (hard) modePerfects else perfects(true),
            hardStreak = if (hard) modeStreak else streak(true)
        )

        val unlocked = unlockedIds().toMutableSet()
        val newOnes = Achievements.ALL.filter { it.id !in unlocked && it.check(info) }
        unlocked += newOnes.map { it.id }

        prefs.edit()
            .putInt(KEY_TRIES, tries)
            .putInt(KEY_PERFECTS, perfects)
            .putInt(KEY_STREAK, streak)
            .putInt(KEY_BEST_STREAK, maxOf(prefs.getInt(KEY_BEST_STREAK, 0), streak))
            .putInt(modeKey(hard, "tries"), modeTries)
            .putInt(modeKey(hard, "perfects"), modePerfects)
            .putInt(modeKey(hard, "streak"), modeStreak)
            .putInt(modeKey(hard, "best_streak"), maxOf(bestStreak(hard), modeStreak))
            .putStringSet(KEY_UNLOCKED, unlocked)
            .apply()

        return newOnes
    }

    /**
     * Unlock an achievement because the player DID something (switched mode, theme...).
     * @return the achievement if it was newly unlocked, or null if already had it.
     */
    fun unlockAction(id: String): Achievement? {
        if (isUnlocked(id)) return null
        val achievement = Achievements.ALL.firstOrNull { it.id == id } ?: return null
        prefs.edit().putStringSet(KEY_UNLOCKED, unlockedIds() + id).apply()
        return achievement
    }

    /** Wipes ALL achievements and stats. Keeps the Easy/Hard setting. */
    fun resetAll() {
        val keepHard = hardMode
        prefs.edit().clear().putBoolean(KEY_HARD_MODE, keepHard).commit()
    }

    private fun modeKey(hard: Boolean, name: String) = (if (hard) "hard_" else "easy_") + name

    private fun unlockedIds(): Set<String> =
        prefs.getStringSet(KEY_UNLOCKED, emptySet()) ?: emptySet()

    companion object {
        private const val KEY_TRIES = "tries"
        private const val KEY_PERFECTS = "perfects"
        private const val KEY_STREAK = "streak"
        private const val KEY_BEST_STREAK = "best_streak"
        private const val KEY_UNLOCKED = "unlocked"
        private const val KEY_HARD_MODE = "hard_mode"
    }
}
