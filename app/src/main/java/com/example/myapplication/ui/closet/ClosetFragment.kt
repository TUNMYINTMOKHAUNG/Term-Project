package com.example.myapplication.ui.closet

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.myapplication.DressroomActivity // Routes back to your dressroom activity frame
import com.example.myapplication.closet.ClothingAdapter
import com.example.myapplication.databinding.FragmentClosetBinding
import com.example.myapplication.model.ClothingItem
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore

class ClosetFragment : Fragment() {
    private var _binding: FragmentClosetBinding? = null
    private val binding get() = _binding!!

    private lateinit var clothingAdapter: ClothingAdapter
    private var masterClothesList: List<ClothingItem> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClosetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Initialize the grid recycler view
        binding.rvCloset.layoutManager = GridLayoutManager(requireContext(), 2)

        // Connect the click lambda listener target straight to your DressroomActivity
        clothingAdapter = ClothingAdapter(emptyList()) { selectedItem ->
            navigateToDressroom(selectedItem)
        }
        binding.rvCloset.adapter = clothingAdapter

        // 2. Build the navigation tab elements programmatically
        setupTabs()

        // 3. Fetch from the global clothingItems database repository
        loadClosetItemsFromCloud()
    }

    private fun setupTabs() {
        // Adding distinct tab elements matching your data model types exactly
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("All"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Top"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Bottom"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Outerwear"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("One-piece"))

        binding.tabLayout.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    val category = tab?.text?.toString() ?: "All"
                    applyCategoryFilter(category)
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            }
        )
    }

    private fun loadClosetItemsFromCloud() {
        FirebaseFirestore.getInstance()
            .collection("clothingItems")
            .get()
            .addOnSuccessListener { snapshot ->
                masterClothesList = snapshot.toObjects(ClothingItem::class.java)

                // Initialize the page with the default "All" filter setup
                applyCategoryFilter("All")
            }
            .addOnFailureListener { exception ->
                Log.e("CLOSET_FIRESTORE", "Failed to load wardrobe data", exception)
                binding.tvItemCount.text = "Error loading clothes"
                binding.emptyState.visibility = View.VISIBLE
                binding.rvCloset.visibility = View.GONE
            }
    }

    private fun applyCategoryFilter(category: String) {
        // Reset the adapter back to the full list copy, then filter down instantly
        clothingAdapter.updateList(masterClothesList)
        clothingAdapter.filterByType(category)

        // Extract total visible elements to display dynamically
        val visibleCount = clothingAdapter.itemCount
        binding.tvItemCount.text = "$visibleCount items"

        // Handle the visibility of empty notification message state placeholders
        if (visibleCount == 0) {
            binding.emptyState.visibility = View.VISIBLE
            binding.rvCloset.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.rvCloset.visibility = View.VISIBLE
        }
    }

    private fun navigateToDressroom(item: ClothingItem) {
        val intent = Intent(requireContext(), DressroomActivity::class.java).apply {
            putExtra("SELECTED_CLOTHING", item)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}