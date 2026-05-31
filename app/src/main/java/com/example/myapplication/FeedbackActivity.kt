package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.dressroom.FeedbackPromptBuilder
import com.example.myapplication.recommendation.GeminiManager
import com.example.myapplication.model.ClothingItem
import kotlinx.coroutines.launch
import com.example.myapplication.dressroom.GeminiRestManager
import com.google.ai.client.generativeai.GenerativeModel
import androidx.lifecycle.lifecycleScope

class FeedbackActivity : AppCompatActivity() {

    private lateinit var eventInput: EditText
    private lateinit var feedbackText: TextView
    private lateinit var generateBtn: Button
    private lateinit var progressBar: ProgressBar

    private var top: ClothingItem? = null
    private var bottom: ClothingItem? = null
    private var outerwear: ClothingItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)

        setupViews()

        @Suppress("DEPRECATION")
        top = intent.getSerializableExtra("TOP") as? ClothingItem
        @Suppress("DEPRECATION")
        bottom = intent.getSerializableExtra("BOTTOM") as? ClothingItem
        @Suppress("DEPRECATION")
        outerwear = intent.getSerializableExtra("OUTERWEAR") as? ClothingItem

        generateBtn.setOnClickListener {
            val event = eventInput.text.toString().trim()
            if (event.isEmpty()) {
                feedbackText.text = "Please describe the event/occasion"
                return@setOnClickListener
            }
            generateFeedback(event)
        }
    }

    private fun setupViews() {
        eventInput = findViewById(R.id.eventInput)
        feedbackText = findViewById(R.id.feedbackText)
        generateBtn = findViewById(R.id.generateBtn)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun generateFeedback(event: String) {
        progressBar.visibility = View.VISIBLE
        feedbackText.text = "Generating feedback..."

        val prompt = FeedbackPromptBuilder.buildPrompt(top, bottom, outerwear, event)

        lifecycleScope.launch {
            val feedback = GeminiRestManager.generateFeedback(prompt)
            progressBar.visibility = View.GONE
            feedbackText.text = feedback
        }
    }
}