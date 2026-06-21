package com.example.myapplication.networkapi

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"
    private const val COLOR_BASE_URL = "https://www.thecolorapi.com/"
//    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"

    val api: WeatherApiService by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApiService::class.java)
    }

    val colorApi: ColorApiService by lazy {
        Retrofit.Builder()
            .baseUrl(COLOR_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ColorApiService::class.java)
    }

//    val geminiApi: GeminiApiService by lazy {
//        Retrofit.Builder()
//            .baseUrl(GEMINI_BASE_URL)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//            .create(GeminiApiService::class.java)
//    }
}