package com.example.myapplication.ui.upload

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityUploadBinding

class UploadActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUploadBinding

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            binding.imgPreview.visibility = View.VISIBLE
            binding.imgPreview.setImageURI(it)
            startAnalysis(it.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnCamera.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivity(intent)
        }

        binding.btnGallery.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnFinalUpload.setOnClickListener {
            startActivity(Intent(this, AnalysisActivity::class.java))
        }
    }

    private fun startAnalysis(imageUri: String) {
        val intent = Intent(this, AnalysisActivity::class.java).apply {
            putExtra("IMAGE_URI", imageUri)
        }
        startActivity(intent)
    }
}