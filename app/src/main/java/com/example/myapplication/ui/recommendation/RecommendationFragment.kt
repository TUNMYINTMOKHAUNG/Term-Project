package com.example.myapplication.ui.recommendation

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.myapplication.closet.ClothingAdapter
import com.example.myapplication.databinding.FragmentRecommendationBinding
import com.example.myapplication.model.ClothingItem
import com.example.myapplication.recommendation.WeatherRecommendationManager
import com.google.firebase.firestore.FirebaseFirestore

class RecommendationFragment : Fragment() {

    private var _binding: FragmentRecommendationBinding? = null
    private val binding get() = _binding!!

    private lateinit var clothingAdapter: ClothingAdapter
    private var baselineThickness: String = "Medium"

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
        val condition = arguments?.getString("CONDITION") ?: "Clear"
        val windSpeed = arguments?.getDouble("WIND_SPEED") ?: 0.0
        val humidity = arguments?.getInt("HUMIDITY") ?: 50

        val advice = WeatherRecommendationManager.getWeatherAdvice(temp, condition, windSpeed, humidity)
        baselineThickness = advice.thickness

        binding.tvWeatherStatus.text = advice.summary
        binding.tvWeatherTip.text = "Recommended setup: Layer pieces with ${advice.thickness.lowercase()} density."

        // ── DYNAMIC WEATHER LOGO LOGIC ──────────────────────────────────────
        // Evaluates condition inputs and swaps out a clean matching vector graphic layout asset
        val weatherIconRes = when {
            condition.contains("Rain", ignoreCase = true) || condition.contains("Drizzle", ignoreCase = true) ->
                android.R.drawable.ic_menu_myplaces // Replace with your clean rain vector asset later
            condition.contains("Snow", ignoreCase = true) ->
                android.R.drawable.button_onoff_indicator_on
            condition.contains("Cloud", ignoreCase = true) ->
                android.R.drawable.ic_menu_gallery
            else -> android.R.drawable.ic_menu_compass // Default clean system navigation/sun asset representation
        }
        binding.ivWeatherIcon.setImageResource(weatherIconRes)

        // ── UPDATE GRID ITEM RECYCLER INTERACTION ──────────────────────────
        // Passing a click block to the clothing adapter ensures touching ANY card goes straight to your ClosetActivity
        clothingAdapter = ClothingAdapter(emptyList()) { selectedItem ->
            navigateToDressroom(selectedItem)
        }

        binding.rvMoreItems.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvMoreItems.adapter = clothingAdapter

        setupChipFilters()
        loadCloudContentRecommendations()
    }

    private fun setupChipFilters() {
        binding.chipAll.isChecked = true
        binding.chipAll.setOnClickListener { clothingAdapter.filterByType("All") }
        binding.chipTops.setOnClickListener { clothingAdapter.filterByType("Top") }
        binding.chipBottoms.setOnClickListener { clothingAdapter.filterByType("Bottom") }
        binding.chipOuterwear.setOnClickListener { clothingAdapter.filterByType("Outerwear") }
        binding.chipOnepiece.setOnClickListener { clothingAdapter.filterByType("One-piece") }
    }

    private fun loadCloudContentRecommendations() {
        FirebaseFirestore.getInstance()
            .collection("clothingItems")
            .whereEqualTo("thickness", baselineThickness)
            .get()
            .addOnSuccessListener { result ->
                val fetchedItems = result.map { it.toObject(ClothingItem::class.java) }

                if (fetchedItems.isEmpty()) {
                    binding.emptyText.visibility = View.VISIBLE
                    binding.rvMoreItems.visibility = View.GONE
                } else {
                    binding.emptyText.visibility = View.GONE
                    binding.rvMoreItems.visibility = View.VISIBLE

                    clothingAdapter.updateList(fetchedItems)
                    populateOotdHeroDisplay(fetchedItems)
                }
            }
    }

    private fun populateOotdHeroDisplay(items: List<ClothingItem>) {
        val topHero = items.find { it.type.equals("Top", ignoreCase = true) }
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
        // Swapped target destination class to point back to your Dressroom Activity
        val intent = Intent(requireContext(), com.example.myapplication.DressroomActivity::class.java).apply {
            putExtra("SELECTED_CLOTHING", item)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}