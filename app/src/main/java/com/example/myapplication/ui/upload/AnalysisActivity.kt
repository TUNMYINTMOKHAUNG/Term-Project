package com.example.myapplication.ui.upload

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityAnalysisBinding
import com.example.myapplication.model.ClothingItem
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class AnalysisActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAnalysisBinding

    private var analyzedColorHexList: List<String> = emptyList()
    private var computedThickness: String = "Medium"

    private val db = FirebaseFirestore.getInstance()
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        // Load image preview from intent
        val imageUriString = intent.getStringExtra("IMAGE_URI")
        imageUriString?.let { uriStr ->
            val uri = Uri.parse(uriStr)
            binding.imgPreview.setImageURI(uri)

            // Runs your multi-color TFLite background manager task on-boot
            ImageAnalysisManager.analyzeImageOnDevice(this, uri) { analysisResult ->
                runOnUiThread {
                    updateUIWithAnalysis(analysisResult)
                }
            }
        }

        binding.btnSave.setOnClickListener { saveItemToCloset() }
    }

    // Handles list models safely and preserves multi-color entries for Firestore caching
    private fun updateUIWithAnalysis(analysis: ClothingAnalysis) {
        binding.typeDropdown.setText(analysis.type, false)

        // Clear any previous color views from the container
        binding.mixedColorsContainer.removeAllViews()

        // Loop through the extracted colors and build UI elements for each
        analysis.color_hexes.forEachIndexed { index, hex ->
            val colorName = analysis.color_names.getOrNull(index) ?: "Unknown"

            // 1. Create a tiny layout container for this individual color capsule
            val itemLayout = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, 0, 24, 0) // Spacing between different colors
            }

            // 2. Create the CardView to act as a perfect circle preview
            val cardView = com.google.android.material.card.MaterialCardView(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(48, 48) // Size: 24dp equivalent
                radius = 24f
                strokeWidth = 0
                cardElevation = 0f
                try {
                    setCardBackgroundColor(Color.parseColor(hex))
                } catch (e: Exception) {
                    setCardBackgroundColor(Color.BLACK)
                }
            }

            // 3. Create the text label for the color
            val textView = android.widget.TextView(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(12, 0, 0, 0)
                }
                text = "$colorName ($hex)"
                textSize = 12f
                setTextColor(Color.parseColor("#4A4A4A"))
            }

            // Combine them together into the row container
            itemLayout.addView(cardView)
            itemLayout.addView(textView)
            binding.mixedColorsContainer.addView(itemLayout)
        }

        binding.chipGroupStyles.removeAllViews()
        analysis.styles.forEach { style -> addStyleChip(style) }

        // Cache the complete array structure for Firestore
        analyzedColorHexList = analysis.color_hexes
        computedThickness = analysis.thickness
    }

    private fun addStyleChip(style: String) {
        val chip = Chip(this).apply {
            text = style
            isCheckable = true
        }
        binding.chipGroupStyles.addView(chip)
    }

    private fun saveItemToCloset() {
        val finalType = binding.typeDropdown.text.toString()
        if (finalType.isBlank()) {
            Toast.makeText(this, "Please verify classification type.", Toast.LENGTH_SHORT).show()
            return
        }

        val item = ClothingItem(
            id = UUID.randomUUID().toString(),
            imageUrl = intent.getStringExtra("IMAGE_URI") ?: "",
            type = finalType,
            color = if (analyzedColorHexList.isNotEmpty()) analyzedColorHexList else listOf("#FFFFFF"),
            thickness = computedThickness
        )

        db.collection("clothingItems")
            .document(item.id)
            .set(item)
            .addOnSuccessListener {
                Toast.makeText(this, "Saved to Wardrobe via TFLite!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

data class ClothingAnalysis(
    val type: String = "",
    val color_hexes: List<String> = emptyList(),
    val color_names: List<String> = emptyList(),
    val styles: List<String> = emptyList(),
    val thickness: String = ""
)