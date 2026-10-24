package com.buddy.quickchat.data.repository

import com.buddy.quickchat.domain.model.Chat
import com.buddy.quickchat.domain.model.Message
import com.google.firebase.firestore.FieldValue
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

    fun getChat(chatId: String): Flow<Chat?> = callbackFlow {
        val listener = chatsRef.document(chatId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.toObject(Chat::class.java))
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

    suspend fun sendMessage(
        chatId: String, 
        senderId: String, 
        text: String, 
        participantNames: Map<String, String>, 
        participantPhotos: Map<String, String>,
        receiverId: String
    ) {
        val msgRef = chatsRef.document(chatId).collection("messages").document()
        val message = Message(
            id = msgRef.id,
            senderId = senderId,
            text = text,
            timestamp = System.currentTimeMillis(),
            status = "sent"
        )
        msgRef.set(message).await()

        val chatUpdates = hashMapOf<String, Any>(
            "id" to chatId,
            "participants" to chatId.split("_"),
            "participantNames" to participantNames,
            "participantPhotos" to participantPhotos,
            "lastMessage" to text,
            "lastMessageTime" to message.timestamp,
            "lastSenderId" to senderId,
            "unreadCount.$receiverId" to FieldValue.increment(1)
        )
        chatsRef.document(chatId).set(chatUpdates, com.google.firebase.firestore.SetOptions.merge()).await()
    }
    
    suspend fun updateMessageStatus(chatId: String, messageIds: List<String>, newStatus: String) {
        if (messageIds.isEmpty()) return
        // Firestore batch allows up to 500 writes
        messageIds.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { msgId ->
                val ref = chatsRef.document(chatId).collection("messages").document(msgId)
                batch.update(ref, "status", newStatus)
            }
            batch.commit().await()
        }
    }
    
    suspend fun setTypingStatus(chatId: String, uid: String, isTyping: Boolean) {
        chatsRef.document(chatId).update("typing.$uid", isTyping).await()
    }
    
    suspend fun resetUnreadCount(chatId: String, uid: String) {
        chatsRef.document(chatId).update("unreadCount.$uid", 0).await()
    }
    
    suspend fun markDelivered(chatId: String, uid: String) {
        // Find messages where senderId != uid and status == "sent"
        val querySnapshot = chatsRef.document(chatId).collection("messages")
            .whereNotEqualTo("senderId", uid)
            .get()
            .await()
        
        val sentIds = querySnapshot.documents.mapNotNull { doc ->
            val msg = doc.toObject(Message::class.java)
            if (msg?.status == "sent") doc.id else null
        }
        
        updateMessageStatus(chatId, sentIds, "delivered")
    }
}
