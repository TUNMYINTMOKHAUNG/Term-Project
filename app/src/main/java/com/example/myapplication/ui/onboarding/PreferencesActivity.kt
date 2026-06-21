package com.example.myapplication.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.MainActivity
import com.example.myapplication.databinding.ActivityPreferencesBinding
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class PreferencesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPreferencesBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreferencesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnContinue.setOnClickListener {
            val selectedStyles = getCheckedChips(binding.chipGroupStyles)
            val selectedColors = getCheckedChips(binding.chipGroupColors)
            saveUserPreferences(selectedColors, selectedStyles)
        }
    }

    private fun getCheckedChips(chipGroup: com.google.android.material.chip.ChipGroup): List<String> {
        val selected = mutableListOf<String>()
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            if (chip?.isChecked == true) selected.add(chip.text.toString())
        }
        return selected
    }

    private fun saveUserPreferences(
        selectedColors: List<String>,
        selectedStyles: List<String>
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Please log in first.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnContinue.isEnabled = false

        val prefs = hashMapOf(
            "preferredColors" to selectedColors,
            "preferredStyles" to selectedStyles
        )
        db.collection("users").document(userId)
            .set(prefs, SetOptions.merge())
            .addOnSuccessListener {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                binding.btnContinue.isEnabled = true
                Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
