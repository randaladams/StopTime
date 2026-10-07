package com.stoptime.game.achievements

/**
 * Everything the game knows about one finished attempt.
 * hundredths = what the clock showed when stopped (100 = exactly 1.00 second).
 */
data class AttemptInfo(
    val hundredths: Int,
    val totalTries: Int,
    val totalPerfects: Int,
    val perfectStreak: Int
) {
    val isPerfect get() = hundredths == 100
    val offBy get() = kotlin.math.abs(hundredths - 100)
}

data class Achievement(
    val id: String,          // never change this once released (it's how unlocks are saved)
    val title: String,
    val description: String,
    val check: (AttemptInfo) -> Boolean
)

/**
 * THE ACHIEVEMENT LIST.
 * To add a new achievement, just add one more line to this list.
 */
object Achievements {
    val ALL: List<Achievement> = listOf(
        // --- Accuracy ---
        Achievement("first_try", "First Tick", "Finish your first attempt") { it.totalTries >= 1 },
        Achievement("close_10", "Ballpark", "Stop within 0.10 of 1.00") { it.offBy <= 10 },
        Achievement("close_5", "Getting Warm", "Stop within 0.05 of 1.00") { it.offBy <= 5 },
        Achievement("close_1", "So Close", "Stop at 0.99 or 1.01") { it.offBy == 1 },
        Achievement("perfect", "One Second Flat", "Stop at exactly 1.00") { it.isPerfect },

        // --- Silly ones ---
        Achievement("trigger_happy", "Trigger Happy", "Stop the clock under 0.50") { it.hundredths < 50 },
        Achievement("daydreamer", "Daydreamer", "Let the clock run past 3.00") { it.hundredths > 300 },
        Achievement("palindrome", "Mirror Image", "Stop at exactly 1.11") { it.hundredths == 111 },

        // --- Perfect counts ---
        Achievement("perfect_3", "Hat Trick", "Get 3 perfect stops total") { it.totalPerfects >= 3 },
        Achievement("perfect_10", "Metronome", "Get 10 perfect stops total") { it.totalPerfects >= 10 },
        Achievement("perfect_50", "Atomic Clock", "Get 50 perfect stops total") { it.totalPerfects >= 50 },

        // --- Streaks ---
        Achievement("streak_2", "Double Down", "2 perfect stops in a row") { it.perfectStreak >= 2 },
        Achievement("streak_3", "Human Clock", "3 perfect stops in a row") { it.perfectStreak >= 3 },

        // --- Persistence ---
        Achievement("tries_10", "Warming Up", "Play 10 times") { it.totalTries >= 10 },
        Achievement("tries_100", "Dedicated", "Play 100 times") { it.totalTries >= 100 },
        Achievement("tries_1000", "Obsessed", "Play 1,000 times") { it.totalTries >= 1000 },
    )
}
