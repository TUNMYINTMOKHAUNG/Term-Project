package com.example.myapplication.ui.upload

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityAnalysisBinding // Or FragmentAnalysisBinding if you renamed your XML
import com.example.myapplication.model.ClothingItem
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import android.content.res.ColorStateList

class AnalysisFragment : Fragment() {

    private var _binding: ActivityAnalysisBinding? = null
    private val binding get() = _binding!!

    private var analyzedColorHexList: List<String> = emptyList()
    private var localImageUri: Uri? = null

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Setup Dropdowns (Notice `requireContext()` is used here now!)
        val categories = arrayOf("Top", "Bottom", "Outerwear", "One-piece")
        binding.typeDropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories))

        val garments = arrayOf(
            "T-shirt", "Blouse", "Shirt", "Knitwear", "Sweater", "Tank top", "Jeans",
            "Slacks", "Skirt", "Shorts", "Jacket", "Coat", "Cardigan", "Hoodie", "Dress",
            "Cargo pants", "Training pants", "jumpsuit", "blazer"
        )
        binding.nameDropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, garments))

        val thicknesses = arrayOf("Thin", "Medium", "Thick")
        binding.thicknessDropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, thicknesses))

        val patterns = arrayOf("Plain", "Checkered", "Dotted", "Striped", "Floral")
        binding.patternDropdown.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, patterns)
        )

        binding.patternDropdown.setOnItemClickListener { _, _, _, _ ->
            updatePatternPreviewCircle(binding.patternDropdown.text.toString())
        }

        setupPresetStyleChips()

        val imageUriString = arguments?.getString("IMAGE_URI")
        imageUriString?.let { uriStr ->
            localImageUri = Uri.parse(uriStr)
            binding.imgPreview.setImageURI(localImageUri)

            ImageAnalysisManager.analyzeImageOnDevice(requireContext(), localImageUri!!) { analysisResult ->
                activity?.runOnUiThread {
                    updateUIWithAnalysis(analysisResult)
                }
            }
        }

        binding.btnSave.setOnClickListener { saveItemToCloset() }
    }

    // --- HELPER FUNCTIONS NOW INCLUDED INSIDE THE CLASS ---

    private fun getCorrectedGarment(name: String, type: String): String {
        return when {
            name.contains("Knitwear", true) && type == "Bottom" -> "Skirt"
            name.contains("Shirt", true) && type == "One-piece" -> "Dress"
            else -> name
        }
    }

    private fun setupPresetStyleChips() {
        binding.chipGroupStyles.removeAllViews()
        val fashionKeywords = listOf("Casual", "Streetwear", "Formal", "Minimalist", "Vintage", "Sporty","Y2K", "Chic")

        // 1. Define the rules for when a chip is Checked vs Unchecked
        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked), // Checked state
            intArrayOf(-android.R.attr.state_checked) // Unchecked state (the minus sign means "not")
        )

        // 2. Define the Background Colors
        val bgColors = intArrayOf(
            Color.parseColor("#5D5CDE"), // Checked: Your app's nice purple color
            Color.parseColor("#FFFFFF")  // Unchecked: Clean white
        )

        // 3. Define the Text Colors
        val textColors = intArrayOf(
            Color.WHITE,                  // Checked: White text
            Color.parseColor("#1C1B1F")   // Unchecked: Dark text
        )

        // 4. Define the Border (Stroke) Colors
        val strokeColors = intArrayOf(
            Color.parseColor("#5D5CDE"), // Checked: Purple border
            Color.parseColor("#DCD9DE")  // Unchecked: Light grey border
        )

        fashionKeywords.forEach { keyword ->
            val chip = Chip(requireContext()).apply {
                text = keyword
                isCheckable = true

                // Apply all our custom colors!
                chipBackgroundColor = ColorStateList(states, bgColors)
                setTextColor(ColorStateList(states, textColors))
                chipStrokeColor = ColorStateList(states, strokeColors)
                chipStrokeWidth = 3f // Gives it a nice visible border

                // Add a subtle drop shadow to make it feel like a real button
                elevation = 4f

                // Make sure the little checkmark icon turns white too!
                checkedIconTint = ColorStateList.valueOf(Color.WHITE)
            }
            binding.chipGroupStyles.addView(chip)
        }
    }

    private fun updateUIWithAnalysis(analysis: ClothingAnalysis) {
        binding.typeDropdown.setText(analysis.type, false)
        binding.typeInputLayout.error = null

        val correctedName = getCorrectedGarment(analysis.name, analysis.type)
        binding.nameDropdown.setText(correctedName, false)

        binding.thicknessDropdown.setText(analysis.thickness, false)

        val detectedPattern = analysis.pattern
        val patternOptions = arrayOf("Plain", "Checkered", "Dotted", "Striped", "Floral")
        binding.patternDropdown.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, patternOptions)
        )
        binding.patternDropdown.setText(detectedPattern, false)
        updatePatternPreviewCircle(detectedPattern)

        analyzedColorHexList = analysis.color_hexes
        displayColorPalette(analysis)
    }

    private fun updatePatternPreviewCircle(pattern: String) {
        binding.patternPreviewContainer.removeAllViews()

        val itemLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 12, 0, 12)
        }

        val cardView = MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(180, 180)
            radius = 90f
            strokeWidth = 2
            setStrokeColor(ColorStateList.valueOf(Color.parseColor("#CCCCCC")))
            cardElevation = 2f
            preventCornerOverlap = false
            useCompatPadding = false
        }

        val patternView = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            background = PatternCircleDrawable(pattern)
        }

        cardView.addView(patternView)

        val textView = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(20, 0, 0, 0) }
            text = "Pattern: $pattern"
            setTypeface(null, android.graphics.Typeface.BOLD)
            textSize = 15f
            setTextColor(Color.parseColor("#1C1B1F"))
        }

        itemLayout.addView(cardView)
        itemLayout.addView(textView)
        binding.patternPreviewContainer.addView(itemLayout)
    }

    private fun getSelectedStyleKeywords(): List<String> {
        val selected = mutableListOf<String>()
        for (i in 0 until binding.chipGroupStyles.childCount) {
            val chip = binding.chipGroupStyles.getChildAt(i) as? Chip
            if (chip?.isChecked == true) {
                selected.add(chip.text.toString())
            }
        }
        return selected
    }

    private fun displayColorPalette(analysis: ClothingAnalysis) {
        binding.mixedColorsContainer.removeAllViews()

        val paletteHeader = android.widget.TextView(requireContext()).apply {
            text = "Detected Color Palette"
            setTypeface(null, android.graphics.Typeface.BOLD)
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#1C1B1F"))
            setPadding(0, 8, 0, 12)
        }
        binding.mixedColorsContainer.addView(paletteHeader)

        val horizontalCircleLayout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        analysis.color_hexes.forEach { hex ->
            val cardView = com.google.android.material.card.MaterialCardView(requireContext()).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(80, 80).apply {
                    setMargins(0, 0, 24, 0)
                }
                radius = 40f
                strokeWidth = 1
                strokeColor = android.graphics.Color.parseColor("#EAE3EA")
                cardElevation = 2f
                try {
                    setCardBackgroundColor(android.graphics.Color.parseColor(hex))
                } catch (e: Exception) {
                    setCardBackgroundColor(android.graphics.Color.BLACK)
                }
            }
            horizontalCircleLayout.addView(cardView)
        }

        binding.mixedColorsContainer.addView(horizontalCircleLayout)
    }

    private fun saveItemToCloset() {
        val finalType = binding.typeDropdown.text.toString()
        val finalName = binding.nameDropdown.text.toString()
        val finalThickness = binding.thicknessDropdown.text.toString()
        val finalPattern = binding.patternDropdown.text.toString()

        if (finalType.isBlank() || finalName.isBlank() || finalThickness.isBlank() || finalPattern.isBlank()) {
            Toast.makeText(requireContext(), "Please verify all fields are selected.", Toast.LENGTH_SHORT).show()
            return
        }

        if (uid.isEmpty()) {
            Toast.makeText(requireContext(), "Please log in to save items.", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = localImageUri ?: return

        binding.btnSave.isEnabled = false
        binding.btnSave.text = "Uploading..."

        val itemId = UUID.randomUUID().toString()
        val storageRef = storage.reference.child("clothing_images").child(uid).child("$itemId.jpg")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    val item = ClothingItem(
                        id = itemId,
                        imageUrl = downloadUrl.toString(),
                        name = finalName,
                        type = finalType,
                        color = if (analyzedColorHexList.isNotEmpty()) analyzedColorHexList else listOf("#FFFFFF"),
                        styleKeywords = getSelectedStyleKeywords(),
                        thickness = finalThickness,
                        pattern = finalPattern
                    )

                    db.collection("clothingItems").document(itemId).set(item)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Saved to your closet!", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack() // Go back instead of finish()
                        }
                        .addOnFailureListener { e ->
                            binding.btnSave.isEnabled = true
                            binding.btnSave.text = "Save to Closet"
                            Toast.makeText(requireContext(), "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                binding.btnSave.isEnabled = true
                binding.btnSave.text = "Save to Closet"
                Toast.makeText(requireContext(), "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class ClothingAnalysis(
    val type: String = "",
    val name: String = "",
    val pattern: String = "Plain",
    val confidence: Float = 0.0f,
    val color_hexes: List<String> = emptyList(),
    val styles: List<String> = emptyList(),
    val thickness: String = ""
)