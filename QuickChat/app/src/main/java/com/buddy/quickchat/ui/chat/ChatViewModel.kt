package com.buddy.quickchat.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buddy.quickchat.data.repository.AuthRepository
import com.buddy.quickchat.data.repository.ChatRepository
import com.buddy.quickchat.data.repository.UserRepository
import com.buddy.quickchat.domain.model.Message
import com.buddy.quickchat.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private val friendId = chatId.split("_").firstOrNull { it != _currentUid.value } ?: ""

    private val _friend = MutableStateFlow<User?>(null)
    val friend: StateFlow<User?> = _friend.asStateFlow()

    private val _isFriendTyping = MutableStateFlow(false)
    val isFriendTyping: StateFlow<Boolean> = _isFriendTyping.asStateFlow()

    private var typingJob: Job? = null

    init {
        loadMessages()
        loadFriendPresence()
        loadChatDetails()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            chatRepository.getMessages(chatId).collect { msgList ->
                _messages.value = msgList

                // Mark received messages as seen
                val uid = _currentUid.value
                val unseenIds = msgList.filter { it.senderId != uid && it.status != "seen" }.map { it.id }
                if (unseenIds.isNotEmpty()) {
                    chatRepository.updateMessageStatus(chatId, unseenIds, "seen")
                }
            }
        }
    }

    private fun loadFriendPresence() {
        if (friendId.isEmpty()) return
        viewModelScope.launch {
            userRepository.getUserFlow(friendId).collect { user ->
                _friend.value = user
            }
        }
    }

    private fun loadChatDetails() {
        viewModelScope.launch {
            chatRepository.getChat(chatId).collect { chat ->
                if (chat != null) {
                    val uid = _currentUid.value
                    if (chat.unreadCount[uid] ?: 0 > 0) {
                        chatRepository.resetUnreadCount(chatId, uid)
                    }
                    _isFriendTyping.value = chat.typing[friendId] == true
                }
            }
        }
    }

    fun onTyping() {
        val uid = _currentUid.value
        if (uid.isEmpty()) return

        typingJob?.cancel()
        viewModelScope.launch {
            chatRepository.setTypingStatus(chatId, uid, true)
        }
        typingJob = viewModelScope.launch {
            delay(3000)
            chatRepository.setTypingStatus(chatId, uid, false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        val uid = _currentUid.value
        if (uid.isNotEmpty()) {
            viewModelScope.launch {
                chatRepository.setTypingStatus(chatId, uid, false)
            }
        }
    }

    fun sendMessage(text: String) {
        val uid = _currentUid.value
        if (uid.isEmpty() || text.isBlank()) return
        
        typingJob?.cancel()
        viewModelScope.launch {
            chatRepository.setTypingStatus(chatId, uid, false)
        }
        
        viewModelScope.launch {
            val me = userRepository.getUser(uid)
            val friendRes = userRepository.getUser(friendId)
            val names = mutableMapOf<String, String>()
            val photos = mutableMapOf<String, String>()
            
            if (me is com.buddy.quickchat.util.UiState.Success && friendRes is com.buddy.quickchat.util.UiState.Success) {
                names[uid] = me.data.name
                names[friendId] = friendRes.data.name
                photos[uid] = me.data.photoUrl
                photos[friendId] = friendRes.data.photoUrl
            }
            
            chatRepository.sendMessage(chatId, uid, text, names, photos, friendId)
        }
    }
}
