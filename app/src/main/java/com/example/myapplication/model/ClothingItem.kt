package com.example.myapplication.model

import com.google.firebase.firestore.PropertyName
import java.io.Serializable

data class ClothingItem(
    val id: String = "",
    val imageUrl: String = "",
    val name: String = "",
    val type: String = "",
    val color: List<String> = emptyList(),
    val styleKeywords: List<String> = emptyList(),
    val thickness: String = "",
    val pattern: String = "",
    val ownerId: String = "",

    @get:PropertyName("isFavorite")
    @set:PropertyName("isFavorite")
    var isFavorite: Boolean = false
) : Serializable