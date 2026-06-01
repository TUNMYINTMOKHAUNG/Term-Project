package com.example.myapplication.ui.feedback

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityFeedbackBinding
import com.example.myapplication.model.ClothingItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.os.Parcelable


class FeedbackActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFeedbackBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val outfit = intent.getSerializableExtra("OUTFIT") as? ArrayList<ClothingItem> ?: emptyList()
        val event = intent.getStringExtra("EVENT") ?: "Casual"
        generateStylingFeedback(outfit, event)
    }

    private fun generateStylingFeedback(outfit: List<ClothingItem>, event: String) {
        val prompt = """
            I am wearing an outfit consisting of:
            ${outfit.joinToString("\n") { "- ${it.type}: ${it.color} ${it.style.joinToString()}" }}
            Event: $event
            
            Provide a brief styling analysis (max 3 sentences) and one Expert Tip.
            Focus on color harmony and appropriateness for the event.
        """.trimIndent()

        // Call Gemini API with this prompt via Retrofit
        // geminiService.generateContent(prompt).enqueue(callback)
        binding.feedbackView.text = "Analyzing your outfit..."
    }
}