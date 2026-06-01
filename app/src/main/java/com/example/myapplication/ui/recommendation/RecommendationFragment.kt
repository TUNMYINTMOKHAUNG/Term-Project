package com.example.myapplication.ui.recommendation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.FragmentRecommendationBinding

class RecommendationFragment : Fragment() {
    private var _binding: FragmentRecommendationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecommendationBinding.inflate(inflater, container, false)
        setupWeatherCard()
        setupRecommendationList()
        return binding.root
    }

    private fun setupWeatherCard() {
        // Replace with real Weather API call
        binding.tvWeatherStatus.text = "Sunny, 24°C"
        binding.tvWeatherTip.text = "Perfect for lightweight layers."
    }

    private fun setupRecommendationList() {
        // Use MatchingEngine.getRecommendations(selectedItem, allItems)
        // to populate Outfit of the Day RecyclerView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}