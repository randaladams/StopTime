package com.stoptime.game

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.stoptime.game.achievements.Achievement
import com.stoptime.game.achievements.AchievementManager
import com.stoptime.game.achievements.Achievements

class AchievementsActivity : AppCompatActivity() {

    private lateinit var manager: AchievementManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT   // never rotate
        setContentView(R.layout.activity_achievements)
        fitToSystemBars(findViewById(R.id.root))

        manager = AchievementManager(this)
        findViewById<android.view.View>(R.id.backButton).setOnClickListener { Sounds.click(); finish() }
        findViewById<Button>(R.id.resetButton).setOnClickListener { Sounds.click(); confirmReset() }

        findViewById<TextView>(R.id.versionText).text =
            "${getString(R.string.app_name)}  •  Version ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})"

        buildList()
    }

    private fun confirmReset() {
        AlertDialog.Builder(this)
            .setTitle("Reset everything?")
            .setMessage(
                "This will lock ALL achievements again and set your tries, perfects " +
                "and streaks back to zero.\n\nThis cannot be undone."
            )
            .setPositiveButton("Reset") { _, _ ->
                manager.resetAll()
                buildList()
                Toast.makeText(this, "All achievements reset", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun buildList() {
        val all = Achievements.ALL
        findViewById<TextView>(R.id.progressText).text =
            "${manager.unlockedCount()} of ${all.size} unlocked"

        val list = findViewById<LinearLayout>(R.id.listContainer)
        list.removeAllViews()

        addSection(list, "General", all.filter { !it.hardMode })
        addSection(list, "Hard Mode  (clock hidden)", all.filter { it.hardMode })
    }

    private fun addSection(list: LinearLayout, title: String, items: List<Achievement>) {
        val density = resources.displayMetrics.density
        val pad = (12 * density).toInt()
        val done = items.count { manager.isUnlocked(it.id) }

        list.addView(TextView(this).apply {
            text = "$title   $done/${items.size}"
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            setPadding(0, if (list.childCount == 0) 0 else (16 * density).toInt(), 0, (8 * density).toInt())
        })

        // Unlocked ones first, then locked ones.
        items.sortedByDescending { manager.isUnlocked(it.id) }.forEach { a ->
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
