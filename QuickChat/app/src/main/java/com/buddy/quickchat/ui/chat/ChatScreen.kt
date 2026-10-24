package com.buddy.quickchat.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.buddy.quickchat.domain.model.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val currentUid by viewModel.currentUid.collectAsState()
    val friend by viewModel.friend.collectAsState()
    val isFriendTyping by viewModel.isFriendTyping.collectAsState()

    var text by remember { mutableStateOf("") }
    val friendName = friend?.name ?: "Unknown"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(friendName, style = MaterialTheme.typography.titleMedium)
                        
                        val subtitle = if (isFriendTyping) {
                            "typing..."
                        } else if (friend?.isOnline == true) {
                            "online"
                        } else if (friend != null && friend!!.lastSeen > 0) {
                            "last seen ${formatLastSeen(friend!!.lastSeen)}"
                        } else {
                            ""
                        }
                        
                        if (subtitle.isNotEmpty()) {
                            Text(
                                text = subtitle, 
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentPadding = PaddingValues(8.dp)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { 
                        text = it 
                        viewModel.onTyping()
                    },
                    modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp)),
                    placeholder = { Text("Message") },
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = {
                        viewModel.sendMessage(text)
                        text = ""
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Filled.Send, "Send")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            reverseLayout = false
        ) {
            items(messages) { msg ->
                MessageBubble(message = msg, isMe = msg.senderId == currentUid)
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (isFriendTyping) {
                item {
                    TypingIndicatorBubble()
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message, isMe: Boolean) {
    val dateFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val timeString = dateFormat.format(Date(message.timestamp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .background(
                    color = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    )
                )
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.bodyLarge
            )
            
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                )
                if (isMe) {
                    Spacer(modifier = Modifier.width(4.dp))
                    val icon = when (message.status) {
                        "sending" -> Icons.Filled.Schedule
                        "sent" -> Icons.Filled.Check
                        "delivered" -> Icons.Filled.DoneAll
                        "seen" -> Icons.Filled.DoneAll
                        "failed" -> Icons.Filled.Error
                        else -> Icons.Filled.Schedule
                    }
                    val tint = when (message.status) {
                        "seen" -> Color(0xFF34B7F1) // Blue tick
                        "failed" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = message.status,
                        modifier = Modifier.size(16.dp),
                        tint = tint
                    )
                }
            }
        }
    }
}

@Composable
fun TypingIndicatorBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
                )
                .padding(12.dp)
        ) {
            Text(text = "...", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

fun formatLastSeen(timestamp: Long): String {
    val cal = Calendar.getInstance()
    cal.timeInMillis = timestamp
    
    val now = Calendar.getInstance()
    
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val timeStr = timeFormat.format(Date(timestamp))
    
    return if (cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) && cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
        "today at $timeStr"
    } else if (cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) - cal.get(Calendar.DAY_OF_YEAR) == 1) {
        "yesterday"
    } else {
        SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}
