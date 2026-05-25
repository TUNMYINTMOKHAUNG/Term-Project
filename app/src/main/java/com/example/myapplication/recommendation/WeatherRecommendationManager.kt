package com.example.myapplication.recommendation

object WeatherRecommendationManager {

    data class WeatherAdvice(
        val thickness: String,
        val needsRainGear: Boolean,
        val needsWindBreaker: Boolean,
        val summary: String
    )

    fun getWeatherAdvice(
        temp: Double,
        condition: String,
        windSpeed: Double,
        humidity: Int
    ): WeatherAdvice {

        var adjustedTemp = temp

        // Step 2: Wind — subtract degrees based on wind speed
        val windChill = when {
            windSpeed >= 10.0 -> 4.0   // strong wind, feels 4°C colder
            windSpeed >= 7.0  -> 2.0   // moderate wind, feels 2°C colder
            else              -> 0.0   // calm, no effect
        }
        adjustedTemp -= windChill

        // Step 3: Rain/Snow — feels much colder without waterproof gear
        val needsRainGear = condition in listOf("Rain", "Drizzle", "Thunderstorm", "Snow")
        if (needsRainGear) {
            adjustedTemp -= 3.0  // damp feeling makes it colder
        }

        // Step 4: Humidity
        adjustedTemp += when {
            temp > 25 && humidity >= 70 -> 2.0   // hot + humid = feels hotter
            temp < 10 && humidity >= 70 -> -2.0  // cold + damp = feels colder
            else                        -> 0.0
        }

        // Step 5: Pick thickness from adjusted temp
        val thickness = when {
            adjustedTemp >= 25 -> "Thin"   // Summer: T-shirts, linen, shorts
            adjustedTemp >= 10 -> "Medium" // Spring/Autumn: Hoodies, light jackets, cardigans
            else               -> "Thick"  // Winter: Heavy coats, "padding" (puffer jackets), thermals
        }

        // Wind breaker needed if windy
        val needsWindBreaker = windSpeed >= 7.0

        // Build summary
        val summary = buildString {
            append("${temp}°C (feels like ${String.format("%.1f", adjustedTemp)}°C)")
            append(" · $condition")
            if (needsWindBreaker) append(" · Windy")
            if (humidity >= 70)   append(" · Humid $humidity%")
            if (needsRainGear)    append(" ☔")
        }

        return WeatherAdvice(
            thickness = thickness,
            needsRainGear = needsRainGear,
            needsWindBreaker = needsWindBreaker,
            summary = summary
        )
    }
}