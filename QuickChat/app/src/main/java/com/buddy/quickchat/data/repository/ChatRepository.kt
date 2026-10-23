package com.buddy.quickchat.data.repository

import com.buddy.quickchat.domain.model.Chat
import com.buddy.quickchat.domain.model.Message
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val chatsRef = firestore.collection("chats")

    fun getChats(uid: String): Flow<List<Chat>> = callbackFlow {
        val listener = chatsRef.whereArrayContains("participants", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val chats = snapshot.documents.mapNotNull { it.toObject(Chat::class.java) }
                        .sortedByDescending { it.lastMessageTime }
                    trySend(chats)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = chatsRef.document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { it.toObject(Message::class.java) }
                    trySend(messages)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(chatId: String, senderId: String, text: String, participantNames: Map<String, String>, participantPhotos: Map<String, String>) {
        val msgRef = chatsRef.document(chatId).collection("messages").document()
        val message = Message(
            id = msgRef.id,
            senderId = senderId,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        msgRef.set(message).await()

        val chatUpdates = hashMapOf<String, Any>(
            "id" to chatId,
            "participants" to chatId.split("_"),
            "participantNames" to participantNames,
            "participantPhotos" to participantPhotos,
            "lastMessage" to text,
            "lastMessageTime" to message.timestamp,
            "lastSenderId" to senderId
        )
        chatsRef.document(chatId).set(chatUpdates, com.google.firebase.firestore.SetOptions.merge()).await()
    }
}
