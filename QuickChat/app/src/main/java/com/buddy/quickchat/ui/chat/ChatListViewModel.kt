package com.buddy.quickchat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buddy.quickchat.data.repository.AuthRepository
import com.buddy.quickchat.data.repository.ChatRepository
import com.buddy.quickchat.data.repository.UserRepository
import com.buddy.quickchat.domain.model.Chat
import com.buddy.quickchat.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val currentUid = authRepository.currentUser?.uid ?: ""

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _friendStatuses = MutableStateFlow<Map<String, User>>(emptyMap())
    val friendStatuses: StateFlow<Map<String, User>> = _friendStatuses.asStateFlow()

    init {
        loadChats()
    }

    private fun loadChats() {
        if (currentUid.isEmpty()) return
        viewModelScope.launch {
            chatRepository.getChats(currentUid).collect { chatList ->
                _chats.value = chatList
                
                // Track observed friends to avoid redundant snapshot listeners
                val currentTracked = _friendStatuses.value.keys.toMutableSet()

                chatList.forEach { chat ->
                    // Mark received messages as delivered
                    if (chat.lastSenderId != currentUid && (chat.unreadCount[currentUid] ?: 0) > 0) {
                        launch {
                            chatRepository.markDelivered(chat.id, currentUid)
                        }
                    }

                    // Listen to presence of the other participant
                    val friendId = chat.participants.firstOrNull { it != currentUid }
                    if (friendId != null && !currentTracked.contains(friendId)) {
                        currentTracked.add(friendId)
                        launch {
                            userRepository.getUserFlow(friendId).collect { user ->
                                if (user != null) {
                                    _friendStatuses.value = _friendStatuses.value.toMutableMap().apply {
                                        put(friendId, user)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
