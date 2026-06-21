package com.example.myapplication.ui.upload

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

object PatternDetector {

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()

    fun init(context: Context) {
        if (interpreter != null) return

        try {
            // Load the .tflite model
            val assetFileDescriptor = context.assets.openFd("pattern_model.tflite")
            val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, assetFileDescriptor.startOffset, assetFileDescriptor.declaredLength)
            interpreter = Interpreter(modelBuffer)

            // Load the labels
            val labelList = mutableListOf<String>()
            BufferedReader(InputStreamReader(context.assets.open("labels.txt"))).use { reader ->
                reader.forEachLine { if (it.isNotBlank()) labelList.add(it.trim()) }
            }
            labels = labelList
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun detectPatternFromImage(bitmap: Bitmap): String {
        val tflite = interpreter ?: return "Other"

        // Resize to 224x224 for MobileNetV2
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val inputBuffer = ByteBuffer.allocateDirect(1 * 224 * 224 * 3 * 4).apply {
            order(ByteOrder.nativeOrder())
        }

        val intValues = IntArray(224 * 224)
        resizedBitmap.getPixels(intValues, 0, resizedBitmap.width, 0, 0, resizedBitmap.width, resizedBitmap.height)

        for (pixelValue in intValues) {
            inputBuffer.putFloat(((pixelValue shr 16) and 0xFF).toFloat()) // R
            inputBuffer.putFloat(((pixelValue shr 8) and 0xFF).toFloat())  // G
            inputBuffer.putFloat((pixelValue and 0xFF).toFloat())          // B
        }

        val output = Array(1) { FloatArray(labels.size) }
        tflite.run(inputBuffer, output)

        // Get the best result
        val probabilities = output[0]
        var maxIdx = 0
        var maxConf = probabilities[0]
        for (i in 1 until probabilities.size) {
            if (probabilities[i] > maxConf) {
                maxConf = probabilities[i]
                maxIdx = i
            }
        }

        // Return the label if confidence is high enough (e.g., > 50%)
        return if (maxConf > 0.5f) labels[maxIdx].replaceFirstChar { it.uppercase() } else "Other"
    }
}