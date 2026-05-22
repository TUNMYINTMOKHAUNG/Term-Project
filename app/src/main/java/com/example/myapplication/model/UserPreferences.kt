package com.example.myapplication.model

data class UserPreferences(
    val favoriteColors: List<String> = emptyList(),
    val favoriteStyles: List<String> = emptyList()
)