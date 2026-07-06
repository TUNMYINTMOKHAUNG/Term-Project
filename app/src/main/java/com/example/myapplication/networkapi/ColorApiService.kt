package com.example.myapplication.networkapi

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ColorApiService {

    @GET("scheme")
    fun getColorScheme(
        @Query("hex") hex: String,
        @Query("mode") mode: String = "triad",
        @Query("count") count: Int = 5
    ): Call<ColorSchemeResponse>
}

data class ColorSchemeResponse(
    val colors: List<ColorData>
)

data class ColorData(
    val hex: ColorHex
)

data class ColorHex(
    val value: String
)