package com.example.myapplication.ui.upload

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

object ImageAnalysisManager {

    private const val FASHION_MODEL_PATH = "fashion_model.tflite"
    private const val PATTERN_MODEL_PATH = "pattern_model.tflite"
    private const val INPUT_SIZE = 224

    suspend fun analyzeImageOnDevice(
        context: Context,
        imageUri: Uri
    ): ClothingAnalysis = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) {
                return@withContext getDefaultErrorAnalysis()
            }

            val softwareBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val scaledBitmap = Bitmap.createScaledBitmap(softwareBitmap, INPUT_SIZE, INPUT_SIZE, true)

            val fashionLabel = runCustomInference(context, scaledBitmap, FASHION_MODEL_PATH, getFashionLabels())
            val patternLabel = runCustomInference(context, scaledBitmap, PATTERN_MODEL_PATH, getPatternLabels())

            val parentType = when (fashionLabel) {
                "Tshirt", "Shirt", "Cardigan", "Hoodie", "Blouse", "Tank top", "Knitwear" -> "Top"
                "Jean pants", "Skirt", "Shorts", "Cargo pants", "Slacks", "Training pants" -> "Bottom"
                "Jacket", "blazer" -> "Outerwear"
                "Dress", "jumpsuit" -> "One-piece"
                else -> "Top"
            }

            val thickness = when (fashionLabel) {
                "Jacket", "blazer", "Cardigan", "Hoodie", "Knitwear" -> "Thick"
                "Tshirt", "Tank top", "Blouse", "Shorts", "Skirt" -> "Thin"
                else -> "Medium"
            }

            val topHexes = extractTopColors(scaledBitmap)

            ClothingAnalysis(
                type = parentType,
                name = fashionLabel,
                pattern = patternLabel,
                confidence = 0.9f,
                color_hexes = topHexes,
                styles = listOf("Casual"),
                thickness = thickness
            )

        } catch (e: Exception) {
            e.printStackTrace()
            getDefaultErrorAnalysis()
        }
    }

    private fun runCustomInference(context: Context, bitmap: Bitmap, modelPath: String, labels: List<String>): String {
        val assetFileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

        val interpreter = Interpreter(modelBuffer, Interpreter.Options())

        val byteBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val value = intValues[pixel++]
                byteBuffer.put((value shr 16 and 0xFF).toByte())
                byteBuffer.put((value shr 8 and 0xFF).toByte())
                byteBuffer.put((value and 0xFF).toByte())
            }
        }

        val outputProbabilityArray = Array(1) { ByteArray(labels.size) }
        interpreter.run(byteBuffer, outputProbabilityArray)
        interpreter.close()

        val maxIndex = outputProbabilityArray[0].indices.maxByOrNull {
            outputProbabilityArray[0][it].toInt() and 0xFF
        } ?: 0

        return labels[maxIndex]
    }

    private fun getFashionLabels(): List<String> {
        return listOf(
            "Dress", "Jean pants", "Cardigan", "Skirt", "Tshirt", "Shirt",
            "Jacket", "Hoodie", "Blouse", "Tank top", "Shorts", "Cargo pants",
            "Slacks", "Training pants", "Knitwear", "jumpsuit", "blazer"
        )
    }

    private fun getPatternLabels(): List<String> {
        return listOf("Plain", "Checkered", "Dotted", "Striped", "Floral")
    }

    private fun extractTopColors(bitmap: Bitmap): List<String> {
        val colorCounts = HashMap<Int, Int>()

        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(x, y)
                val rgbColor = pixel or -0x1000000

                val r = Color.red(rgbColor)
                val g = Color.green(rgbColor)
                val b = Color.blue(rgbColor)

                // Ignore pure white backgrounds
                if (r > 230 && g > 230 && b > 230) continue

                val quantizedR = (r / 24) * 24
                val quantizedG = (g / 24) * 24
                val quantizedB = (b / 24) * 24
                val quantizedColor = Color.rgb(quantizedR, quantizedG, quantizedB)

                colorCounts[quantizedColor] = (colorCounts[quantizedColor] ?: 0) + 1
            }
        }

        val uniqueColors = colorCounts.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(3)

        val finalColors = uniqueColors.ifEmpty { listOf(bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)) }

        return finalColors.map { String.format("#%06X", 0xFFFFFF and it) }
    }

    private fun getClosestFriendlyColorName(color: Int): String {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)

        val knownColors = mapOf(
            "Black" to intArrayOf(0, 0, 0),
            "White" to intArrayOf(255, 255, 255),
            "Red" to intArrayOf(255, 0, 0),
            "Green" to intArrayOf(0, 128, 0),
            "Blue" to intArrayOf(0, 0, 255),
            "Yellow" to intArrayOf(255, 255, 0),
            "Cyan" to intArrayOf(0, 255, 255),
            "Magenta" to intArrayOf(255, 0, 255),
            "Grey" to intArrayOf(128, 128, 128),
            "Light Grey" to intArrayOf(211, 211, 211),
            "Orange" to intArrayOf(255, 165, 0),
            "Brown" to intArrayOf(139, 69, 19),
            "Pink" to intArrayOf(255, 192, 203),
            "Navy" to intArrayOf(0, 0, 128),
            "Beige" to intArrayOf(245, 245, 220),
            "Maroon" to intArrayOf(128, 0, 0),
            "Olive" to intArrayOf(128, 128, 0),
            "Teal" to intArrayOf(0, 128, 128),
            "Khaki" to intArrayOf(240, 230, 140)
        )

        var closestName = "Unknown"
        var minDistance = Double.MAX_VALUE

        for ((name, rgb) in knownColors) {
            val dist = Math.sqrt(
                Math.pow((r - rgb[0]).toDouble(), 2.0) +
                        Math.pow((g - rgb[1]).toDouble(), 2.0) +
                        Math.pow((b - rgb[2]).toDouble(), 2.0)
            )
            if (dist < minDistance) {
                minDistance = dist
                closestName = name
            }
        }
        return closestName
    }

    private fun getDefaultErrorAnalysis() = ClothingAnalysis(
        type = "Unknown",
        name = "Clothing Item",
        pattern = "Plain",
        confidence = 0.0f,
        color_hexes = listOf("#000000"),
        styles = listOf("Casual"),
        thickness = "Medium"
    )
}