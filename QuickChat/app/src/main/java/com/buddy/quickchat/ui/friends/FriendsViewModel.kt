package com.buddy.quickchat.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buddy.quickchat.data.repository.AuthRepository
import com.buddy.quickchat.data.repository.UserRepository
import com.buddy.quickchat.domain.model.Friend
import com.buddy.quickchat.util.UiState
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    private val _addFriendState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val addFriendState: StateFlow<UiState<Unit>> = _addFriendState.asStateFlow()

    init {
        loadFriends()
    }

    private fun loadFriends() {
        val uid = authRepository.currentUser?.uid ?: return
        firestore.collection("users").document(uid).collection("friends")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _friends.value = snapshot.documents.mapNotNull { it.toObject(Friend::class.java) }
                }
            }
    }

    fun addFriend(friendUid: String) {
        val uid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _addFriendState.value = UiState.Loading
            _addFriendState.value = userRepository.addFriend(uid, friendUid)
        }
    }
}
