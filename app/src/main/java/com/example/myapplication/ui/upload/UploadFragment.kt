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

    private var cameraImageUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            startAnalysis(it.toString())
        }
    }

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            cameraImageUri?.let {
                startAnalysis(it.toString())
            }
        } else {
            Toast.makeText(requireContext(), "Camera cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                launchCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission required.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
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

    private fun startAnalysis(imageUriString: String) {
        val analysisFragment = AnalysisFragment().apply {
            arguments = Bundle().apply {
                putString("IMAGE_URI", imageUriString)
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, analysisFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun checkPermissionAndLaunchCamera() {
        if (
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val photoFile = File.createTempFile(
            "clothing_",
            ".jpg",
            requireContext().cacheDir
        )

        cameraImageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )

        takePicture.launch(cameraImageUri)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}