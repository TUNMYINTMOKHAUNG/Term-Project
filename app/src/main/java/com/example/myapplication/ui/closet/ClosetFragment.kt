package com.example.myapplication.ui.closet

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.myapplication.DressroomActivity
import com.example.myapplication.closet.ClothingAdapter
import com.example.myapplication.databinding.FragmentClosetBinding
import com.example.myapplication.model.ClothingItem
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

class ClosetFragment : Fragment() {
    private var _binding: FragmentClosetBinding? = null
    private val binding get() = _binding!!

    private lateinit var clothingAdapter: ClothingAdapter
    private var masterClothesList: List<ClothingItem> = emptyList()
    private var isEditMode: Boolean = false

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

        binding.rvCloset.layoutManager = GridLayoutManager(requireContext(), 2)

        clothingAdapter = ClothingAdapter(
            originalList = emptyList(),
            onItemClick = { selectedItem ->
                if (isEditMode) {
                    showDeleteConfirmation(selectedItem)
                } else {
                    navigateToDressroom(selectedItem)
                }
            }
        )
        binding.rvCloset.adapter = clothingAdapter

        setupTabs()
        loadClosetItemsFromCloud()
        setupEditButton()
    }

    private fun setupTabs() {
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

    private fun setupEditButton() {
        binding.btnEditCloset.setOnClickListener {
            isEditMode = !isEditMode
            clothingAdapter.setEditMode(isEditMode)
            updateEditButtonUI()
            Toast.makeText(
                requireContext(),
                if (isEditMode) "Edit mode ON" else "Edit mode OFF",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateEditButtonUI() {
        binding.btnEditCloset.text = if (isEditMode) "Done" else "Edit"
        binding.btnEditCloset.setTextColor(
            android.graphics.Color.parseColor(if (isEditMode) "#FF6B6B" else "#5D5CDE")
        )
    }

    private fun loadClosetItemsFromCloud() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        FirebaseFirestore.getInstance()
            .collection("clothingItems")
            .whereEqualTo("ownerId", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (_binding == null) return@addOnSuccessListener

                masterClothesList = snapshot.toObjects(ClothingItem::class.java)

                applyCategoryFilter(
                    binding.tabLayout.getTabAt(binding.tabLayout.selectedTabPosition)
                        ?.text
                        ?.toString()
                        ?: "All"
                )
            }
            .addOnFailureListener { exception ->
                Log.e("CLOSET_FIRESTORE", "Failed to load wardrobe data", exception)

                if (_binding == null) return@addOnFailureListener

                binding.tvItemCount.text = "Error loading clothes"
                binding.emptyState.visibility = View.VISIBLE
                binding.rvCloset.visibility = View.GONE
            }
    }

    private fun applyCategoryFilter(category: String) {
        clothingAdapter.updateList(masterClothesList)
        clothingAdapter.filterByType(category)

        val visibleCount = clothingAdapter.itemCount
        binding.tvItemCount.text = if (visibleCount == 1) {
            "1 item"
        } else {
            "$visibleCount items"
        }

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

    private fun showDeleteConfirmation(item: ClothingItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete this from your closet?")
            .setPositiveButton("Yes") { _, _ ->
                deleteItemFromCloset(item)
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteItemFromCloset(item: ClothingItem) {
        FirebaseFirestore.getInstance()
            .collection("clothingItems")
            .document(item.id)
            .delete()
            .addOnSuccessListener {
                Log.d("CLOSET_DELETE", "Item deleted successfully")
                masterClothesList = masterClothesList.filter { it.id != item.id }
                applyCategoryFilter("All")
                Toast.makeText(requireContext(), "Item deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("CLOSET_DELETE", "Failed to delete item", e)
                Toast.makeText(requireContext(), "Failed to delete item", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}