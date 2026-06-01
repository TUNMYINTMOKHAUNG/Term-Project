package com.example.myapplication.recommendation

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiManager {

    private const val GEMINI_API_KEY = ""

    suspend fun generateFeedback(
        prompt: String
    ): String = withContext(Dispatchers.IO) {
        try {
            Log.d("GEMINI_DEBUG", "Key starts with: ${GEMINI_API_KEY.take(10)}")
            Log.d("GEMINI_DEBUG", "Key length: ${GEMINI_API_KEY.length}")
            Log.d("GEMINI_DEBUG", "Key ends with: ${GEMINI_API_KEY.takeLast(4)}")

            val model = GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = ""
            )

            Log.d("GEMINI_DEBUG", "Model created, sending request...")
            val response = model.generateContent(prompt)
            response.text ?: "No feedback received"
        } catch (e: Exception) {
            Log.e("GEMINI_ERROR", "Error: ${e.message}")
            "Error: ${e.message}"
        }
    }
}