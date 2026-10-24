package com.buddy.quickchat.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.buddy.quickchat.domain.model.Chat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onNavigateToChat: (String) -> Unit,
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val chats by viewModel.chats.collectAsState()
    val friendStatuses by viewModel.friendStatuses.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Chats") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            items(chats) { chat ->
                val friendId = chat.participants.firstOrNull { it != viewModel.currentUid } ?: ""
                val isOnline = friendStatuses[friendId]?.isOnline == true
                
                ChatItem(
                    chat = chat,
                    currentUid = viewModel.currentUid,
                    isOnline = isOnline,
                    onClick = { onNavigateToChat(chat.id) }
                )
            }
        }
    }
}

@Composable
fun ChatItem(chat: Chat, currentUid: String, isOnline: Boolean, onClick: () -> Unit) {
    val otherName = chat.participantNames.values.firstOrNull { it.isNotBlank() } ?: "Unknown"
    val otherPhoto = chat.participantPhotos.values.firstOrNull { it.isNotBlank() } ?: ""

    val dateFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val timeString = if (chat.lastMessageTime > 0) dateFormat.format(Date(chat.lastMessageTime)) else ""
    val unreadCount = chat.unreadCount[currentUid] ?: 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AsyncImage(
                model = otherPhoto.ifEmpty { "https://ui-avatars.com/api/?name=$otherName" },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
            if (isOnline) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color.Green)
                        .align(Alignment.BottomEnd)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = otherName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            
            // If typing, show "typing..." instead of last message? The instruction says in top bar and message list, not specifically chat list, but good to have if we want.
            // Let's stick to last message for now.
            Text(
                text = chat.lastMessage,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = if (unreadCount > 0) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = timeString, style = MaterialTheme.typography.labelSmall)
            if (unreadCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(text = unreadCount.toString())
                }
            }
        }
    }
}
