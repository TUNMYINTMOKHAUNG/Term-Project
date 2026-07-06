package com.example.myapplication.recommendation

import android.graphics.Color
import com.example.myapplication.model.ClothingItem
import kotlin.math.pow
import kotlin.math.sqrt

object ColorMatchingManager {
    const val MAX_COLOR_DISTANCE = 441.67

    fun findMatchingClothes(targetHex: String, clothes: List<ClothingItem>, maxMatches: Int): List<ClothingItem> {
        return clothes.sortedByDescending { item ->
            val itemColor = safeParseColor(item.color.firstOrNull() ?: "#FFFFFF") ?: 0xFFFFFF
            val targetColor = safeParseColor(targetHex) ?: 0xFFFFFF

            var score = 500.0 - calculateColorDistance(targetColor, itemColor).coerceAtMost(500.0)

            if (isNeutral(itemColor)) score += 200.0

            score
        }.take(maxMatches)
    }
    fun colorDistance(hex1: String, hex2: String): Double {
        val c1 = safeParseColor(hex1) ?: 0xFFFFFF
        val c2 = safeParseColor(hex2) ?: 0xFFFFFF
        return calculateColorDistance(c1, c2)
    }

    fun isNeutralColor(hex: String): Boolean {
        val color = safeParseColor(hex) ?: return false
        return isNeutral(color)
    }


    private fun isNeutral(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)

        val max = maxOf(r, maxOf(g, b))
        val min = minOf(r, minOf(g, b))

        return (max - min) < 150
    }

    private fun safeParseColor(hex: String): Int? {
        return try {
            val formattedHex = if (!hex.startsWith("#")) "#$hex" else hex
            Color.parseColor(formattedHex)
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateColorDistance(color1: Int, color2: Int): Double {
        val r1 = Color.red(color1)
        val g1 = Color.green(color1)
        val b1 = Color.blue(color1)

        val r2 = Color.red(color2)
        val g2 = Color.green(color2)
        val b2 = Color.blue(color2)

        return sqrt(
            (r2 - r1).toDouble().pow(2) +
                    (g2 - g1).toDouble().pow(2) +
                    (b2 - b1).toDouble().pow(2)
        )
    }
}