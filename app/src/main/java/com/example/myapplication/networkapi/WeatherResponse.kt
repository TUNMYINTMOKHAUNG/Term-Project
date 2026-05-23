package com.example.myapplication.networkapi

data class WeatherResponse(
    val main: Main
)

data class Main(
    val temp: Double
)