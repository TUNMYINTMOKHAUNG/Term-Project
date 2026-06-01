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
import com.google.gson.Gson
import java.util.UUID

class AnalysisActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAnalysisBinding
    private val db = FirebaseFirestore.getInstance()
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        // Load image preview from intent
        val imageUriString = intent.getStringExtra("IMAGE_URI")
        imageUriString?.let {
            binding.imgPreview.setImageURI(Uri.parse(it))
        }

        binding.btnSave.setOnClickListener { saveItemToCloset() }
    }

    fun onAnalysisReceived(jsonResult: String) {
        val analysis = Gson().fromJson(jsonResult, ClothingAnalysis::class.java)
        binding.typeDropdown.setText(analysis.type, false)
        binding.tvColorName.text = "${analysis.color_name} (${analysis.color_hex})"
        binding.colorPreview.setBackgroundColor(
            Color.parseColor(analysis.color_hex)
        )
        analysis.styles.forEach { style -> addStyleChip(style) }
    }

    private fun addStyleChip(style: String) {
        val chip = Chip(this).apply {
            text = style
            isCheckable = true
        }
        binding.chipGroupStyles.addView(chip)
    }

    private fun saveItemToCloset() {
        val item = ClothingItem(
            id = UUID.randomUUID().toString(),
            imageUrl = "",  // Set after Firebase Storage upload
            type = binding.typeDropdown.text.toString(),
            color = binding.tvColorName.text.toString()
        )
        db.collection("users").document(uid)
            .collection("closet").document(item.id)
            .set(item)
            .addOnSuccessListener {
                Toast.makeText(this, "Saved to Closet!", Toast.LENGTH_SHORT).show()
                finish()
            }
    }
}

data class ClothingAnalysis(
    val type: String = "",
    val color_hex: String = "",
    val color_name: String = "",
    val styles: List<String> = emptyList(),
    val thickness: String = ""
)