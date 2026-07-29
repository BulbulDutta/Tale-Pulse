package com.example.ui.screens.chats

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.CallType
import com.example.data.model.EmailHeaderInfo
import com.example.ui.components.AiImageDialog
import com.example.ui.components.AttachmentBottomSheet
import com.example.ui.components.AttachmentType
import com.example.ui.components.ChatBubble
import com.example.ui.components.ContactPickerDialog
import com.example.ui.components.EmailHeaderDialog
import com.example.ui.components.EncryptionSecurityDialog
import com.example.ui.components.EventCreationDialog
import com.example.ui.components.LocationPickerDialog
import com.example.ui.components.PollCreationDialog
import com.example.ui.components.UserAvatar
import com.example.ui.theme.ChatWallpaper
import com.example.ui.theme.ChatWallpaperBackground
import com.example.ui.theme.Emerald500
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectChatScreen(
    chat: ChatEntity?,
    messages: List<MessageEntity>,
    currentUser: UserEntity?,
    contacts: List<ContactEntity> = emptyList(),
    chatWallpaper: ChatWallpaper = ChatWallpaper.DOODLE,
    customWallpaperUri: String? = null,
    customWallpaperDimming: Float = 0.3f,
    customWallpaperScale: String = "CROP",
    isDarkMode: Boolean = true,
    onBackClick: () -> Unit,
    onSendMessage: (text: String, mediaUri: String?, mediaType: String?, richFormat: String?) -> Unit,
    onVotePoll: (messageId: String, optionIndex: Int) -> Unit = { _, _ -> },
    onSendPoll: (question: String, options: List<String>, allowMultiple: Boolean) -> Unit = { _, _, _ -> },
    onSendLocation: (title: String, address: String, lat: Double, lng: Double) -> Unit = { _, _, _, _ -> },
    onSendContact: (name: String, email: String, phone: String) -> Unit = { _, _, _ -> },
    onSendEvent: (title: String, dateText: String, locationText: String) -> Unit = { _, _, _ -> },
    onStartCall: (contactName: String, contactEmail: String, contactAvatar: String?, callType: CallType) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var selectedRichStyle by remember { mutableStateOf<String?>(null) } // "BOLD", "ITALIC", "CODE"
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var activeEmailHeader by remember { mutableStateOf<EmailHeaderInfo?>(null) }
    var showSecurityDialog by remember { mutableStateOf(false) }

    var showPollDialog by remember { mutableStateOf(false) }
    var showEventDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showAiImageDialog by remember { mutableStateOf(false) }

    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }

    val sheetState = rememberModalBottomSheetState()

    val isGeminiChat = chat?.id == "chat_gemini_ai" || chat?.name?.contains("Gemini") == true

    // Activity Result Launchers
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            onSendMessage("Photo Attachment", uri.toString(), "IMAGE", null)
        }
    }

    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Document.pdf"
            onSendMessage(fileName, uri.toString(), "DOCUMENT", null)
        }
    }

    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            onSendMessage("Audio Recording Note", uri.toString(), "AUDIO", null)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        val samplePhotoUrl = "https://picsum.photos/seed/${(100..999).random()}/500/400"
        onSendMessage("Instant Camera Capture", samplePhotoUrl, "IMAGE", null)
    }

    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            recordingSeconds = 0
            while (isRecordingAudio) {
                delay(1000)
                recordingSeconds++
            }
        }
    }

    val listState = rememberLazyListState()

    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    LaunchedEffect(imeBottom) {
        if (imeBottom > 0.dp && messages.isNotEmpty()) {
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (activeEmailHeader != null) {
        EmailHeaderDialog(
            info = activeEmailHeader!!,
            onDismiss = { activeEmailHeader = null }
        )
    }

    if (showSecurityDialog) {
        EncryptionSecurityDialog(
            chatId = chat?.id ?: "chat_default",
            chatName = chat?.name ?: "Contact",
            onDismiss = { showSecurityDialog = false }
        )
    }

    if (showAttachmentMenu) {
        AttachmentBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { showAttachmentMenu = false },
            onOptionSelected = { type ->
                when (type) {
                    AttachmentType.GALLERY -> galleryLauncher.launch("image/*")
                    AttachmentType.CAMERA -> cameraLauncher.launch(null)
                    AttachmentType.DOCUMENT -> documentLauncher.launch("*/*")
                    AttachmentType.AUDIO -> audioLauncher.launch("audio/*")
                    AttachmentType.LOCATION -> showLocationDialog = true
                    AttachmentType.CONTACT -> showContactDialog = true
                    AttachmentType.POLL -> showPollDialog = true
                    AttachmentType.EVENT -> showEventDialog = true
                    AttachmentType.AI_IMAGE -> showAiImageDialog = true
                }
            }
        )
    }

    if (showPollDialog) {
        PollCreationDialog(
            onDismiss = { showPollDialog = false },
            onCreatePoll = { question, options, allowMultiple ->
                onSendPoll(question, options, allowMultiple)
            }
        )
    }

    if (showEventDialog) {
        EventCreationDialog(
            onDismiss = { showEventDialog = false },
            onCreateEvent = { title, dateText, locationText ->
                onSendEvent(title, dateText, locationText)
            }
        )
    }

    if (showContactDialog) {
        ContactPickerDialog(
            contacts = contacts,
            onDismiss = { showContactDialog = false },
            onContactSelected = { name, email, phone ->
                onSendContact(name, email, phone)
            }
        )
    }

    if (showLocationDialog) {
        LocationPickerDialog(
            onDismiss = { showLocationDialog = false },
            onSendLocation = { title, address, lat, lng ->
                onSendLocation(title, address, lat, lng)
            }
        )
    }

    if (showAiImageDialog) {
        AiImageDialog(
            onDismiss = { showAiImageDialog = false },
            onSendAiImage = { prompt, imageUrl ->
                onSendMessage(prompt, imageUrl, "IMAGE", null)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isGeminiChat) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF7C4DFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "Gemini", tint = Color.White)
                            }
                        } else {
                            UserAvatar(
                                name = chat?.name ?: "Chat",
                                isGroup = chat?.isGroup ?: false,
                                size = 38.dp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = chat?.name ?: "Chat",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isGeminiChat) "Gemini 3.5 Flash AI • Active" else if (chat?.isGroup == true) "Group Chat • Email Active" else "Online • Email Mirrored",
                                fontSize = 11.sp,
                                color = if (isGeminiChat) Color(0xFF7C4DFF) else Emerald500
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (!isGeminiChat) {
                        IconButton(onClick = {
                            onStartCall(
                                chat?.name ?: "Contact",
                                "${chat?.name?.lowercase()?.replace(" ", ".")}@talepulse.com",
                                null,
                                CallType.AUDIO
                            )
                        }) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = "Audio Call", tint = Emerald500)
                        }
                        IconButton(onClick = {
                            onStartCall(
                                chat?.name ?: "Contact",
                                "${chat?.name?.lowercase()?.replace(" ", ".")}@talepulse.com",
                                null,
                                CallType.VIDEO
                            )
                        }) {
                            Icon(imageVector = Icons.Default.Videocam, contentDescription = "Video Call", tint = Emerald500)
                        }
                    }
                    IconButton(onClick = { showSecurityDialog = true }) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "E2EE Security", tint = Emerald500)
                    }
                    IconButton(onClick = {
                        activeEmailHeader = EmailHeaderInfo(
                            from = currentUser?.email ?: "alex.rivera@talepulse.com",
                            to = if (isGeminiChat) "gemini.ai@talepulse.com" else "${chat?.name?.lowercase()?.replace(" ", ".")}@talepulse.com",
                            subject = "TalePulse Direct Sync [${chat?.name}]",
                            messageId = "<chat-session-${chat?.id}@talepulse.net>",
                            smtpRoute = "smtp.talepulse.net (TLS 1.3)",
                            timestampFormatted = "Just now",
                            status = "ACTIVE_EMAIL_MIRROR"
                        )
                    }) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Email Info")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            // Chat Input Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                // Gemini AI Prompt Suggestions Row
                if (isGeminiChat) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            AssistChip(
                                onClick = {
                                    onSendMessage("✨ Summarize our recent chat history into key bullet points.", null, null, null)
                                },
                                label = { Text("✨ Summarize chat", fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF7C4DFF).copy(alpha = 0.15f))
                            )
                        }
                        item {
                            AssistChip(
                                onClick = {
                                    onSendMessage("💡 Draft a polite follow-up email message regarding project updates.", null, null, null)
                                },
                                label = { Text("💡 Draft email", fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF7C4DFF).copy(alpha = 0.15f))
                            )
                        }
                        item {
                            AssistChip(
                                onClick = {
                                    onSendMessage("🖼️ Analyze this image and describe its key elements in detail.", "https://picsum.photos/400/300", "IMAGE", null)
                                },
                                label = { Text("🖼️ Analyze photo", fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF7C4DFF).copy(alpha = 0.15f))
                            )
                        }
                        item {
                            AssistChip(
                                onClick = {
                                    onSendMessage("📝 Give me 3 creative story ideas for a modern messaging app user.", null, null, null)
                                },
                                label = { Text("📝 Story ideas", fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF7C4DFF).copy(alpha = 0.15f))
                            )
                        }
                    }
                }

                // Rich Text Toolbar Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Formatting:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { selectedRichStyle = if (selectedRichStyle == "BOLD") null else "BOLD" },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatBold,
                            contentDescription = "Bold",
                            tint = if (selectedRichStyle == "BOLD") Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { selectedRichStyle = if (selectedRichStyle == "ITALIC") null else "ITALIC" },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatItalic,
                            contentDescription = "Italic",
                            tint = if (selectedRichStyle == "ITALIC") Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { selectedRichStyle = if (selectedRichStyle == "CODE") null else "CODE" },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Code",
                            tint = if (selectedRichStyle == "CODE") Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isRecordingAudio) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Recording",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val durationText = formatSeconds(recordingSeconds)
                                Text(
                                    text = "Recording Voice Note... $durationText",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Row {
                                IconButton(onClick = { isRecordingAudio = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error)
                                }
                                IconButton(
                                    onClick = {
                                        isRecordingAudio = false
                                        val durationText = formatSeconds(recordingSeconds)
                                        onSendMessage("🎙️ Voice Note ($durationText)", null, "AUDIO", null)
                                        recordingSeconds = 0
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = Emerald500)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Voice Note", tint = Color.White)
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showAttachmentMenu = true },
                            modifier = Modifier.testTag("attachment_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Plus Attachment Menu",
                                tint = Emerald500,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        IconButton(onClick = { showAttachmentMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Attach File",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text(if (isGeminiChat) "Ask Gemini anything or type..." else "Type a message or email...", fontSize = 14.sp) },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedBorderColor = if (isGeminiChat) Color(0xFF7C4DFF) else Emerald500,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (inputText.isNotBlank()) {
                                        onSendMessage(inputText, null, null, selectedRichStyle)
                                        inputText = ""
                                        selectedRichStyle = null
                                    }
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                        )

                        if (inputText.isBlank()) {
                            IconButton(
                                onClick = { isRecordingAudio = true },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Record",
                                    tint = Emerald500
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    onSendMessage(inputText, null, null, selectedRichStyle)
                                    inputText = ""
                                    selectedRichStyle = null
                                },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = if (isGeminiChat) Color(0xFF7C4DFF) else Emerald500),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        ChatWallpaperBackground(
            wallpaper = chatWallpaper,
            isDarkMode = isDarkMode,
            customWallpaperUri = customWallpaperUri,
            customWallpaperDimming = customWallpaperDimming,
            customWallpaperScale = customWallpaperScale,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isFromMe = currentUser?.id == msg.senderId || currentUser?.email == msg.senderEmail
                    MessageBubble(
                        message = msg,
                        isFromMe = isFromMe,
                        currentUserId = currentUser?.id,
                        onVotePoll = { optionIndex ->
                            onVotePoll(msg.id, optionIndex)
                        },
                        onEmailBadgeClick = {
                            activeEmailHeader = EmailHeaderInfo(
                                from = msg.senderEmail,
                                to = currentUser?.email ?: "me@talepulse.com",
                                subject = msg.emailSubject ?: "TalePulse Message",
                                messageId = msg.emailMessageId ?: "<msg@talepulse.net>",
                                timestampFormatted = formatTime(msg.timestamp),
                                status = msg.emailTransportStatus
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: MessageEntity,
    isFromMe: Boolean,
    currentUserId: String?,
    onVotePoll: (optionIndex: Int) -> Unit,
    onEmailBadgeClick: () -> Unit
) {
    ChatBubble(
        text = message.text,
        senderName = message.senderName,
        isFromMe = isFromMe,
        timestampText = formatTime(message.timestamp),
        status = message.status,
        emailTransportStatus = message.emailTransportStatus,
        mediaUri = message.mediaUri,
        mediaType = message.mediaType,
        formattedRichText = message.formattedRichText,
        isEncrypted = message.isEncrypted,
        currentUserId = currentUserId,
        onVotePoll = onVotePoll,
        onEmailBadgeClick = onEmailBadgeClick
    )
}

private fun formatSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", m, s)
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
