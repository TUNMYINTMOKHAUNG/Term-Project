package com.example.myapplication.recommendation

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiManager {

    private const val GEMINI_API_KEY = ""

    suspend fun generateFeedback(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val config = generationConfig {
                responseMimeType = "application/json"
            }

            val model = GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = GEMINI_API_KEY,
                generationConfig = config
            )

            val structuredSystemPrompt = """
                You are an expert AI fashion stylist analyzer. Analyze the outfit described against the user's event option constraint.
                You must return your output strictly in JSON using the following structure with no markdown or formatting code outside it:
                {
                  "suitability_score": Int (0 to 100),
                  "suitability_reasons": ["Point 1", "Point 2"],
                  "combination_score": Int (0 to 100),
                  "combination_reasons": ["Point 1", "Point 2"],
                  "alternatives": ["Alternative outfit look 1", "Alternative outfit look 2"],
                  "styling_tips": ["Tip 1", "Tip 2"]
                }
                
                User Request: $prompt
            """.trimIndent()

            val response = model.generateContent(structuredSystemPrompt)
            response.text ?: "{}"
        } catch (e: Exception) {
            e.printStackTrace()
            "{}"
        }
    }
}