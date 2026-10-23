package com.buddy.quickchat.domain.model

data class Chat(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val participantPhotos: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val lastSenderId: String = "",
    val unreadCount: Map<String, Int> = emptyMap(),
    val typing: Map<String, Boolean> = emptyMap()
)
