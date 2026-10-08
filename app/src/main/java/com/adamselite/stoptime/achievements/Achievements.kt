package com.adamselite.stoptime.achievements

/**
 * Everything the game knows about one finished attempt.
 * hundredths = what the clock showed when stopped (100 = exactly 1.00 second).
 * The totals already include this attempt.
 */
data class AttemptInfo(
    val hundredths: Int,
    val hard: Boolean,           // was this attempt played in Hard mode?
    // All modes combined
    val totalTries: Int,
    val totalPerfects: Int,
    val perfectStreak: Int,
    // Hard mode only
    val hardTries: Int,
    val hardPerfects: Int,
    val hardStreak: Int
) {
    val isPerfect get() = hundredths == 100
    val offBy get() = kotlin.math.abs(hundredths - 100)
}

data class Achievement(
    val id: String,          // never change this once released (it's how unlocks are saved)
    val title: String,
    val description: String,
    val hardMode: Boolean = false,   // true = listed in the "Hard Mode" section
    val action: Boolean = false,     // true = unlocked by doing something (not by a try); see unlockAction()
    val check: (AttemptInfo) -> Boolean
)

/**
 * THE ACHIEVEMENT LIST.
 * To add a new achievement, just add one more line to this list.
 */
object Achievements {
    // ids for the action achievements
    const val BRAVERY = "action_bravery"
    const val CHICKENED_OUT = "action_chickened_out"
    const val DARK_SIDE = "action_dark_side"
    const val SILENCE = "action_silence"

    val ALL: List<Achievement> = listOf(
        // ================= GENERAL (any mode) =================
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

        // --- Unlocked by actions, not by tries (see AchievementManager.unlockAction) ---
        Achievement(BRAVERY, "Bravery", "Switch to Hard mode", action = true) { false },
        Achievement(CHICKENED_OUT, "Chickened Out", "Switch back to Easy mode", action = true) { false },
        Achievement(DARK_SIDE, "Joined the Dark Side", "Switch to Dark mode", action = true) { false },
        Achievement(SILENCE, "Silence", "Turn the sound off", action = true) { false },

        // ================= HARD MODE (clock hidden) =================
        Achievement("hard_first", "Blindfolded", "Finish your first Hard mode attempt", hardMode = true) { it.hard },
        Achievement("hard_close_10", "Feeling It", "Hard mode: stop within 0.10 of 1.00", hardMode = true) { it.hard && it.offBy <= 10 },
        Achievement("hard_close_5", "Inner Clock", "Hard mode: stop within 0.05 of 1.00", hardMode = true) { it.hard && it.offBy <= 5 },
        Achievement("hard_close_1", "Sixth Sense", "Hard mode: stop at 0.99 or 1.01", hardMode = true) { it.hard && it.offBy == 1 },
        Achievement("hard_perfect", "Blind Luck", "Hard mode: stop at exactly 1.00", hardMode = true) { it.hard && it.isPerfect },
        Achievement("hard_perfect_3", "Not Luck", "Get 3 perfect stops in Hard mode", hardMode = true) { it.hardPerfects >= 3 },
        Achievement("hard_perfect_10", "Clockwork Heart", "Get 10 perfect stops in Hard mode", hardMode = true) { it.hardPerfects >= 10 },
        Achievement("hard_streak_2", "Lightning Strikes Twice", "Hard mode: 2 perfect stops in a row", hardMode = true) { it.hardStreak >= 2 },
        Achievement("hard_streak_3", "Living Metronome", "Hard mode: 3 perfect stops in a row", hardMode = true) { it.hardStreak >= 3 },
        Achievement("hard_tries_50", "Trust the Feeling", "Play 50 times in Hard mode", hardMode = true) { it.hardTries >= 50 },
        Achievement("hard_tries_500", "Zen Master", "Play 500 times in Hard mode", hardMode = true) { it.hardTries >= 500 },
    )
}
