package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadFragment(RecommendationFragment())

        setupBottomNavigation()
        fetchWeatherData()
    }

    private fun fetchWeatherData() {
        RetrofitInstance.api.getCurrentWeather(
            city = "Busan",
            apiKey = "79038bb0b4317e59d829c8518fde3a44"
        ).enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    val body = response.body() ?: return
                    val bundle = Bundle().apply {
                        putDouble("TEMPERATURE", body.main?.temp ?: 0.0)
                        putString("CONDITION", body.weather?.firstOrNull()?.main ?: "Clear")
                        putDouble("WIND_SPEED", body.wind?.speed ?: 0.0)
                        putInt("HUMIDITY", body.main?.humidity ?: 50)
                    }

                    val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                    if (currentFragment is RecommendationFragment) {
                        currentFragment.arguments = bundle

                    }
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Log.e("WEATHER", "Failed: ${t.message}")
            }
        })
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)

            when (item.itemId) {
                R.id.nav_trends -> {
                    if (currentFragment !is RecommendationFragment) {
                        loadFragment(RecommendationFragment())
                        fetchWeatherData()
                    }
                    true
                }
                R.id.nav_closet -> {
                    if (currentFragment !is ClosetFragment) loadFragment(ClosetFragment())
                    true
                }
                R.id.nav_upload -> {
                    if (currentFragment !is UploadFragment) loadFragment(UploadFragment())
                    true
                }
                R.id.nav_profile -> {
                    if (currentFragment !is ProfileFragment) loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}