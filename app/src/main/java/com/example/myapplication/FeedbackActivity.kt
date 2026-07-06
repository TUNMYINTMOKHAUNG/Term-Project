package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.databinding.ActivityFeedbackBinding
import com.example.myapplication.recommendation.GeminiManager
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URLEncoder

class FeedbackActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedbackBinding

    private var suitabilityScore = 0
    private var suitabilityReasons = ""
    private var combinationScore = 0
    private var combinationReasons = ""
    private var alternativesList = emptyList<String>()
    private var stylingTipsText = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.generateBtn.setOnClickListener {
            val eventText = binding.eventInput.text.toString().trim()
            if (eventText.isEmpty()) {
                Toast.makeText(this, "Please write an event context!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            fetchStylingFeedback(eventText)
        }

        binding.menuTabsChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                binding.tabSuitability.id -> displaySuitability()
                binding.tabCombination.id -> displayCombination()
                binding.tabAlternatives.id -> displayAlternatives()
                binding.tabTips.id -> displayStylingTips()
            }
        }
    }

    private fun fetchStylingFeedback(eventDescription: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.feedbackResultsPanel.visibility = View.GONE

        val topType = intent.getStringExtra("TOP_TYPE") ?: "Top"
        val topColor = intent.getStringExtra("TOP_COLOR") ?: "Unknown Color"
        val topThickness = intent.getStringExtra("TOP_THICKNESS") ?: "Medium"

        val bottomType = intent.getStringExtra("BOTTOM_TYPE") ?: "Bottom"
        val bottomColor = intent.getStringExtra("BOTTOM_COLOR") ?: "Unknown Color"
        val bottomThickness = intent.getStringExtra("BOTTOM_THICKNESS") ?: "Medium"

        val outfitContextBuilder = StringBuilder("Outfit Context: ")
        outfitContextBuilder.append("A $topThickness thickness $topType with hex color code $topColor, ")
        outfitContextBuilder.append("paired with a $bottomThickness thickness $bottomType with hex color code $bottomColor")

        if (intent.hasExtra("OUTER_TYPE")) {
            val outerType = intent.getStringExtra("OUTER_TYPE")
            val outerColor = intent.getStringExtra("OUTER_COLOR")
            val outerThickness = intent.getStringExtra("OUTER_THICKNESS")
            outfitContextBuilder.append(", and layered with a $outerThickness $outerType with hex color code $outerColor")
        }
        outfitContextBuilder.append(".")

        val fullPrompt = "${outfitContextBuilder.toString()} Target event or occasion profile context restriction: $eventDescription"

        lifecycleScope.launch {
            val jsonRawResult = GeminiManager.generateFeedback(fullPrompt)
            binding.progressBar.visibility = View.GONE

            try {
                val jsonObject = JSONObject(jsonRawResult)

                suitabilityScore = jsonObject.optInt("suitability_score", 50)
                val suitArray = jsonObject.optJSONArray("suitability_reasons")
                val suitBuilder = StringBuilder()
                if (suitArray != null) {
                    for (i in 0 until suitArray.length()) {
                        suitBuilder.append("• ").append(suitArray.getString(i)).append("\n\n")
                    }
                }
                suitabilityReasons = suitBuilder.toString().trim()

                combinationScore = jsonObject.optInt("combination_score", 50)
                val combArray = jsonObject.optJSONArray("combination_reasons")
                val combBuilder = StringBuilder()
                if (combArray != null) {
                    for (i in 0 until combArray.length()) {
                        combBuilder.append("• ").append(combArray.getString(i)).append("\n\n")
                    }
                }
                combinationReasons = combBuilder.toString().trim()

                val altArray = jsonObject.optJSONArray("alternatives")
                val tempAlts = mutableListOf<String>()
                if (altArray != null) {
                    for (i in 0 until altArray.length()) {
                        tempAlts.add(altArray.getString(i))
                    }
                }
                alternativesList = tempAlts


                val tipsArray = jsonObject.optJSONArray("styling_tips")
                val tipsBuilder = StringBuilder()
                if (tipsArray != null) {
                    for (i in 0 until tipsArray.length()) {
                        tipsBuilder.append("✔ ").append(tipsArray.getString(i)).append("\n\n")
                    }
                }
                stylingTipsText = tipsBuilder.toString().trim()

                binding.feedbackResultsPanel.visibility = View.VISIBLE
                binding.tabSuitability.isChecked = true
                displaySuitability()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@FeedbackActivity, "Analysis decoding parsed format structural failure.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun displaySuitability() {
        binding.scoreContainer.visibility = View.VISIBLE
        binding.scoreProgressBar.setProgress(suitabilityScore, true)
        binding.scorePercentageText.text = "$suitabilityScore%"
        binding.dynamicFeedbackContent.text = suitabilityReasons
        binding.dynamicFeedbackContent.setOnClickListener(null)
    }

    private fun displayCombination() {
        binding.scoreContainer.visibility = View.VISIBLE
        binding.scoreProgressBar.setProgress(combinationScore, true)
        binding.scorePercentageText.text = "$combinationScore%"
        binding.dynamicFeedbackContent.text = combinationReasons
        binding.dynamicFeedbackContent.setOnClickListener(null)
    }

    private fun displayAlternatives() {
        binding.scoreContainer.visibility = View.GONE

        val builder = java.lang.StringBuilder()
        builder.append("Tap any alternative recommendation below to instantly discover curated reference mood boards over on Pinterest:\n\n")

        alternativesList.forEach { item ->
            builder.append("💡 ").append(item).append("\n\n")
        }

        binding.dynamicFeedbackContent.text = builder.toString().trim()

        binding.dynamicFeedbackContent.setOnClickListener {
            if (alternativesList.isNotEmpty()) {
                val topQuery = alternativesList.firstOrNull() ?: "fashion look"
                val encodedQuery = URLEncoder.encode("$topQuery aesthetic outfit", "UTF-8")
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.pinterest.com/search/pins/?q=$encodedQuery"))
                startActivity(webIntent)
            }
        }
    }

    private fun displayStylingTips() {
        binding.scoreContainer.visibility = View.GONE
        binding.dynamicFeedbackContent.text = stylingTipsText
        binding.dynamicFeedbackContent.setOnClickListener(null)
    }
}