package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.FirebaseManager
import com.example.myapplication.model.ClothingItem
import com.example.myapplication.closet.ClothingAdapter
import com.example.myapplication.recommendation.WeatherRecommendationManager
import com.google.android.material.chip.Chip

class RecommendationActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ClothingAdapter
    private lateinit var weatherText: TextView
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommendation)

        recyclerView = findViewById(R.id.recommendationRecyclerView)
        weatherText = findViewById(R.id.weatherInfoText)
        emptyText = findViewById(R.id.emptyText)

        val temp = intent.getDoubleExtra("TEMPERATURE", 20.0)
        val condition = intent.getStringExtra("CONDITION") ?: "Clear"
        val windSpeed = intent.getDoubleExtra("WIND_SPEED", 0.0)
        val humidity = intent.getIntExtra("HUMIDITY", 50)

        val advice = WeatherRecommendationManager.getWeatherAdvice(
            temp, condition, windSpeed, humidity
        )

        weatherText.text = advice.summary

        // Setup RecyclerView
        // Setup RecyclerView
        adapter = ClothingAdapter(emptyList())
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter

// ← Add chip filter code RIGHT HERE
        val chipAll       = findViewById<Chip>(R.id.chipAll)
        val chipTops      = findViewById<Chip>(R.id.chipTops)
        val chipBottoms   = findViewById<Chip>(R.id.chipBottoms)
        val chipOuterwear = findViewById<Chip>(R.id.chipOuterwear)
        val chipOnepiece = findViewById<Chip>(R.id.chipOnepiece)

        chipAll.isChecked = true
        chipAll.setOnClickListener       { adapter.filterByType("All") }
        chipTops.setOnClickListener      { adapter.filterByType("Top") }
        chipBottoms.setOnClickListener   { adapter.filterByType("Bottom") }
        chipOuterwear.setOnClickListener { adapter.filterByType("Outerwear") }
        chipOnepiece.setOnClickListener { adapter.filterByType("One-piece") }

// Then this stays at the bottom
        loadRecommendedClothes(advice.thickness)
    }

    private fun loadRecommendedClothes(
        thickness: String,
    ) {
        var query = FirebaseManager.firestore
            .collection("clothingItems")
            .whereEqualTo("thickness", thickness)


        query.get()
            .addOnSuccessListener { result ->
                Log.d("FIRESTORE", "Docs found: ${result.size()}")
                val items = result.map { it.toObject(ClothingItem::class.java) }
                if (items.isEmpty()) {
                    emptyText.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    emptyText.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    adapter.updateList(items)
                }
            }
            .addOnFailureListener {
                emptyText.text = "Failed to load clothes"
                emptyText.visibility = View.VISIBLE
            }
    }
}