package com.example.ui.screens.chats

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.components.CustomEmojiPickerDialog
import com.example.ui.components.EmailHeaderDialog
import com.example.ui.components.EmojiPickerPanel
import com.example.ui.components.EncryptionSecurityDialog
import com.example.ui.components.EventCreationDialog
import com.example.ui.components.ImagePreviewDialog
import com.example.ui.components.LocationPickerDialog
import com.example.ui.components.MessageReactionsBar
import com.example.ui.components.PollCreationDialog
import com.example.ui.components.RecipientProfileSheet
import com.example.ui.components.UserAvatar
import com.example.util.ImageStorageHelper
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
    isUserOnline: (String?) -> Boolean = { false },
    chatWallpaper: ChatWallpaper = ChatWallpaper.DOODLE,
    customWallpaperUri: String? = null,
    customWallpaperDimming: Float = 0.3f,
    customWallpaperScale: String = "CROP",
    isDarkMode: Boolean = true,
    currentLanguage: com.example.util.AppLanguage = com.example.util.AppLanguage.ENGLISH,
    translatedMessages: Map<String, String> = emptyMap(),
    onBackClick: () -> Unit,
    onSendMessage: (text: String, mediaUri: String?, mediaType: String?, richFormat: String?) -> Unit,
    onMarkAsRead: (chatId: String) -> Unit = {},
    onToggleReaction: (messageId: String, emoji: String) -> Unit = { _, _ -> },
    onDeleteForMe: (messageId: String) -> Unit = {},
    onDeleteForEveryone: (messageId: String) -> Unit = {},
    onVotePoll: (messageId: String, optionIndex: Int) -> Unit = { _, _ -> },
    onSendPoll: (question: String, options: List<String>, allowMultiple: Boolean) -> Unit = { _, _, _ -> },
    onSendLocation: (title: String, address: String, lat: Double, lng: Double) -> Unit = { _, _, _, _ -> },
    onSendContact: (name: String, email: String, phone: String) -> Unit = { _, _, _ -> },
    onSendEvent: (title: String, dateText: String, locationText: String) -> Unit = { _, _, _ -> },
    onStartCall: (contactName: String, contactEmail: String, contactAvatar: String?, callType: CallType) -> Unit,
    onClearChatHistory: () -> Unit = {},
    onDeleteChat: () -> Unit = {},
    onAddGroupMembers: (chatId: String, selectedContacts: List<ContactEntity>) -> Unit = { _, _ -> },
    onToggleAdminRole: (chatId: String, memberUserId: String, makeAdmin: Boolean) -> Unit = { _, _, _ -> },
    onRemoveMember: (chatId: String, memberUserId: String) -> Unit = { _, _ -> },
    onSendPrivateMessage: (contactUserId: String, contactEmail: String, contactName: String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var selectedRichStyle by remember { mutableStateOf<String?>(null) } // "BOLD", "ITALIC", "CODE"
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var activeEmailHeader by remember { mutableStateOf<EmailHeaderInfo?>(null) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }

    val recipientContact = remember(chat, contacts) {
        if (chat == null) null
        else contacts.find { "chat_${it.contactUserId}" == chat.id || it.contactDisplayName.equals(chat.name, ignoreCase = true) }
    }

    val isGeminiChat = chat?.id == "chat_gemini_ai" || chat?.name?.contains("Gemini") == true
    val isAiBot = isGeminiChat

    val isChatUserOnline = remember(chat, recipientContact, isUserOnline) {
        if (chat == null) false
        else if (isGeminiChat) true
        else if (chat.isGroup) false
        else if (recipientContact != null) isUserOnline(recipientContact.contactEmail)
        else isUserOnline(chat.name)
    }

    var showPollDialog by remember { mutableStateOf(false) }
    var showEventDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showAiImageDialog by remember { mutableStateOf(false) }
    var pendingSelectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var showEmojiPickerPanel by remember { mutableStateOf(false) }
    var reactionTargetMessageId by remember { mutableStateOf<String?>(null) }
    var customEmojiTargetMessageId by remember { mutableStateOf<String?>(null) }

    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }
    var actionTargetMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var replyingToMessage by remember { mutableStateOf<MessageEntity?>(null) }

    val sheetState = rememberModalBottomSheetState()

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val audioRecorderManager = remember { com.example.util.AudioRecorderManager(context) }

    // Activity Result Launchers
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val savedUri = ImageStorageHelper.saveUriToInternalStorage(context, uri)
                pendingSelectedImageUri = savedUri ?: uri
            }
        }
    }

    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val savedUri = ImageStorageHelper.saveUriToInternalStorage(context, uri)
                val finalUriStr = savedUri?.toString() ?: uri.toString()
                val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Document.pdf"
                onSendMessage(fileName, finalUriStr, "DOCUMENT", null)
            }
        }
    }

    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val savedUri = ImageStorageHelper.saveUriToInternalStorage(context, uri)
                val finalUriStr = savedUri?.toString() ?: uri.toString()
                onSendMessage("Audio Recording Note", finalUriStr, "AUDIO", null)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            scope.launch {
                val savedUri = ImageStorageHelper.saveBitmapToInternalStorage(context, bitmap)
                if (savedUri != null) {
                    pendingSelectedImageUri = savedUri
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Camera permission is required to capture photos", Toast.LENGTH_SHORT).show()
        }
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

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, chat?.id) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                chat?.id?.let { id -> onMarkAsRead(id) }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(chat?.id, messages) {
        chat?.id?.let { id ->
            val hasUnreadFromOthers = messages.any { it.senderId != currentUser?.id && it.status != "READ" }
            if (hasUnreadFromOthers) {
                onMarkAsRead(id)
            }
        }
    }

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
                    AttachmentType.CAMERA -> {
                        val hasCameraPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasCameraPermission) {
                            cameraLauncher.launch(null)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                    AttachmentType.DOCUMENT -> documentLauncher.launch("*/*")
                    AttachmentType.AUDIO -> audioLauncher.launch("audio/*")
                    AttachmentType.LOCATION -> showLocationDialog = true
                    AttachmentType.CONTACT -> showContactDialog = true
                    AttachmentType.POLL -> showPollDialog = true
                    AttachmentType.EVENT -> showEventDialog = true
                    AttachmentType.AI_IMAGE -> showAiImageDialog = true
                    AttachmentType.IN_APP_CALL -> {
                        val peerEmail = "${(chat?.name ?: "contact").lowercase().replace(" ", ".")}@linko.net"
                        onStartCall(
                            chat?.name ?: "Contact",
                            peerEmail,
                            chat?.groupIconUri,
                            CallType.VIDEO
                        )
                    }
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

    if (pendingSelectedImageUri != null) {
        ImagePreviewDialog(
            imageUri = pendingSelectedImageUri!!,
            onDismiss = { pendingSelectedImageUri = null },
            onSendImage = { caption, uploadedUrl ->
                onSendMessage(caption, uploadedUrl, "IMAGE", null)
                pendingSelectedImageUri = null
            }
        )
    }

    if (customEmojiTargetMessageId != null) {
        val targetMsgId = customEmojiTargetMessageId!!
        CustomEmojiPickerDialog(
            onEmojiSelect = { emoji ->
                onToggleReaction(targetMsgId, emoji)
                customEmojiTargetMessageId = null
            },
            onDismiss = { customEmojiTargetMessageId = null }
        )
    }

    if (showProfileSheet) {
        RecipientProfileSheet(
            chat = chat,
            messages = messages,
            recipientContact = recipientContact,
            currentUser = currentUser,
            allContacts = contacts,
            isOnline = isChatUserOnline,
            onDismissRequest = { showProfileSheet = false },
            onStartCall = { isVideo ->
                showProfileSheet = false
                val peerEmail = recipientContact?.contactEmail ?: "${(chat?.name ?: "contact").lowercase().replace(" ", ".")}@linko.net"
                onStartCall(
                    recipientContact?.contactDisplayName ?: chat?.name ?: "Contact",
                    peerEmail,
                    recipientContact?.contactAvatarUri ?: chat?.groupIconUri,
                    if (isVideo) CallType.VIDEO else CallType.AUDIO
                )
            },
            onClearChatHistory = {
                onClearChatHistory()
                Toast.makeText(context, "Chat history cleared", Toast.LENGTH_SHORT).show()
            },
            onDeleteChat = {
                onDeleteChat()
                Toast.makeText(context, "Chat deleted", Toast.LENGTH_SHORT).show()
            },
            onAddGroupMembers = onAddGroupMembers,
            onToggleAdminRole = onToggleAdminRole,
            onRemoveMember = onRemoveMember,
            onSendPrivateMessage = onSendPrivateMessage
        )
    }

    ChatWallpaperBackground(
        wallpaper = chatWallpaper,
        isDarkMode = isDarkMode,
        customWallpaperUri = customWallpaperUri,
        customWallpaperDimming = customWallpaperDimming,
        customWallpaperScale = customWallpaperScale,
        modifier = modifier.fillMaxSize()
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showProfileSheet = true }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        ) {
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
                                    name = recipientContact?.contactDisplayName ?: chat?.name ?: "Chat",
                                    avatarUri = recipientContact?.contactAvatarUri ?: chat?.groupIconUri,
                                    isGroup = chat?.isGroup ?: false,
                                    size = 38.dp,
                                    showOnlineStatus = true,
                                    isOnline = isChatUserOnline
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = recipientContact?.contactDisplayName ?: chat?.name ?: "Chat",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "E2EE",
                                        tint = if (isGeminiChat) Color(0xFF7C4DFF) else if (isChatUserOnline) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isGeminiChat) "Gemini AI • E2EE" else if (chat?.isGroup == true) "Group • E2EE" else if (isChatUserOnline) "Online • E2EE" else "Offline • E2EE",
                                        fontSize = 11.sp,
                                        color = if (isGeminiChat) Color(0xFF7C4DFF) else if (isChatUserOnline) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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
                        if (!isAiBot) {
                            val peerEmail = "${(chat?.name ?: "contact").lowercase().replace(" ", ".")}@linko.net"
                            IconButton(onClick = {
                                onStartCall(
                                    chat?.name ?: "Contact",
                                    peerEmail,
                                    chat?.groupIconUri,
                                    CallType.AUDIO
                                )
                            }) {
                                Icon(imageVector = Icons.Default.Call, contentDescription = "Voice Call", tint = Emerald500)
                            }
                            IconButton(onClick = {
                                onStartCall(
                                    chat?.name ?: "Contact",
                                    peerEmail,
                                    chat?.groupIconUri,
                                    CallType.VIDEO
                                )
                            }) {
                                Icon(imageVector = Icons.Default.Videocam, contentDescription = "Video Call", tint = Emerald500)
                            }
                        }
                        IconButton(onClick = { showProfileSheet = true }) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = "Contact Info & Shared Media", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                )
            },
            bottomBar = {
                // Chat Input Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    // Quoted Reply Preview Bar
                    if (replyingToMessage != null) {
                        val replyMsg = replyingToMessage!!
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(34.dp)
                                        .background(Emerald500, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Replying to ${replyMsg.senderName}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Emerald500
                                    )
                                    val snippet = if (replyMsg.text.isNotBlank()) replyMsg.text
                                    else if (replyMsg.mediaType?.uppercase() == "IMAGE") "📷 Photo"
                                    else if (replyMsg.mediaType?.uppercase() == "AUDIO") "🎙️ Voice Note"
                                    else if (replyMsg.mediaType?.uppercase() == "DOCUMENT") "📄 Document"
                                    else "Media"
                                    Text(
                                        text = snippet,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { replyingToMessage = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel Reply", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

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
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSendMessage("✨ Summarize our recent chat history into key bullet points.", null, null, null)
                                    },
                                    label = { Text("✨ Summarize chat", fontSize = 11.sp) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF7C4DFF).copy(alpha = 0.15f))
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSendMessage("💡 Draft a polite follow-up email message regarding project updates.", null, null, null)
                                    },
                                    label = { Text("💡 Draft email", fontSize = 11.sp) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF7C4DFF).copy(alpha = 0.15f))
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSendMessage("🖼️ Analyze this image and describe its key elements in detail.", "https://picsum.photos/400/300", "IMAGE", null)
                                    },
                                    label = { Text("🖼️ Analyze photo", fontSize = 11.sp) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF7C4DFF).copy(alpha = 0.15f))
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSendMessage("📝 Give me 3 creative story ideas for a modern messaging app user.", null, null, null)
                                    },
                                    label = { Text("📝 Story ideas", fontSize = 11.sp) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF7C4DFF).copy(alpha = 0.15f))
                                )
                            }
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
                                    IconButton(
                                        onClick = {
                                            audioRecorderManager.stopRecording()
                                            isRecordingAudio = false
                                            recordingSeconds = 0
                                        }
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error)
                                    }
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val recordedFile = audioRecorderManager.stopRecording()
                                            isRecordingAudio = false
                                            val durationText = formatSeconds(recordingSeconds)
                                            if (recordedFile != null) {
                                                scope.launch {
                                                    val savedUri = ImageStorageHelper.saveUriToInternalStorage(context, Uri.fromFile(recordedFile))
                                                    val audioUri = savedUri?.toString() ?: Uri.fromFile(recordedFile).toString()
                                                    onSendMessage("🎙️ Voice Note ($durationText)", audioUri, "AUDIO", null)
                                                }
                                            }
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
                        // Ultra-slim single-row WhatsApp/Instagram style chat input bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                                shadowElevation = 1.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 0.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left icon cluster: Emoji, Plus ('+')
                                    IconButton(
                                        onClick = { showEmojiPickerPanel = !showEmojiPickerPanel },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("emoji_picker_toggle")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Face,
                                            contentDescription = "Emoji Picker",
                                            tint = if (showEmojiPickerPanel) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { showAttachmentMenu = true },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("attachment_menu_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add Content",
                                            tint = Emerald500,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    // Simple text input field ("Message")
                                    TextField(
                                        value = inputText,
                                        onValueChange = { inputText = it },
                                        placeholder = {
                                            Text(
                                                text = if (isGeminiChat) "Message Gemini..." else com.example.util.LocalizationManager.getString("type_message", currentLanguage),
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        },
                                        singleLine = false,
                                        maxLines = 4,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            disabledContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            disabledIndicatorColor = Color.Transparent,
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        keyboardOptions = KeyboardOptions(
                                            capitalization = KeyboardCapitalization.Sentences,
                                            imeAction = ImeAction.Default
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 2.dp)
                                    )

                                    // Voice / Microphone icon tightly integrated inside the bar
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            audioRecorderManager.startRecording()
                                            isRecordingAudio = true
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = "Voice Record",
                                            tint = if (inputText.isBlank()) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // Immediate Send action button when text is entered
                            if (inputText.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val replyPrefix = if (replyingToMessage != null) {
                                            val rMsg = replyingToMessage!!
                                            val textSnippet = rMsg.text.ifBlank { rMsg.mediaType ?: "Media" }
                                            "REPLY:${rMsg.senderName}|$textSnippet"
                                        } else null
                                        val finalStyle = if (replyPrefix != null) {
                                            if (selectedRichStyle != null) "$replyPrefix;$selectedRichStyle" else replyPrefix
                                        } else selectedRichStyle

                                        onSendMessage(inputText.trim(), null, null, finalStyle)
                                        inputText = ""
                                        selectedRichStyle = null
                                        replyingToMessage = null
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = if (isGeminiChat) Color(0xFF7C4DFF) else Emerald500
                                    ),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (showEmojiPickerPanel) {
                        EmojiPickerPanel(
                            onEmojiSelect = { emoji ->
                                inputText += emoji
                            },
                            onBackspace = {
                                if (inputText.isNotEmpty()) {
                                    inputText = inputText.dropLast(1)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .padding(top = 6.dp)
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                item(key = "e2ee_security_banner") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                            .clickable { showSecurityDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        color = Emerald500.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Encrypted",
                                tint = Emerald500,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Messages and calls are end-to-end encrypted with Signal Protocol AES-256-GCM. No one outside of this chat can read or listen. Tap to verify.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                if (currentLanguage != com.example.util.AppLanguage.ENGLISH) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF7C4DFF).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Translation",
                                    tint = Color(0xFF7C4DFF),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Real-Time Dynamic Chat Translation active into ${currentLanguage.nativeName} (${currentLanguage.displayName})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF7C4DFF),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    val isFromMe = currentUser?.id == msg.senderId || currentUser?.email == msg.senderEmail
                    val translatedText = translatedMessages[msg.id] ?: translatedMessages["${msg.id}_${currentLanguage.code}"]
                    val displayText = if (currentLanguage != com.example.util.AppLanguage.ENGLISH && !translatedText.isNullOrBlank()) {
                        translatedText
                    } else {
                        msg.text
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (reactionTargetMessageId == msg.id) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                contentAlignment = if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                MessageReactionsBar(
                                    onSelectEmoji = { emoji ->
                                        onToggleReaction(msg.id, emoji)
                                        reactionTargetMessageId = null
                                    },
                                    onMoreClick = {
                                        val mId = msg.id
                                        reactionTargetMessageId = null
                                        customEmojiTargetMessageId = mId
                                    },
                                    onDismiss = { reactionTargetMessageId = null }
                                )
                            }
                        }

                        MessageBubble(
                            message = msg,
                            displayText = displayText,
                            isTranslated = currentLanguage != com.example.util.AppLanguage.ENGLISH && !translatedText.isNullOrBlank(),
                            targetLanguageName = currentLanguage.nativeName,
                            isFromMe = isFromMe,
                            currentUserId = currentUser?.id,
                            onToggleReaction = { emoji ->
                                onToggleReaction(msg.id, emoji)
                            },
                            onLongPress = {
                                actionTargetMessage = msg
                            },
                            onSwipeToReply = {
                                replyingToMessage = msg
                            },
                            onVotePoll = { optionIndex ->
                                onVotePoll(msg.id, optionIndex)
                            },
                            onMediaClick = {
                                if (!msg.mediaUri.isNullOrEmpty()) {
                                    scope.launch {
                                        val ok = ImageStorageHelper.downloadMediaFile(context, msg.mediaUri)
                                        if (ok) Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_SHORT).show()
                                        else Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onEmailBadgeClick = {
                                activeEmailHeader = EmailHeaderInfo(
                                    from = msg.senderEmail,
                                    to = currentUser?.email ?: "me@linko.com",
                                    subject = msg.emailSubject ?: "Linko Message",
                                    messageId = msg.emailMessageId ?: "<msg@linko.net>",
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

    if (actionTargetMessage != null) {
        val targetMsg = actionTargetMessage!!
        val isMine = currentUser?.id == targetMsg.senderId || currentUser?.email == targetMsg.senderEmail
        AlertDialog(
            onDismissRequest = { actionTargetMessage = null },
            title = {
                Column {
                    Text("Message Options", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    // Quick Emoji Reaction Bar
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val quickEmojis = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")
                            quickEmojis.forEach { emoji ->
                                Text(
                                    text = emoji,
                                    fontSize = 20.sp,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable {
                                            val id = targetMsg.id
                                            actionTargetMessage = null
                                            onToggleReaction(id, emoji)
                                        }
                                        .padding(4.dp)
                                )
                            }
                            Text(
                                text = "➕",
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        val id = targetMsg.id
                                        actionTargetMessage = null
                                        customEmojiTargetMessageId = id
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Reply Action
                    TextButton(
                        onClick = {
                            replyingToMessage = targetMsg
                            actionTargetMessage = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = "Reply", tint = Emerald500)
                            Spacer(Modifier.width(10.dp))
                            Text("Reply to Message", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    // Copy Text Action
                    if (targetMsg.text.isNotBlank()) {
                        TextButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(targetMsg.text))
                                actionTargetMessage = null
                                Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                Spacer(Modifier.width(10.dp))
                                Text("Copy Text", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        // Forward Action
                        TextButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(targetMsg.text))
                                actionTargetMessage = null
                                Toast.makeText(context, "Message copied for forwarding", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Share, contentDescription = "Forward")
                                Spacer(Modifier.width(10.dp))
                                Text("Forward Message", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    // Download Media Action
                    if (!targetMsg.mediaUri.isNullOrEmpty()) {
                        TextButton(
                            onClick = {
                                val uri = targetMsg.mediaUri
                                actionTargetMessage = null
                                scope.launch {
                                    val ok = ImageStorageHelper.downloadMediaFile(context, uri)
                                    if (ok) Toast.makeText(context, "Saved to Downloads folder!", Toast.LENGTH_SHORT).show()
                                    else Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Download, contentDescription = "Download")
                                Spacer(Modifier.width(10.dp))
                                Text("Download Media / File", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    // Delete for Me Action
                    TextButton(
                        onClick = {
                            val id = targetMsg.id
                            actionTargetMessage = null
                            onDeleteForMe(id)
                            Toast.makeText(context, "Message deleted for you", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete for Me", tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(10.dp))
                            Text("Delete for Me", color = MaterialTheme.colorScheme.error)
                        }
                    }

                    // Delete for Everyone Action
                    if (isMine) {
                        TextButton(
                            onClick = {
                                val id = targetMsg.id
                                actionTargetMessage = null
                                onDeleteForEveryone(id)
                                Toast.makeText(context, "Message deleted for everyone", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "Delete for Everyone", tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(10.dp))
                                Text("Delete for Everyone", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { actionTargetMessage = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun MessageBubble(
    message: MessageEntity,
    displayText: String,
    isTranslated: Boolean = false,
    targetLanguageName: String = "",
    isFromMe: Boolean,
    currentUserId: String?,
    onToggleReaction: (String) -> Unit,
    onLongPress: () -> Unit,
    onSwipeToReply: () -> Unit,
    onVotePoll: (optionIndex: Int) -> Unit,
    onMediaClick: () -> Unit = {},
    onEmailBadgeClick: () -> Unit
) {
    ChatBubble(
        text = displayText,
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
        reactionsJson = message.reactionsJson,
        onToggleReaction = onToggleReaction,
        onLongPress = onLongPress,
        onSwipeToReply = onSwipeToReply,
        onVotePoll = onVotePoll,
        onMediaClick = onMediaClick,
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
