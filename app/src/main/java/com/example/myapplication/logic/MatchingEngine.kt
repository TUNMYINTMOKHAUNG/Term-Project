package com.example.myapplication.logic
import com.example.myapplication.model.ClothingItem

object MatchingEngine {

    fun getRecommendations(
        selected: ClothingItem,
        allItems: List<ClothingItem>
    ): List<ClothingItem> {
        return allItems.filter { item ->
            val isDifferentCategory = item.type != selected.type
            val styleMatch = item.styleKeywords.any { it in selected.styleKeywords }
            val colorMatch = isColorCompatible( //fixed temporarity cuz color should be lists
                selected.color.firstOrNull() ?: "#FFFFFF", // Falls back to white if empty
                item.color.firstOrNull() ?: "#FFFFFF"
            )
            isDifferentCategory && (styleMatch || colorMatch)
        }
    }

    private fun isColorCompatible(c1: String, c2: String): Boolean {
        val neutrals = listOf("White", "Black", "Gray", "#FFFFFF", "#000000")
        return c1 in neutrals || c2 in neutrals
    }
}