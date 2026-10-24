package com.buddy.quickchat.domain.model

data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val type: String = "text",
    val mediaUrl: String? = null,
    val duration: Int? = null,
    val status: String = "sending", // sending, sent, delivered, seen, failed
    val replyToId: String? = null,
    val deletedFor: List<String> = emptyList(),
    val deletedForEveryone: Boolean = false
)
