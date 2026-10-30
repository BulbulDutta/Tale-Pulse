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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
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
import com.example.data.local.entity.MessageStatus
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
import com.example.ui.components.MessageStatusTicks
import com.example.ui.components.PollCreationDialog
import com.example.ui.components.RecipientProfileSheet
import com.example.ui.components.UserAvatar
import com.example.util.ImageStorageHelper
import com.example.ui.theme.ChatWallpaper
import com.example.ui.theme.ChatWallpaperBackground
import com.example.ui.theme.Emerald500
import com.example.ui.theme.StatusReadBlue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
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
    onMarkVisibleMessagesAsRead: (chatId: String, visibleMessageIds: List<String>) -> Unit = { _, _ -> },
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
    onFecharGoogleMeet: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var selectedRichStyle by remember { mutableStateOf<String?>(null) } // "BOLD", "ITALIC", "CODE"
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var activeEmailHeader by remember { mutableStateOf<EmailHeaderInfo?>(null) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var showInviteContactsDialog by remember { mutableStateOf(false) }

    val recipientContact = remember(chat, contacts, currentUser) {
        if (chat == null || chat.isGroup) null
        else {
            val participantIds = try {
                if (chat.participantIdsJson.isBlank()) emptyList()
                else {
                    val array = org.json.JSONArray(chat.participantIdsJson)
                    (0 until array.length()).map { array.getString(it) }
                }
            } catch (_: Exception) {
                emptyList()
            }
            val myId = currentUser?.id ?: ""
            val otherUserId = participantIds.find { it != myId }
            if (otherUserId != null) {
                contacts.find { it.contactUserId == otherUserId } ?: contacts.find { it.contactDisplayName.equals(chat.name, ignoreCase = true) }
            } else {
                contacts.find { "chat_${it.contactUserId}" == chat.id || it.contactDisplayName.equals(chat.name, ignoreCase = true) }
            }
        }
    }

    val isGeminiChat = chat?.id == "chat_gemini_ai" || chat?.name?.contains("Gemini") == true
    val isAiBot = isGeminiChat

    val displayTitleName = remember(chat, recipientContact, currentUser) {
        when {
            chat == null -> "Chat"
            isGeminiChat -> "Gemini AI"
            chat.isGroup -> chat.name
            recipientContact != null -> recipientContact.contactDisplayName
            currentUser != null && chat.name.equals(currentUser.displayName, ignoreCase = true) -> "Contact"
            else -> chat.name.ifBlank { "Contact" }
        }
    }

    val displayAvatarUri = remember(chat, recipientContact) {
        when {
            chat == null -> null
            chat.isGroup -> chat.groupIconUri
            recipientContact != null -> recipientContact.contactAvatarUri
            else -> chat.groupIconUri
        }
    }

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
    var isHoldingMicButton by remember { mutableStateOf(false) }
    var isRecordingLocked by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }
    var recordingDeciseconds by remember { mutableStateOf(0) }
    var slideCancelOffsetX by remember { mutableStateOf(0f) }
    var holdHintMessage by remember { mutableStateOf<String?>(null) }
    val liveWaveformAmplitudes = remember {
        mutableStateListOf(
            0.25f, 0.35f, 0.50f, 0.30f, 0.65f, 0.45f, 0.70f, 0.40f,
            0.55f, 0.30f, 0.60f, 0.35f, 0.45f, 0.25f, 0.50f, 0.35f
        )
    }
    var actionTargetMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var messageInfoTarget by remember { mutableStateOf<MessageEntity?>(null) }
    var replyingToMessage by remember { mutableStateOf<MessageEntity?>(null) }

    val sheetState = rememberModalBottomSheetState()

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val audioRecorderManager = remember { com.example.util.AudioRecorderManager(context) }

    // Ensure recorder is cleaned up if user leaves the chat screen while recording
    DisposableEffect(Unit) {
        onDispose {
            if (isRecordingAudio) {
                audioRecorderManager.cancelRecording()
            }
        }
    }

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

    val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            holdHintMessage = "Microphone enabled — Hold the mic button to record & release to send"
        } else {
            Toast.makeText(context, "Microphone permission is required to record voice notes", Toast.LENGTH_SHORT).show()
        }
    }

    val finishAndSendVoiceNote: () -> Unit = {
        val result = audioRecorderManager.stopRecordingWithMetadata()
        isRecordingAudio = false
        isHoldingMicButton = false
        isRecordingLocked = false
        slideCancelOffsetX = 0f
        recordingSeconds = 0
        recordingDeciseconds = 0
        if (result != null) {
            val replyPrefix = if (replyingToMessage != null) {
                val rMsg = replyingToMessage!!
                val textSnippet = rMsg.text.ifBlank { rMsg.mediaType ?: "Media" }
                "REPLY:${rMsg.senderName}|$textSnippet"
            } else null
            replyingToMessage = null

            scope.launch {
                val savedUri = ImageStorageHelper.saveUriToInternalStorage(context, Uri.fromFile(result.file))
                val audioUri = savedUri?.toString() ?: Uri.fromFile(result.file).toString()
                onSendMessage(
                    "🎙️ Voice Note (${result.durationFormatted})",
                    audioUri,
                    "AUDIO",
                    replyPrefix
                )
            }
        }
    }

    val cancelActiveVoiceNote: (Boolean) -> Unit = { showToast ->
        audioRecorderManager.cancelRecording()
        isRecordingAudio = false
        isHoldingMicButton = false
        isRecordingLocked = false
        slideCancelOffsetX = 0f
        recordingSeconds = 0
        recordingDeciseconds = 0
        if (showToast) {
            holdHintMessage = "Voice note recording cancelled"
        }
    }

    LaunchedEffect(holdHintMessage) {
        if (holdHintMessage != null) {
            delay(2800)
            holdHintMessage = null
        }
    }

    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            recordingSeconds = 0
            recordingDeciseconds = 0
            var tickCount = 0
            while (isRecordingAudio) {
                delay(100)
                tickCount++
                recordingDeciseconds = tickCount % 10
                recordingSeconds = tickCount / 10
                val amp = audioRecorderManager.sampleNormalizedAmplitude()
                if (liveWaveformAmplitudes.size >= 18) {
                    liveWaveformAmplitudes.removeAt(0)
                }
                liveWaveformAmplitudes.add(amp)
            }
        }
    }

    val listState = rememberLazyListState()

    val lifecycleOwner = LocalLifecycleOwner.current
    var isScreenResumed by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isScreenResumed = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> isScreenResumed = false
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Trigger Stage 3 ('read' - Double Blue Tick) ONLY when recipient has opened the chat
    // and the incoming message is actually visible on their screen.
    LaunchedEffect(chat?.id, messages, isScreenResumed, currentUser?.id) {
        val activeChatId = chat?.id ?: return@LaunchedEffect
        if (!isScreenResumed) return@LaunchedEffect

        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? String }.toSet()
        }
            .distinctUntilChanged()
            .collect { visibleItemKeys ->
                if (visibleItemKeys.isEmpty()) return@collect
                val myId = currentUser?.id ?: ""
                val myEmail = currentUser?.email?.lowercase()?.trim() ?: ""

                val visibleUnreadIds = messages.filter { msg ->
                    val isVisibleOnScreen = msg.id in visibleItemKeys
                    val isFromRecipient = (myId.isNotBlank() && msg.senderId != myId) ||
                            (myEmail.isNotBlank() && msg.senderEmail.lowercase().trim() != myEmail)
                    val isNotYetRead = MessageStatus.normalize(msg.status) != MessageStatus.READ
                    isVisibleOnScreen && isFromRecipient && isNotYetRead
                }.map { it.id }

                if (visibleUnreadIds.isNotEmpty()) {
                    onMarkVisibleMessagesAsRead(activeChatId, visibleUnreadIds)
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

    if (showInviteContactsDialog && chat?.isGroup == true) {
        val currentParticipants = remember(chat.participantIdsJson) {
            try {
                if (chat.participantIdsJson.isBlank()) emptyList()
                else {
                    val arr = org.json.JSONArray(chat.participantIdsJson)
                    (0 until arr.length()).map { arr.getString(it) }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
        val availableContacts = remember(contacts, currentParticipants) {
            contacts.filter { it.contactUserId !in currentParticipants }
        }
        val selectedInviteContacts = remember { mutableStateListOf<ContactEntity>() }
        val isChannel = chat.id.startsWith("channel_")

        AlertDialog(
            onDismissRequest = { showInviteContactsDialog = false },
            title = {
                Text(
                    text = if (isChannel) "Invite Contacts to Channel" else "Invite Contacts to Group",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (availableContacts.isEmpty()) {
                    Text("All your saved contacts are already members of \"${chat.name}\".")
                } else {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Select one or more contacts to invite to \"${chat.name}\":",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        availableContacts.forEach { contact ->
                            val isChecked = selectedInviteContacts.any { it.id == contact.id }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (isChecked) selectedInviteContacts.removeAll { it.id == contact.id }
                                        else selectedInviteContacts.add(contact)
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            if (selectedInviteContacts.none { it.id == contact.id }) {
                                                selectedInviteContacts.add(contact)
                                            }
                                        } else {
                                            selectedInviteContacts.removeAll { it.id == contact.id }
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = Emerald500)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                UserAvatar(
                                    name = contact.contactDisplayName,
                                    avatarUri = contact.contactAvatarUri,
                                    size = 36.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = contact.contactDisplayName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = contact.contactEmail,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (availableContacts.isNotEmpty()) {
                    Button(
                        onClick = {
                            onAddGroupMembers(chat.id, selectedInviteContacts.toList())
                            showInviteContactsDialog = false
                            Toast.makeText(
                                context,
                                "Invited ${selectedInviteContacts.size} contact(s) to ${chat.name}",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        enabled = selectedInviteContacts.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                        modifier = Modifier.testTag("confirm_group_invite_button")
                    ) {
                        Text("Invite (${selectedInviteContacts.size})", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteContactsDialog = false }) {
                    Text("Close")
                }
            }
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
                                    name = displayTitleName,
                                    avatarUri = displayAvatarUri,
                                    isGroup = chat?.isGroup ?: false,
                                    size = 38.dp,
                                    showOnlineStatus = true,
                                    isOnline = isChatUserOnline
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = displayTitleName,
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
                                    val groupMemberCount = remember(chat?.participantIdsJson) {
                                        try {
                                            if (chat?.participantIdsJson.isNullOrBlank()) 1
                                            else org.json.JSONArray(chat?.participantIdsJson).length().coerceAtLeast(1)
                                        } catch (_: Exception) {
                                            1
                                        }
                                    }
                                    val isBroadcastChannel = chat?.id?.startsWith("channel_") == true
                                     Text(
                                        text = when {
                                            isGeminiChat -> "Gemini AI • E2EE"
                                            chat?.isGroup == true && isBroadcastChannel -> "Channel • $groupMemberCount members • E2EE"
                                            chat?.isGroup == true -> "Group • $groupMemberCount members • E2EE"
                                            isChatUserOnline -> "Online • E2EE"
                                            else -> "Offline • E2EE"
                                        },
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isGeminiChat) Color(0xFF7C4DFF) else if (isChatUserOnline || chat?.isGroup == true) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant
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
                        if (chat?.isGroup == true) {
                            IconButton(
                                onClick = { showInviteContactsDialog = true },
                                modifier = Modifier.testTag("group_invite_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = "Invite Contacts to Group",
                                    tint = Emerald500
                                )
                            }
                        }
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

                    // Floating Hold-to-Record Tooltip Hint
                    AnimatedVisibility(visible = holdHintMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.92f),
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .testTag("voice_record_hint_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = Emerald500,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = holdHintMessage ?: "",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.inverseOnSurface
                                    )
                                }
                                IconButton(
                                    onClick = { holdHintMessage = null },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss Hint",
                                        tint = MaterialTheme.colorScheme.inverseOnSurface,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Single-row WhatsApp/Signal style chat input bar with persistent Hold-to-Record Mic button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isRecordingAudio) {
                            // Live Voice Note Recording Bar (active during Hold-to-Record or Locked mode)
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("active_voice_recording_bar")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Pulsing Mic + Timer
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (recordingDeciseconds % 2 == 0) MaterialTheme.colorScheme.error
                                                    else MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val durationText = "${formatSeconds(recordingSeconds)}.$recordingDeciseconds"
                                        Text(
                                            text = durationText,
                                            fontSize = 14.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.testTag("voice_recording_timer")
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Live Microphone Amplitude Waveform Bars
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(24.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        liveWaveformAmplitudes.forEach { amp ->
                                            val barH = (4.dp + (18.dp * amp.coerceIn(0.15f, 1.0f)))
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(barH)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(MaterialTheme.colorScheme.error)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    if (isRecordingLocked) {
                                        // Hands-free Locked Controls: Cancel or Send
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    cancelActiveVoiceNote(true)
                                                },
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .testTag("cancel_voice_note_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Cancel Voice Note",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    finishAndSendVoiceNote()
                                                },
                                                colors = IconButtonDefaults.iconButtonColors(containerColor = Emerald500),
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .testTag("send_locked_voice_note_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                                    contentDescription = "Send Voice Note",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        // Hold-to-Record instruction: Slide left to cancel / Release to send
                                        Text(
                                            text = if (slideCancelOffsetX < -40f) "◀ Keep sliding to cancel" else "◀ Slide to cancel • Release to send",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        } else {
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

                                    // Google Maps Share Location quick action button
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            showLocationDialog = true
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("quick_share_location_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = "Share Google Maps Location",
                                            tint = Emerald500,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Right Primary Action Button:
                        // - Shows Send Text button when inputText is non-blank and not currently recording audio
                        // - Shows Hold-to-Record Microphone Button when inputText is blank (or while actively holding/recording)
                        if (inputText.isNotBlank() && !isRecordingAudio) {
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
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("send_message_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            // Hold-to-Record Microphone Button (48.dp touch target)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(48.dp)
                                    .scale(if (isHoldingMicButton) 1.16f else 1.0f)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isHoldingMicButton -> MaterialTheme.colorScheme.error
                                            isRecordingLocked -> Emerald500
                                            isGeminiChat -> Color(0xFF7C4DFF)
                                            else -> Emerald500
                                        }
                                    )
                                    .pointerInput(Unit) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            down.consume()

                                            // If already in locked hands-free mode, tapping the button finishes & sends
                                            if (isRecordingAudio && isRecordingLocked) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                finishAndSendVoiceNote()
                                                return@awaitEachGesture
                                            }

                                            // Verify RECORD_AUDIO runtime permission
                                            val hasMicPermission = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.RECORD_AUDIO
                                            ) == PackageManager.PERMISSION_GRANTED

                                            if (!hasMicPermission) {
                                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                return@awaitEachGesture
                                            }

                                            // Start Hold-to-Record immediately on press down
                                            val pressStartTime = System.currentTimeMillis()
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            holdHintMessage = null
                                            slideCancelOffsetX = 0f
                                            isHoldingMicButton = true
                                            isRecordingLocked = false
                                            audioRecorderManager.startRecording()
                                            isRecordingAudio = true

                                            var dragTotalX = 0f
                                            var dragTotalY = 0f
                                            var cancelledBySlide = false
                                            var lockedBySlideUp = false

                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val activeChange = event.changes.firstOrNull { it.id == down.id }
                                                    ?: event.changes.firstOrNull()

                                                if (activeChange == null || !activeChange.pressed) {
                                                    break
                                                }

                                                val delta = activeChange.positionChange()
                                                if (delta != androidx.compose.ui.geometry.Offset.Zero) {
                                                    activeChange.consume()
                                                    dragTotalX += delta.x
                                                    dragTotalY += delta.y
                                                    slideCancelOffsetX = dragTotalX.coerceAtMost(0f)

                                                    // Slide left past threshold -> cancel voice note
                                                    if (dragTotalX < -190f) {
                                                        cancelledBySlide = true
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        cancelActiveVoiceNote(true)
                                                        break
                                                    }

                                                    // Slide up past threshold -> lock hands-free recording
                                                    if (dragTotalY < -140f) {
                                                        lockedBySlideUp = true
                                                        isRecordingLocked = true
                                                        isHoldingMicButton = false
                                                        slideCancelOffsetX = 0f
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        holdHintMessage = "Recording locked hands-free — Tap Send when finished"
                                                        break
                                                    }
                                                }
                                            }

                                            // Pointer released
                                            if (!cancelledBySlide && !lockedBySlideUp) {
                                                val heldDurationMs = System.currentTimeMillis() - pressStartTime
                                                isHoldingMicButton = false
                                                slideCancelOffsetX = 0f
                                                if (heldDurationMs >= 350L) {
                                                    // Standard Hold-to-Record release -> immediately send voice note!
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    finishAndSendVoiceNote()
                                                } else {
                                                    // Quick tap (< 350ms) -> lock recording & show helpful Hold-to-Record hint
                                                    isRecordingLocked = true
                                                    holdHintMessage = "Hold mic & release to send instantly, or tap Send to finish"
                                                }
                                            }
                                        }
                                    }
                                    .testTag("hold_to_record_audio_button")
                            ) {
                                Icon(
                                    imageVector = if (isRecordingLocked) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                                    contentDescription = if (isHoldingMicButton) {
                                        "Recording Voice Note - Release to Send"
                                    } else if (isRecordingLocked) {
                                        "Send Locked Voice Note"
                                    } else {
                                        "Hold to Record Voice Note"
                                    },
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
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
                                .heightIn(min = 180.dp, max = 240.dp)
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
                            },
                            onStatusTicksClick = {
                                messageInfoTarget = msg
                            },
                            onFecharGoogleMeet = onFecharGoogleMeet
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
                    // Message Info Action
                    TextButton(
                        onClick = {
                            val msg = targetMsg
                            actionTargetMessage = null
                            messageInfoTarget = msg
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = "Message Info", tint = Emerald500)
                            Spacer(Modifier.width(10.dp))
                            Text("Message Info (Read & Delivered)", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

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

    if (messageInfoTarget != null) {
        val infoMsg = messageInfoTarget!!
        val normalizedStatus = MessageStatus.normalize(infoMsg.status)
        AlertDialog(
            onDismissRequest = { messageInfoTarget = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MessageStatusTicks(status = normalizedStatus, iconSize = 20.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Message Delivery & Read Info", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Sent",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Sent to Server (Single Gray Tick)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(formatTime(infoMsg.timestamp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Delivered",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Delivered to Recipient (Double Gray Tick)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                text = if (infoMsg.deliveredAt != null) formatTime(infoMsg.deliveredAt)
                                else if (normalizedStatus in listOf(MessageStatus.DELIVERED, MessageStatus.READ)) "Delivered"
                                else "Pending recipient device connection",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Read",
                            tint = StatusReadBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Read by Recipient (Double Blue Tick)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                text = if (infoMsg.readAt != null) formatTime(infoMsg.readAt)
                                else if (normalizedStatus == MessageStatus.READ) "Seen on screen"
                                else "Not yet seen on recipient's screen",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { messageInfoTarget = null }) {
                    Text("Done")
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
    onEmailBadgeClick: () -> Unit,
    onStatusTicksClick: () -> Unit = {},
    onFecharGoogleMeet: () -> Unit = {}
) {
    ChatBubble(
        text = displayText,
        senderName = message.senderName,
        isFromMe = isFromMe,
        timestampText = formatTime(message.timestamp),
        status = message.status,
        deliveredAt = message.deliveredAt,
        readAt = message.readAt,
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
        onEmailBadgeClick = onEmailBadgeClick,
        onStatusTicksClick = onStatusTicksClick,
        onFecharGoogleMeet = onFecharGoogleMeet
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
