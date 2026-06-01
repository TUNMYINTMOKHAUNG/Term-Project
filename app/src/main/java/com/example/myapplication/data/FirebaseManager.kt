package com.example.myapplication.data

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

object FirebaseManager {
    private val storage = FirebaseStorage.getInstance().reference

    fun uploadClothingImage(
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val fileName = "closet/${UUID.randomUUID()}.jpg"
        val ref = storage.child(fileName)

        ref.putFile(imageUri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    onSuccess(uri.toString())
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}