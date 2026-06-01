package com.example.myapplication.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ClothingItem(
    val id: String = "",
    val imageUrl: String = "",
    val type: String = "",      // "Top", "Bottom", "Outerwear", "Footwear", "Accessory"
    val color: String = "",     // Hex code e.g. "#5D5CDE" or name e.g. "Lavender"
    val style: List<String> = emptyList(), // ["Casual", "Minimalist", "Streetwear"]
    val thickness: String = "Medium",       // "Thin", "Medium", "Thick"
    val isAvailable: Boolean = true
) : Parcelable