package com.example.myapplication.recommendation

object WeatherRecommendationManager {

    fun getRecommendedThickness(temp: Double): String {

        return when {
            temp >= 28 -> "Thin"
            temp >= 16 -> "Medium"
            else -> "Thick"
        }
    }
}