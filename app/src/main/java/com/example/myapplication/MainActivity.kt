package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.networkapi.RetrofitInstance
import com.example.myapplication.networkapi.WeatherResponse
import com.example.myapplication.ui.closet.ClosetFragment
import com.example.myapplication.ui.onboarding.LoginActivity
import com.example.myapplication.ui.profile.ProfileFragment
import com.example.myapplication.ui.recommendation.RecommendationFragment
import com.example.myapplication.ui.upload.UploadFragment
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If not logged in, send to login screen first
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        fetchWeatherData()
    }

    private fun fetchWeatherData() {
        RetrofitInstance.api.getCurrentWeather(
            city = "Busan",
            apiKey = "79038bb0b4317e59d829c8518fde3a44"
        ).enqueue(object : Callback<WeatherResponse> {

            override fun onResponse(
                call: Call<WeatherResponse>,
                response: Response<WeatherResponse>
            ) {
                if (response.isSuccessful) {
                    val body = response.body()
                    val temp      = body?.main?.temp ?: 0.0
                    val condition = body?.weather?.firstOrNull()?.main ?: "Clear"
                    val windSpeed = body?.wind?.speed ?: 0.0
                    val humidity  = body?.main?.humidity ?: 50

                    Log.d("WEATHER", "Temp: $temp, Condition: $condition, Wind: $windSpeed, Humidity: $humidity")

                    val bundle = Bundle().apply {
                        putDouble("TEMPERATURE", temp)
                        putString("CONDITION", condition)
                        putDouble("WIND_SPEED", windSpeed)
                        putInt("HUMIDITY", humidity)
                    }

                    val recommendationFragment = RecommendationFragment().apply {
                        arguments = bundle
                    }
                    loadFragment(recommendationFragment)
                } else {
                    loadFragment(RecommendationFragment())
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Log.e("WEATHER", "Failed: ${t.message}")
                loadFragment(RecommendationFragment())
            }
        })
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_trends -> {
                    fetchWeatherData()
                    true
                }
                R.id.nav_closet -> {
                    loadFragment(ClosetFragment())
                    true
                }
                R.id.nav_upload -> {
                    loadFragment(UploadFragment())
                    false
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
