package com.buddy.quickchat.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.buddy.quickchat.util.UiState
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth
) {
    val currentUser get() = auth.currentUser

    suspend fun login(email: String, password: String): UiState<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            UiState.Success(result.user?.uid ?: "")
        } catch (e: Exception) {
            UiState.Error(e.message ?: "Login failed")
        }
    }

    suspend fun register(email: String, password: String): UiState<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            UiState.Success(result.user?.uid ?: "")
        } catch (e: Exception) {
            UiState.Error(e.message ?: "Registration failed")
        }
    }

    fun logout() {
        auth.signOut()
    }
}
