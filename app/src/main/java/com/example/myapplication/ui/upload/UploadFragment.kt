package com.example.myapplication.ui.upload

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityUploadBinding // Change to FragmentUploadBinding if you rename your XML file

class UploadFragment : Fragment() {

    // Fragments use a special binding setup to prevent memory leaks
    private var _binding: ActivityUploadBinding? = null
    private val binding get() = _binding!!

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            binding.imgPreview.visibility = View.VISIBLE
            binding.imgPreview.setImageURI(it)
            startAnalysis(it.toString())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityUploadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide the bottom nav setup, MainActivity handles it now!

        binding.btnCamera.setOnClickListener {
            try {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivity(intent)
            } catch (e: Exception) {
                // Handle no camera
            }
        }

        binding.btnGallery.setOnClickListener {
            pickImage.launch("image/*")
        }

        // Removed btnFinalUpload since you auto-navigate on gallery pick
    }

    private fun startAnalysis(imageUri: String) {
        // Use a Bundle to pass the data instead of an Intent
        val bundle = Bundle().apply {
            putString("IMAGE_URI", imageUri)
        }

        val analysisFragment = AnalysisFragment().apply {
            arguments = bundle
        }

        // Swap the fragment inside the MainActivity
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, analysisFragment)
            .addToBackStack(null) // This allows the user to press the back button to return to Upload!
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clean up binding to prevent memory leaks
    }
}