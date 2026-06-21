package com.example.myapplication.ui.upload

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityUploadBinding
import java.io.File

class UploadFragment : Fragment() {

    private var _binding: ActivityUploadBinding? = null
    private val binding get() = _binding!!

    // Holds the URI of the photo the camera will write to
    private var cameraImageUri: Uri? = null

    // ── Gallery picker (already working) ─────────────────────────────
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            binding.imgPreview.visibility = View.VISIBLE
            binding.imgPreview.setImageURI(it)
            startAnalysis(it.toString())
        }
    }

    // ── Camera capture result launcher (THE FIX) ──────────────────────
    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            binding.imgPreview.visibility = View.VISIBLE
            binding.imgPreview.setImageURI(cameraImageUri)
            startAnalysis(cameraImageUri.toString())
        } else {
            Toast.makeText(requireContext(), "Photo capture cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Camera permission request (THE FIX) ────────────────────────────
    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required to take a photo.", Toast.LENGTH_LONG).show()
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

        binding.btnCamera.setOnClickListener {
            checkPermissionAndLaunchCamera()
        }

        binding.btnGallery.setOnClickListener {
            pickImage.launch("image/*")
        }
    }

    private fun checkPermissionAndLaunchCamera() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            else -> {
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun launchCamera() {
        val photoFile = File.createTempFile(
            "clothing_${System.currentTimeMillis()}_",
            ".jpg",
            requireContext().cacheDir
        )

        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )

        cameraImageUri = uri
        takePicture.launch(uri)
    }

    private fun startAnalysis(imageUri: String) {
        val bundle = Bundle().apply {
            putString("IMAGE_URI", imageUri)
        }

        val analysisFragment = AnalysisFragment().apply {
            arguments = bundle
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, analysisFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}