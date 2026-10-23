package com.buddy.quickchat.data.repository

import com.buddy.quickchat.domain.model.Friend
import com.buddy.quickchat.domain.model.User
import com.buddy.quickchat.util.UiState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val usersRef = firestore.collection("users")

    suspend fun createUser(uid: String, name: String, email: String): UiState<Unit> {
        return try {
            val user = User(uid = uid, name = name, email = email)
            usersRef.document(uid).set(user).await()
            UiState.Success(Unit)
        } catch (e: Exception) {
            UiState.Error(e.message ?: "Failed to create user profile")
        }
    }

    suspend fun getUser(uid: String): UiState<User> {
        return try {
            val snapshot = usersRef.document(uid).get().await()
            val user = snapshot.toObject(User::class.java)
            if (user != null) {
                UiState.Success(user)
            } else {
                UiState.Error("User not found")
            }
        } catch (e: Exception) {
            UiState.Error(e.message ?: "Failed to fetch user")
        }
    }

    suspend fun addFriend(currentUid: String, friendUid: String): UiState<Unit> {
        return try {
            // Fetch friend info
            val friendSnapshot = usersRef.document(friendUid).get().await()
            val friendUser = friendSnapshot.toObject(User::class.java) ?: return UiState.Error("User not found")
            
            // Add friend to current user's friends list
            val friendForCurrent = Friend(
                uid = friendUser.uid,
                name = friendUser.name,
                photoUrl = friendUser.photoUrl,
                addedAt = System.currentTimeMillis()
            )
            usersRef.document(currentUid).collection("friends").document(friendUid).set(friendForCurrent).await()

            // Fetch current user info
            val currentSnapshot = usersRef.document(currentUid).get().await()
            val currentUser = currentSnapshot.toObject(User::class.java) ?: return UiState.Error("Current user not found")
            
            // Add current user to friend's friends list
            val currentForFriend = Friend(
                uid = currentUser.uid,
                name = currentUser.name,
                photoUrl = currentUser.photoUrl,
                addedAt = System.currentTimeMillis()
            )
            usersRef.document(friendUid).collection("friends").document(currentUid).set(currentForFriend).await()

            UiState.Success(Unit)
        } catch (e: Exception) {
            UiState.Error(e.message ?: "Failed to add friend")
        }
    }
}
