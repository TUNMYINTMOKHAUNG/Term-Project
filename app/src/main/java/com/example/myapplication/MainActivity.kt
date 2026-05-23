package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import com.example.myapplication.networkapi.RetrofitInstance
import com.example.myapplication.networkapi.WeatherResponse
import com.example.myapplication.RecommendationActivity
import com.example.myapplication.recommendation.WeatherRecommendationManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        RetrofitInstance.api.getCurrentWeather(
            city = "Busan",
            apiKey = "79038bb0b4317e59d829c8518fde3a44"
        ).enqueue(object : Callback<WeatherResponse> {

            override fun onResponse(
                call: Call<WeatherResponse>,
                response: Response<WeatherResponse>
            ) {
                if (response.isSuccessful) {
                    val temp = response.body()?.main?.temp ?: 0.0  // ← inside here
                    Log.d("WEATHER", "Temperature: $temp")

                    val intent = Intent(
                        this@MainActivity,
                        RecommendationActivity::class.java
                    )
                    intent.putExtra("TEMPERATURE", temp)
                    startActivity(intent)  // ← navigate to recommendation screen
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Log.e("WEATHER", "Failed: ${t.message}")
            }
        })
    }
}