package com.example.myapplication.model

data class ClothingItem(
    val id: String = "",
    val imageUrl: String = "",

    val type: String = "",
    val name: String = "",
    val color: List<String> = emptyList(),
    val pattern: String = "",

    val styleKeywords: List<String> = emptyList(),

    val thickness: String = "",

    val createdAt: Long = System.currentTimeMillis()
)