package com.example.myapplication.ui.onboarding

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import android.content.Intent
import com.example.myapplication.MainActivity
import com.example.myapplication.databinding.ActivityPreferencesBinding

class PreferencesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPreferencesBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreferencesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnContinue.setOnClickListener {
            val selectedStyles = getSelectedStyles()
            saveUserPreferences(listOf(), selectedStyles)
        }
    }

    private fun getSelectedStyles(): List<String> {
        val selected = mutableListOf<String>()
        for (i in 0 until binding.chipGroupStyles.childCount) {
            val chip = binding.chipGroupStyles.getChildAt(i)
                    as? Chip
            if (chip?.isChecked == true) selected.add(chip.text.toString())
        }
        return selected
    }

    private fun saveUserPreferences(
        selectedColors: List<String>,
        selectedStyles: List<String>
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
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
    }
}