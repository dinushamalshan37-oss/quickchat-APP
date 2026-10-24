package com.buddy.quickchat.data.repository

import android.net.Uri
import com.buddy.quickchat.util.UiState
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class StorageRepository @Inject constructor(
    private val storage: FirebaseStorage
) {
    suspend fun uploadProfileImage(uid: String, uri: Uri): UiState<String> {
        return try {
            val ref = storage.reference.child("profiles/$uid.jpg")
            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            UiState.Success(downloadUrl)
        } catch (e: Exception) {
            UiState.Error(e.message ?: "Failed to upload image")
        }
    }
}
