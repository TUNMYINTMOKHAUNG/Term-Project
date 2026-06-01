package com.example.myapplication.ui.closet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.myapplication.databinding.FragmentClosetBinding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.tabs.TabLayout

class ClosetFragment : Fragment() {
    private var _binding: FragmentClosetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClosetBinding.inflate(inflater, container, false)
        binding.rvCloset.layoutManager = GridLayoutManager(context, 2)
        setupTabs()
        loadClosetItems("All")
        return binding.root
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    loadClosetItems(tab?.text.toString())
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            }
        )
    }

    private fun loadClosetItems(category: String) {
        // Fetch from Firestore:
        // db.collection("users").document(uid).collection("closet")
        //   .let { if (category != "All") it.whereEqualTo("type", category) else it }
        //   .get()
        //   .addOnSuccessListener { docs -> adapter.submitList(docs.toObjects()) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}