package com.example.myapplication.dressroom

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object GeminiRestManager {

    private const val API_KEY = "" //second api key
    private const val API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
    // ^^^^^^^^^^^ changed

    suspend fun generateFeedback(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            // In GeminiRestManager.kt
            val client = OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS) // Time to establish connection
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)    // Time to wait for data
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)   // Time to send data
                .build()

            val jsonBody = JSONObject().apply {
                put("contents", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            val requestBody = jsonBody.toString()
                .toRequestBody("application/json".toMediaType())

            Log.d("GEMINI_REST", "Request JSON: ${jsonBody.toString(2)}")

            val request = Request.Builder()
                .url("$API_URL?key=$API_KEY")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            Log.d("GEMINI_REST", "Response code: ${response.code}")
            Log.d("GEMINI_REST", "Response body: $responseBody")

            if (response.isSuccessful) {
                val jsonResponse = JSONObject(responseBody)
                val text = jsonResponse
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                text
            } else {
                "Error ${response.code}: $responseBody"
            }
        } catch (e: Exception) {
            Log.e("GEMINI_REST", "Exception: ${e.stackTraceToString()}")
            "Error: ${e.message}"
        }
    }
}