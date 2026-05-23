package com.example.myapplication

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.closet.ClothingAdapter
import com.example.myapplication.data.FirebaseManager
import com.example.myapplication.model.ClothingItem
import com.example.myapplication.recommendation.WeatherRecommendationManager

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

        // Get temp passed from MainActivity
        val temp = intent.getDoubleExtra("TEMPERATURE", 20.0)
        val thickness = WeatherRecommendationManager.getRecommendedThickness(temp)

        weatherText.text = "Today: ${temp}°C — Recommending $thickness clothes"

        // Setup RecyclerView with 2-column grid
        adapter = ClothingAdapter(emptyList())
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter

        loadRecommendedClothes(thickness)
    }

    private fun loadRecommendedClothes(thickness: String) {
        FirebaseManager.firestore
            .collection("clothingItems")
            .whereEqualTo("thickness", thickness)
            .get()
            .addOnSuccessListener { result ->
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