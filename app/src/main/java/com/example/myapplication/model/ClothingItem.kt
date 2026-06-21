package com.example.myapplication.model

import java.io.Serializable

data class ClothingItem(
    val id: String = "",
    val imageUrl: String = "",
    val type: String = "",
    val name: String = "",
    val color: List<String> = emptyList(),
    val pattern: String = "",
    val styleKeywords: List<String> = emptyList(), // Your working field
    val thickness: String = "",
    val createdAt: Long = System.currentTimeMillis()
) : Serializable