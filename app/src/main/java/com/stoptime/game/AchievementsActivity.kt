package com.stoptime.game

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.stoptime.game.achievements.AchievementManager
import com.stoptime.game.achievements.Achievements

class AchievementsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)
        fitToSystemBars(findViewById(R.id.root))

        findViewById<Button>(R.id.backButton).setOnClickListener { finish() }

        val manager = AchievementManager(this)
        val all = Achievements.ALL
        findViewById<TextView>(R.id.progressText).text =
            "${manager.unlockedCount()} of ${all.size} unlocked"

        val list = findViewById<LinearLayout>(R.id.listContainer)
        val density = resources.displayMetrics.density
        val pad = (12 * density).toInt()

        // Unlocked ones first, then locked ones.
        all.sortedByDescending { manager.isUnlocked(it.id) }.forEach { a ->
            val unlocked = manager.isUnlocked(a.id)
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(pad, pad, pad, pad)
                setBackgroundColor(ContextCompat.getColor(context, R.color.surface))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (8 * density).toInt() }
            }
            row.addView(TextView(this).apply {
                text = if (unlocked) "🏆  ${a.title}" else "🔒  ${a.title}"
                textSize = 18f
                setTextColor(ContextCompat.getColor(context, if (unlocked) R.color.gold else R.color.locked))
            })
            row.addView(TextView(this).apply {
                text = a.description
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, if (unlocked) R.color.text_secondary else R.color.locked))
            })
            list.addView(row)
        }
    }
}
