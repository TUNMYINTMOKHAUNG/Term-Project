package com.example.myapplication.ui.profile

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentProfileBinding
import com.example.myapplication.ui.onboarding.LoginActivity
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Same color name -> hex map used on the Preferences page, so swatches match exactly
    private val colorNameHexMap = mapOf(
        "Black"    to "#1A1A1A",
        "White"    to "#FFFFFF",
        "Grey"     to "#9E9E9E",
        "Beige"    to "#D8C3A5",
        "Brown"    to "#6B4226",
        "Navy"     to "#1B2A4A",
        "Blue"     to "#2E5BFF",
        "Sky Blue" to "#87CEEB",
        "Green"    to "#2E7D32",
        "Olive"    to "#6B8E23",
        "Red"      to "#D32F2F",
        "Burgundy" to "#6D1B2C",
        "Pink"     to "#F48FB1",
        "Pastel"   to "#F3D9E0",
        "Purple"   to "#7E57C2",
        "Yellow"   to "#FBC02D",
        "Orange"   to "#F57C00"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserProfile()

        binding.btnLogout.setOnClickListener {
            confirmLogout()
        }

        binding.btnEditPreferences.setOnClickListener {
            startActivity(Intent(requireContext(), com.example.myapplication.ui.onboarding.PreferencesActivity::class.java))
        }
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        if (user == null) {
            goToLogin()
            return
        }

        binding.tvEmail.text = user.email ?: "No email"

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                val styles = asStringList(doc.get("preferredStyles"))
                val colors = asStringList(doc.get("preferredColors"))
                displayStyleChips(styles)
                displayColorChips(colors)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Could not load preferences.", Toast.LENGTH_SHORT).show()
            }

        db.collection("clothingItems")
            .get()
            .addOnSuccessListener { snapshot ->
                binding.tvItemCount.text = "${snapshot.size()} items in closet"
            }
    }

    // Safely converts Firestore's Any? into a List<String>, item by item,
    // avoiding the unchecked-cast warning from a direct "as? List<String>"
    private fun asStringList(value: Any?): List<String> {
        return (value as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
    }

    private fun displayStyleChips(items: List<String>) {
        binding.chipGroupStyles.removeAllViews()
        if (items.isEmpty()) {
            addPlaceholderChip(binding.chipGroupStyles, "No styles selected yet")
            return
        }
        items.forEach { style ->
            val chip = Chip(requireContext()).apply {
                text = style
                isClickable = false
                isCheckable = false
            }
            binding.chipGroupStyles.addView(chip)
        }
    }

    // FIX: colors now show their actual color as a swatch dot, same as the Preferences page
    private fun displayColorChips(items: List<String>) {
        binding.chipGroupColors.removeAllViews()
        if (items.isEmpty()) {
            addPlaceholderChip(binding.chipGroupColors, "No colors selected yet")
            return
        }
        items.forEach { colorName ->
            val hex = colorNameHexMap[colorName]
            val chip = Chip(requireContext()).apply {
                text = colorName
                isClickable = false
                isCheckable = false
                if (hex != null) {
                    chipIcon = androidx.core.content.ContextCompat.getDrawable(
                        requireContext(), R.drawable.ic_color_swatch
                    )
                    chipIconTint = android.content.res.ColorStateList.valueOf(Color.parseColor(hex))
                    isChipIconVisible = true

                    // Light colors need a visible border against the white background
                    if (hex == "#FFFFFF" || hex == "#F3D9E0") {
                        chipStrokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#CCCCCC"))
                        chipStrokeWidth = 2f
                    }
                }
            }
            binding.chipGroupColors.addView(chip)
        }
    }

    private fun addPlaceholderChip(chipGroup: com.google.android.material.chip.ChipGroup, message: String) {
        val chip = Chip(requireContext()).apply {
            text = message
            isClickable = false
            isCheckable = false
        }
        chipGroup.addView(chip)
    }

    private fun confirmLogout() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Log out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log out") { _, _ -> performLogout() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        auth.signOut()
        goToLogin()
    }

    private fun goToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}