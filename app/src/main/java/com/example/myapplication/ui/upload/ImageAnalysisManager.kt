package com.example.myapplication.ui.upload

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

object ImageAnalysisManager {

    private const val MODEL_PATH = "fashion_model_cnn.tflite"
    private const val INPUT_SIZE = 28

    fun analyzeImageOnDevice(
        context: Context,
        imageUri: Uri,
        onComplete: (ClothingAnalysis) -> Unit
    ) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) {
                onComplete(ClothingAnalysis("Unknown", listOf("#000000"), listOf("Black"), listOf("Casual"), "Medium"))
                return
            }

            val softwareBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val scaledBitmap = Bitmap.createScaledBitmap(softwareBitmap, INPUT_SIZE, INPUT_SIZE, true)
            val fashionLabel = runCustomFashionInference(context, scaledBitmap)

            val (parentType, thickness) = when (fashionLabel) {
                "T-shirt/top", "Shirt", "Pullover" -> Pair("Top", "Thin")
                "Trouser" -> Pair("Bottom", "Medium")
                "Dress" -> Pair("One-piece", "Medium")
                "Coat", "Ankle boot" -> Pair("Outerwear", "Thick")
                else -> Pair("Bottom", "Medium")
            }

            val (topHexes, topNames) = extractTopColors(scaledBitmap)

            val analysis = ClothingAnalysis(
                type = parentType,
                color_hexes = topHexes,
                color_names = topNames,
                styles = listOf("Casual", "Streetwear"),
                thickness = thickness
            )
            onComplete(analysis)

        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(
                ClothingAnalysis(
                    type = "Error",
                    color_hexes = listOf("#FF0000"),
                    color_names = listOf("Error"),
                    styles = listOf("Error"),
                    thickness = "Medium"
                )
            )
        }
    }

    private fun runCustomFashionInference(context: Context, bitmap: Bitmap): String {
        val assetFileDescriptor = context.assets.openFd(MODEL_PATH)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

        val options = Interpreter.Options()
        val interpreter = Interpreter(modelBuffer, options)

        val byteBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 1 * 4).apply {
            order(ByteOrder.nativeOrder())
        }

        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val value = intValues[pixel++]
                val r = (value shr 16 and 0xFF)
                val g = (value shr 8 and 0xFF)
                val b = (value and 0xFF)
                val grayscale = (r + g + b) / 3.0f / 255.0f
                byteBuffer.putFloat(grayscale)
            }
        }

        val outputProbabilityArray = Array(1) { FloatArray(10) }
        interpreter.run(byteBuffer, outputProbabilityArray)
        interpreter.close()

        val maxIndex = outputProbabilityArray[0].indices.maxByOrNull { outputProbabilityArray[0][it] } ?: 0

        val fashionLabels = listOf(
            "T-shirt/top", "Trouser", "Pullover", "Dress", "Coat",
            "Sandal", "Shirt", "Sneaker", "Bag", "Ankle boot"
        )
        return fashionLabels[maxIndex]
    }

    private fun extractTopColors(bitmap: Bitmap): Pair<List<String>, List<String>> {
        val colorCounts = HashMap<Int, Int>()

        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(x, y)
                val rgbColor = pixel or -0x1000000

                val r = Color.red(rgbColor)
                val g = Color.green(rgbColor)
                val b = Color.blue(rgbColor)

                if (r > 210 && g > 210 && b > 210) {
                    continue
                }

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
            .distinctBy { getClosestFriendlyColorName(it) }
            .take(3)

        val finalColors = if (uniqueColors.isEmpty()) {
            listOf(bitmap.getPixel(bitmap.width / 2, bitmap.height / 2))
        } else {
            uniqueColors
        }

        val hexList = finalColors.map { String.format("#%06X", 0xFFFFFF and it) }
        val nameList = finalColors.map { getClosestFriendlyColorName(it) }

        return Pair(hexList, nameList)
    }

    private fun getClosestFriendlyColorName(color: Int): String {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)

        return when {
            r > 200 && g > 200 && b > 200 -> "White"
            r < 50 && g < 50 && b < 50 -> "Black"
            g > r && g > b -> "Green"
            r > 130 && b > 130 && g < 100 -> "Purple"
            r > 150 && g < 100 && b < 100 -> "Red"
            b > 150 && r < 100 && g < 100 -> "Blue"
            else -> "Grey"
        }
    }
}