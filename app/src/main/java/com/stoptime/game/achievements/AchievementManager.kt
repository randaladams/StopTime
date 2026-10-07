package com.stoptime.game.achievements

import android.content.Context

/** Saves stats and unlocked achievements on the phone, and checks for new unlocks. */
class AchievementManager(context: Context) {

    private val prefs = context.getSharedPreferences("stoptime_stats", Context.MODE_PRIVATE)

    val totalTries get() = prefs.getInt(KEY_TRIES, 0)
    val totalPerfects get() = prefs.getInt(KEY_PERFECTS, 0)
    val perfectStreak get() = prefs.getInt(KEY_STREAK, 0)
    val bestStreak get() = prefs.getInt(KEY_BEST_STREAK, 0)

    fun isUnlocked(id: String) = unlockedIds().contains(id)
    fun unlockedCount() = Achievements.ALL.count { isUnlocked(it.id) }

    /**
     * Record one finished attempt.
     * @return achievements that were unlocked by THIS attempt (usually empty).
     */
    fun recordAttempt(hundredths: Int): List<Achievement> {
        val perfect = hundredths == 100
        val tries = totalTries + 1
        val perfects = totalPerfects + if (perfect) 1 else 0
        val streak = if (perfect) perfectStreak + 1 else 0

        val info = AttemptInfo(hundredths, tries, perfects, streak)
        val unlocked = unlockedIds().toMutableSet()
        val newOnes = Achievements.ALL.filter { it.id !in unlocked && it.check(info) }
        unlocked += newOnes.map { it.id }

        prefs.edit()
            .putInt(KEY_TRIES, tries)
            .putInt(KEY_PERFECTS, perfects)
            .putInt(KEY_STREAK, streak)
            .putInt(KEY_BEST_STREAK, maxOf(bestStreak, streak))
            .putStringSet(KEY_UNLOCKED, unlocked)
            .apply()

        return newOnes
    }

    private fun unlockedIds(): Set<String> =
        prefs.getStringSet(KEY_UNLOCKED, emptySet()) ?: emptySet()

    companion object {
        private const val KEY_TRIES = "tries"
        private const val KEY_PERFECTS = "perfects"
        private const val KEY_STREAK = "streak"
        private const val KEY_BEST_STREAK = "best_streak"
        private const val KEY_UNLOCKED = "unlocked"
    }
}
