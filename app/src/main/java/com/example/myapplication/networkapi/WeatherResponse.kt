package com.example.myapplication.networkapi

data class WeatherResponse(
    val main: Main,
    val weather: List<WeatherCondition>,
    val wind: Wind
)

data class Main(
    val temp: Double,
    val humidity: Int
)

data class WeatherCondition(
    val main: String,        // "Rain", "Clear", "Clouds", "Snow", "Thunderstorm"
    val description: String  // "light rain", "scattered clouds" etc
)

data class Wind(
    val speed: Double        // meters/second
)