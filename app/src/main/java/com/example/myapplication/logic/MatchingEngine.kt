package com.example.myapplication.logic
import com.example.myapplication.model.ClothingItem

object MatchingEngine {

    fun getRecommendations(
        selected: ClothingItem,
        allItems: List<ClothingItem>
    ): List<ClothingItem> {
        return allItems.filter { item ->
            val isDifferentCategory = item.type != selected.type
            val styleMatch = item.style.any { it in selected.style }
            val colorMatch = isColorCompatible(selected.color, item.color)
            isDifferentCategory && (styleMatch || colorMatch)
        }
    }

    private fun isColorCompatible(c1: String, c2: String): Boolean {
        val neutrals = listOf("White", "Black", "Gray", "#FFFFFF", "#000000")
        return c1 in neutrals || c2 in neutrals
    }
}