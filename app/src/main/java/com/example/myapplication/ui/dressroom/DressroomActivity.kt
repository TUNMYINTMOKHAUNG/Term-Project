package com.example.myapplication.ui.dressroom

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.myapplication.databinding.ActivityDressroomBinding
import com.example.myapplication.logic.MatchingEngine
import com.example.myapplication.model.ClothingItem
import com.example.myapplication.ui.feedback.FeedbackActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DressroomActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDressroomBinding
    private val db = FirebaseFirestore.getInstance()
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Tracks what's currently placed in each slot
    private var selectedTop: ClothingItem? = null
    private var selectedBottom: ClothingItem? = null
    private var selectedOuterwear: ClothingItem? = null
    private var selectedFootwear: ClothingItem? = null

    // All closet items fetched from Firestore
    private var allItems: List<ClothingItem> = emptyList()

    // Which slot is waiting for a pick (used when ClosetPickerBottomSheet returns)
    private var activeSlot: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDressroomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        loadAllItems()
        setupSlotClickListeners()

        binding.btnGetFeedback.setOnClickListener { launchFeedback() }
    }

    // ── Data loading ────────────────────────────────────────────────────────

    private fun loadAllItems() {
        db.collection("users").document(uid).collection("closet")
            .get()
            .addOnSuccessListener { snapshot ->
                allItems = snapshot.toObjects(ClothingItem::class.java)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load closet", Toast.LENGTH_SHORT).show()
            }
    }

    // ── Slot click listeners ────────────────────────────────────────────────

    private fun setupSlotClickListeners() {
        binding.slotOuterwear.setOnClickListener { pickItemForSlot("Outerwear") }
        binding.slotTop.setOnClickListener { pickItemForSlot("Top") }
        binding.slotBottom.setOnClickListener { pickItemForSlot("Bottom") }
        binding.slotFootwear.setOnClickListener { pickItemForSlot("Footwear") }

        binding.btnClearTop.setOnClickListener { clearSlot("Top") }
        binding.btnClearBottom.setOnClickListener { clearSlot("Bottom") }
    }

    /**
     * Opens a simple picker: filter allItems by category and let the user tap one.
     * In a full implementation this would be a BottomSheetDialogFragment.
     * Here we show a quick AlertDialog with item names for clarity.
     */
    private fun pickItemForSlot(category: String) {
        activeSlot = category
        val candidates = allItems.filter { it.type == category }

        if (candidates.isEmpty()) {
            Toast.makeText(this, "No $category items in your closet yet", Toast.LENGTH_SHORT).show()
            return
        }

        val names = candidates.map { "${it.type}  ·  ${it.color}" }.toTypedArray()

        android.app.AlertDialog.Builder(this)
            .setTitle("Pick a $category")
            .setItems(names) { _, idx -> onItemPicked(candidates[idx]) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun onItemPicked(item: ClothingItem) {
        when (activeSlot) {
            "Top" -> { selectedTop = item; bindSlot(item, binding.imgTop, binding.emptyTop, binding.btnClearTop) }
            "Bottom" -> { selectedBottom = item; bindSlot(item, binding.imgBottom, binding.emptyBottom, binding.btnClearBottom) }
            "Outerwear" -> { selectedOuterwear = item; bindSlotNoClose(item, binding.imgOuterwear, binding.emptyOuterwear) }
            "Footwear" -> { selectedFootwear = item; bindSlotNoClose(item, binding.imgFootwear, binding.emptyFootwear) }
        }
        refreshRecommendations()
    }

    // ── Slot UI helpers ─────────────────────────────────────────────────────

    private fun bindSlot(
        item: ClothingItem,
        imageView: android.widget.ImageView,
        emptyState: View,
        clearBtn: android.widget.ImageButton
    ) {
        emptyState.visibility = View.GONE
        imageView.visibility = View.VISIBLE
        clearBtn.visibility = View.VISIBLE
        Glide.with(this).load(item.imageUrl).centerCrop().into(imageView)
    }

    private fun bindSlotNoClose(
        item: ClothingItem,
        imageView: android.widget.ImageView,
        emptyState: View
    ) {
        emptyState.visibility = View.GONE
        imageView.visibility = View.VISIBLE
        Glide.with(this).load(item.imageUrl).centerCrop().into(imageView)
    }

    private fun clearSlot(category: String) {
        when (category) {
            "Top" -> {
                selectedTop = null
                binding.imgTop.visibility = View.GONE
                binding.emptyTop.visibility = View.VISIBLE
                binding.btnClearTop.visibility = View.GONE
            }
            "Bottom" -> {
                selectedBottom = null
                binding.imgBottom.visibility = View.GONE
                binding.emptyBottom.visibility = View.VISIBLE
                binding.btnClearBottom.visibility = View.GONE
            }
        }
        refreshRecommendations()
    }

    // ── Recommendations ─────────────────────────────────────────────────────

    /**
     * After any slot change, use MatchingEngine to suggest items
     * that complement the currently selected anchor piece.
     * Anchor priority: Top > Bottom > Outerwear.
     */
    private fun refreshRecommendations() {
        val anchor = selectedTop ?: selectedBottom ?: selectedOuterwear ?: return

        val recommendations = MatchingEngine.getRecommendations(anchor, allItems)

        if (recommendations.isEmpty()) {
            binding.tvRecommendTitle.visibility = View.GONE
            binding.rvRecommendations.visibility = View.GONE
            return
        }

        binding.tvRecommendTitle.visibility = View.VISIBLE
        binding.rvRecommendations.visibility = View.VISIBLE

        // Re-use item_clothing_card via a lightweight inline adapter
        binding.rvRecommendations.layoutManager = GridLayoutManager(this, 2)
        binding.rvRecommendations.adapter = RecommendationAdapter(recommendations) { picked ->
            activeSlot = picked.type
            onItemPicked(picked)
        }
    }

    // ── AI Feedback ─────────────────────────────────────────────────────────

    private fun launchFeedback() {
        val outfit = listOfNotNull(selectedOuterwear, selectedTop, selectedBottom, selectedFootwear)
        if (outfit.isEmpty()) {
            Toast.makeText(this, "Add at least one item to your outfit", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, FeedbackActivity::class.java).apply {
            // Use putExtra instead of putParcelableArrayListExtra
            putExtra("OUTFIT", ArrayList(outfit))
            putExtra("EVENT", "Casual")
        }
        startActivity(intent)
    }
}