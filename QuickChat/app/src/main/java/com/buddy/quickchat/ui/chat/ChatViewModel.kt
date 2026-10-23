package com.buddy.quickchat.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buddy.quickchat.data.repository.AuthRepository
import com.buddy.quickchat.data.repository.ChatRepository
import com.buddy.quickchat.data.repository.UserRepository
import com.buddy.quickchat.domain.model.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chatId: String = checkNotNull(savedStateHandle["chatId"])

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _currentUid = MutableStateFlow(authRepository.currentUser?.uid ?: "")
    val currentUid: StateFlow<String> = _currentUid.asStateFlow()

    init {
        loadMessages()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            chatRepository.getMessages(chatId).collect { msgList ->
                _messages.value = msgList
            }
        }
    }

    fun sendMessage(text: String, friendId: String) {
        val uid = _currentUid.value
        if (uid.isEmpty() || text.isBlank()) return
        
        viewModelScope.launch {
            // Ideally we fetch names/photos beforehand and keep in state. 
            // Simplified here.
            val me = userRepository.getUser(uid)
            val friend = userRepository.getUser(friendId)
            val names = mutableMapOf<String, String>()
            val photos = mutableMapOf<String, String>()
            
            if (me is com.buddy.quickchat.util.UiState.Success && friend is com.buddy.quickchat.util.UiState.Success) {
                names[uid] = me.data.name
                names[friendId] = friend.data.name
                photos[uid] = me.data.photoUrl
                photos[friendId] = friend.data.photoUrl
            }
            
            chatRepository.sendMessage(chatId, uid, text, names, photos)
        }
    }
}
