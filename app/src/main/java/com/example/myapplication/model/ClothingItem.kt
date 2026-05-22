package com.example.myapplication.model

data class ClothingItem(
    val id: String = "",
    val imageUrl: String = "",

    val type: String = "",
    val color: String = "",

    val styleKeywords: List<String> = emptyList(),

    val thickness: String = "",

    val createdAt: Long = System.currentTimeMillis()
)