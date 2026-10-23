package com.buddy.quickchat.domain.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val fcmToken: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L
)

data class Friend(
    val uid: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val addedAt: Long = 0L
)
