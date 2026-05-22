package com.example.myapplication.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

object FirebaseManager {

    val firestore = FirebaseFirestore.getInstance()

    val storage = FirebaseStorage.getInstance()
}