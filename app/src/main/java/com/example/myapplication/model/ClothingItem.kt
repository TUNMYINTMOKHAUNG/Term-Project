package com.example.myapplication.model

import java.io.Serializable // Import this

data class ClothingItem(
    val id: String = "",
    val type: String = "",
    val color: String = "",
    val style: List<String> = emptyList(),
    val imageUrl: String = ""
) : Serializable