package com.example.myapplication.recommendation

object WeatherRecommendationManager {

    data class WeatherAdvice(
        val thickness: String,
        val summary: String
    )

    fun getWeatherAdvice(
        temp: Double,
        condition: String,
        windSpeed: Double,
        humidity: Int
    ): WeatherAdvice {
        var adjustedTemp = temp
        val lowerCondition = condition.lowercase()

        val windChill = when {
            windSpeed >= 10.0 -> 4.0
            windSpeed >= 7.0 -> 2.0
            else -> 0.0
        }

        adjustedTemp -= windChill

        val isRainy = lowerCondition.contains("rain") ||
                lowerCondition.contains("drizzle") ||
                lowerCondition.contains("thunderstorm") ||
                lowerCondition.contains("snow")

        if (isRainy) {
            adjustedTemp -= 3.0
        }

        adjustedTemp += when {
            temp > 25 && humidity >= 70 -> 2.0
            temp < 10 && humidity >= 70 -> -2.0
            else -> 0.0
        }

        val thickness = when {
            adjustedTemp >= 25 -> "Thin"
            adjustedTemp >= 10 -> "Medium"
            else -> "Thick"
        }

        val summary = buildString {
            append("${String.format("%.1f", temp)}°C")
            append(" (feels like ${String.format("%.1f", adjustedTemp)}°C)")
            append(" · $condition")

            if (windSpeed >= 7.0) {
                append(" · Windy")
            }

            if (humidity >= 70) {
                append(" · Humid $humidity%")
            }

            if (isRainy) {
                append(" · Rainy")
            }
        }

        return WeatherAdvice(
            thickness = thickness,
            summary = summary
        )
    }
}