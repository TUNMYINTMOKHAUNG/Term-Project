package com.example.myapplication.ui.recommendation

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.DressroomActivity
import com.example.myapplication.R
import com.example.myapplication.closet.ClothingAdapter
import com.example.myapplication.databinding.FragmentRecommendationBinding
import com.example.myapplication.model.ClothingItem
import com.example.myapplication.recommendation.ColorMatchingManager
import com.example.myapplication.recommendation.WeatherRecommendationManager
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

class RecommendationFragment : Fragment() {

    private var _binding: FragmentRecommendationBinding? = null
    private val binding get() = _binding!!

    private lateinit var clothingAdapter: ClothingAdapter
    private var baselineThickness: String = "Medium"
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecommendationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val temp = arguments?.getDouble("TEMPERATURE") ?: 20.0
        val condition = arguments?.getString("CONDITION") ?: "Cloud"
        val windSpeed = arguments?.getDouble("WIND_SPEED") ?: 0.0
        val humidity = arguments?.getInt("HUMIDITY") ?: 50

        val advice = WeatherRecommendationManager.getWeatherAdvice(temp, condition, windSpeed, humidity)
        baselineThickness = advice.thickness

        binding.tvWeatherStatus.text = advice.summary
        binding.tvWeatherTip.text = "Recommended setup: Layer pieces with ${advice.thickness.lowercase()} density."

        val weatherIconRes = when {
            condition.contains("Rain", ignoreCase = true) || condition.contains("Drizzle", ignoreCase = true) -> R.drawable.ic_weather_rainy
            condition.contains("Snow", ignoreCase = true) -> R.drawable.ic_weather_snowy
            condition.contains("Cloud", ignoreCase = true) || condition.contains("Mist", ignoreCase = true) -> R.drawable.ic_weather_cloudy
            else -> R.drawable.ic_weather_sunny
        }
        binding.ivWeatherIcon.setImageResource(weatherIconRes)

        binding.rvMoreItems.layoutManager = GridLayoutManager(requireContext(), 2)


        clothingAdapter = ClothingAdapter(
            originalList = emptyList(),
            onItemClick = { selectedItem ->
                navigateToDressroom(selectedItem)
            }
        )
        binding.rvMoreItems.adapter = clothingAdapter

        setupChipFilters()
        loadCloudContentRecommendations()
    }

    private fun setupChipFilters() {
        binding.chipAll?.isChecked = true
        binding.chipAll?.setOnClickListener { clothingAdapter.filterByType("All") }
        binding.chipTops?.setOnClickListener { clothingAdapter.filterByType("Top") }
        binding.chipBottoms?.setOnClickListener { clothingAdapter.filterByType("Bottom") }
        binding.chipOuterwear?.setOnClickListener { clothingAdapter.filterByType("Outerwear") }
        binding.chipOnepiece?.setOnClickListener { clothingAdapter.filterByType("One-piece") }
    }

    private fun loadCloudContentRecommendations() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
            val prefColors = asStringList(userDoc.get("preferredColors"))
            val prefStyles = asStringList(userDoc.get("preferredStyles"))
            val likedIds = asStringList(userDoc.get("likedItems"))

            fetchLikedColorPalette(likedIds) { likedPalette ->
                db.collection("clothingItems")
                    .whereEqualTo("ownerId", uid)
                    .get()
                    .addOnSuccessListener { result ->
                        val allItems = result.toObjects(ClothingItem::class.java)

                        val sortedItems = allItems.filter { it.thickness == baselineThickness }
                            .sortedByDescending { item ->
                                calculateOptimizedScore(item, prefStyles, likedIds, likedPalette)
                            }

                        if (sortedItems.isEmpty()) {
                            binding.emptyText.visibility = View.VISIBLE
                            binding.rvMoreItems.visibility = View.GONE
                            binding.outfitOfDay.visibility = View.GONE
                        } else {
                            binding.emptyText.visibility = View.GONE
                            binding.rvMoreItems.visibility = View.VISIBLE
                            binding.outfitOfDay.visibility = View.VISIBLE

                            clothingAdapter.updateList(sortedItems)
                            populateOotdHeroDisplay(sortedItems)
                        }
                    }
            }
        }.addOnFailureListener { e ->
            Log.e("RECOMMENDATION", "Error loading: ${e.message}")
        }
    }

    private fun asStringList(value: Any?): List<String> = (value as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

    private fun fetchLikedColorPalette(likedClothingIds: List<String>, onComplete: (List<String>) -> Unit) {
        if (likedClothingIds.isEmpty()) { onComplete(emptyList()); return }
        val idsToQuery = likedClothingIds.take(30)
        FirebaseFirestore.getInstance().collection("clothingItems").whereIn(FieldPath.documentId(), idsToQuery).get()
            .addOnSuccessListener { result -> onComplete(result.mapNotNull { it.toObject(ClothingItem::class.java).color.firstOrNull { c -> c.isNotBlank() } }) }
            .addOnFailureListener { onComplete(emptyList()) }
    }

    private fun calculateOptimizedScore(item: ClothingItem, userFavoriteStyles: List<String>, likedClothingIds: List<String>, likedColorPalette: List<String>): Double {
        val weatherScore = 35.0
        val styleScore = if (item.styleKeywords.isNotEmpty()) (item.styleKeywords.count { it in userFavoriteStyles }.toDouble() / item.styleKeywords.size) * 30.0 else 0.0

        val itemColor = item.color.firstOrNull { it.isNotBlank() } ?: "#FFFFFF"
        val colorScore = if (likedColorPalette.isEmpty()) {
            if (ColorMatchingManager.isNeutralColor(itemColor)) 20.0 else 12.0
        } else {
            val closestDistance = likedColorPalette.minOf { ColorMatchingManager.colorDistance(itemColor, it) }
            ((1.0 - (closestDistance / ColorMatchingManager.MAX_COLOR_DISTANCE).coerceIn(0.0, 1.0)) * 16.0) + (if (ColorMatchingManager.isNeutralColor(itemColor)) 4.0 else 0.0)
        }
        val likedScore = if (item.id in likedClothingIds) 15.0 else 0.0
        return weatherScore + styleScore + colorScore + likedScore
    }

    private fun populateOotdHeroDisplay(items: List<ClothingItem>) {
        val topHero = items.find { it.type.equals("Top", ignoreCase = true) || it.type.equals("Outerwear", ignoreCase = true) }
        val bottomHero = items.find { it.type.equals("Bottom", ignoreCase = true) }

        if (topHero != null && isAdded) {
            Glide.with(this).load(topHero.imageUrl).centerCrop().into(binding.imgOotdTop)
            binding.cardOotdTop.setOnClickListener { navigateToDressroom(topHero) }
        }

        if (bottomHero != null && isAdded) {
            Glide.with(this).load(bottomHero.imageUrl).centerCrop().into(binding.imgOotdBottom)
            binding.cardOotdBottom.setOnClickListener { navigateToDressroom(bottomHero) }
        }
    }

    private fun navigateToDressroom(item: ClothingItem) {
        val advice = WeatherRecommendationManager.getWeatherAdvice(
            arguments?.getDouble("TEMPERATURE") ?: 20.0,
            arguments?.getString("CONDITION") ?: "Clear",
            0.0,
            50
        )

        val intent = Intent(requireContext(), DressroomActivity::class.java).apply {
            putExtra("SELECTED_CLOTHING", item)
            putExtra("WEATHER_THICKNESS", advice.thickness)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class EqualSpacingDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            outRect.left = spacing / 2
            outRect.right = spacing / 2
            outRect.top = spacing / 2
            outRect.bottom = spacing / 2
        }
    }
}