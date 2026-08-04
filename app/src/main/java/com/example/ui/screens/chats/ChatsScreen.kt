package com.example.ui.screens.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.ContactEntity
import com.example.ui.components.AdBannerView
import com.example.ui.components.EmailTransportBadge
import com.example.ui.components.UserAvatar
import com.example.ui.theme.Emerald500
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatsScreen(
    chats: List<ChatEntity>,
    contacts: List<ContactEntity>,
    currentLanguage: com.example.util.AppLanguage = com.example.util.AppLanguage.ENGLISH,
    onSelectChat: (String) -> Unit,
    onOpenGeminiClick: () -> Unit,
    onCreateGroupClick: () -> Unit,
    onAddContactClick: () -> Unit,
    onOpenContactChat: ((ContactEntity) -> Unit)? = null,
    isUserOnline: (String?) -> Boolean = { false },
    isChatOnline: (ChatEntity) -> Boolean = { false },
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Unread", "Groups"

    val filteredChats = remember(chats, searchQuery, selectedFilter) {
        chats.filter { chat ->
            val matchesQuery = chat.name.contains(searchQuery, ignoreCase = true) ||
                    chat.lastMessageText.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "Unread" -> chat.unreadCount > 0
                "Groups" -> chat.isGroup
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Title Bar with Linko Logo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_linko_logo),
                        contentDescription = "Linko Logo",
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Linko",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = com.example.ui.theme.Emerald500
                )
            }

            // Header Search Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(com.example.util.LocalizationManager.getString("chats_search_placeholder", currentLanguage), fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = Emerald500,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Stories Bar / Quick Contacts & Gemini AI Row
                LazyRow(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clickable { onOpenGeminiClick() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF7C4DFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Gemini AI",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Gemini AI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C4DFF))
                        }
                    }

                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clickable { onAddContactClick() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Emerald500),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddComment,
                                    contentDescription = "Add Contact",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("New Contact", fontSize = 11.sp, maxLines = 1)
                        }
                    }

                    items(contacts) { contact ->
                        val isOnline = isUserOnline(contact.contactEmail)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clickable {
                                    onOpenContactChat?.invoke(contact)
                                }
                        ) {
                            UserAvatar(
                                name = contact.contactDisplayName,
                                avatarUri = contact.contactAvatarUri,
                                size = 48.dp,
                                showOnlineStatus = true,
                                isOnline = isOnline
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = contact.contactDisplayName.substringBefore(" "),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips Row
                Row {
                    listOf("All", "Unread", "Groups").forEach { filter ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Emerald500,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
            }

            // Google AdMob Banner Ad
            AdBannerView()

            // Chat List
            if (filteredChats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "Empty Chats",
                            tint = Emerald500,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (chats.isEmpty()) "No conversations yet" else "No matching conversations",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (chats.isEmpty()) "Scan a friend's QR code or enter their email address to start chatting!" else "Try searching for a different contact or message.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (chats.isEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onAddContactClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(Icons.Default.AddComment, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scan QR / Add Contact", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredChats, key = { it.id }) { chat ->
                        val isOnline = isChatOnline(chat)
                        ChatItemRow(
                            chat = chat,
                            isOnline = isOnline,
                            onClick = { onSelectChat(chat.id) }
                        )
                    }
                }
            }
        }

        // Floating Action Buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            horizontalAlignment = Alignment.End
        ) {
            FloatingActionButton(
                onClick = onCreateGroupClick,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .size(44.dp)
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = "Create Group")
            }

            FloatingActionButton(
                onClick = onAddContactClick,
                containerColor = Emerald500,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.AddComment, contentDescription = "New Chat")
            }
        }
    }
}

@Composable
private fun ChatItemRow(
    chat: ChatEntity,
    isOnline: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                name = chat.name,
                avatarUri = chat.groupIconUri,
                isGroup = chat.isGroup,
                size = 52.dp,
                showOnlineStatus = true,
                isOnline = isOnline
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "End-to-End Encrypted",
                        tint = Emerald500,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = formatTime(chat.lastMessageTimestamp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EmailTransportBadge(
                        emailStatus = "DELIVERED_INBOX",
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    Text(
                        text = chat.lastMessageText.ifBlank { "No messages yet" },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (chat.unreadCount > 0) {
                        Badge(
                            containerColor = Emerald500,
                            contentColor = Color.White,
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Text("${chat.unreadCount}")
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val now = System.currentTimeMillis()
    val date = Date(timestamp)

    return when {
        isSameDay(now, timestamp) -> {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
        }
        isYesterday(now, timestamp) -> {
            "Yesterday"
        }
        else -> {
            SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
        }
    }
}

private fun isSameDay(t1: Long, t2: Long): Boolean {
    val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    return fmt.format(Date(t1)) == fmt.format(Date(t2))
}

private fun isYesterday(now: Long, t: Long): Boolean {
    val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = now; add(java.util.Calendar.DAY_OF_YEAR, -1) }
    val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = t }
    return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
            cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
}
