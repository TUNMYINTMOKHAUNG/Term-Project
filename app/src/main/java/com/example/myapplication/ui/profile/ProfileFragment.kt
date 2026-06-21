package com.example.myapplication.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
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

        // Load preferences from Firestore
        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                val styles = doc.get("preferredStyles") as? List<String> ?: emptyList()
                val colors = doc.get("preferredColors") as? List<String> ?: emptyList()
                displayChips(binding.chipGroupStyles, styles, "No styles selected yet")
                displayChips(binding.chipGroupColors, colors, "No colors selected yet")
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Could not load preferences.", Toast.LENGTH_SHORT).show()
            }

        // Load closet item count
        db.collection("clothingItems")
            .get()
            .addOnSuccessListener { snapshot ->
                binding.tvItemCount.text = "${snapshot.size()} items in closet"
            }
    }

    private fun displayChips(
        chipGroup: com.google.android.material.chip.ChipGroup,
        items: List<String>,
        emptyMessage: String
    ) {
        chipGroup.removeAllViews()
        if (items.isEmpty()) {
            val chip = Chip(requireContext()).apply {
                text = emptyMessage
                isClickable = false
                isCheckable = false
            }
            chipGroup.addView(chip)
            return
        }
        items.forEach { item ->
            val chip = Chip(requireContext()).apply {
                text = item
                isClickable = false
                isCheckable = false
            }
            chipGroup.addView(chip)
        }
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
