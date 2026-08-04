package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CallLogEntity
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.StatusEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.ActiveCallState
import com.example.data.model.AuthState
import com.example.data.model.CallType
import com.example.data.model.UserStatusGroup
import com.example.data.repository.TalePulseRepository
import com.example.util.LinkoCallSignalingEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TalePulseViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = TalePulseRepository(
        userDao = db.userDao(),
        contactDao = db.contactDao(),
        chatDao = db.chatDao(),
        messageDao = db.messageDao(),
        callLogDao = db.callLogDao(),
        statusDao = db.statusDao()
    )

    val currentUser: StateFlow<UserEntity?> = repository.currentUserFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val authState: StateFlow<AuthState> = currentUser
        .map { user -> if (user != null) AuthState.Authenticated(user) else AuthState.Unauthenticated }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthState.Unauthenticated)

    val chats: StateFlow<List<ChatEntity>> = repository.allChatsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts: StateFlow<List<ContactEntity>> = currentUser.flatMapLatest { user ->
        if (user != null) repository.getContactsFlow(user.email) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callLogs: StateFlow<List<CallLogEntity>> = repository.allCallLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeStatuses: StateFlow<List<StatusEntity>> = repository.activeStatusesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userStatusGroups: StateFlow<List<UserStatusGroup>> = combine(
        currentUser,
        contacts,
        activeStatuses
    ) { user, contactList, statuses ->
        if (statuses.isEmpty()) return@combine emptyList<UserStatusGroup>()

        val meId = user?.id ?: ""
        val groupedByUserId = statuses.groupBy { it.userId }

        groupedByUserId.map { (userId, userStatuses) ->
            val isMe = userId == meId || (user != null && userStatuses.firstOrNull()?.userEmail == user.email)
            val displayName = if (isMe) (user?.displayName ?: "My Status") else (userStatuses.firstOrNull()?.userDisplayName ?: "Friend")
            val email = if (isMe) (user?.email ?: "") else (userStatuses.firstOrNull()?.userEmail ?: "")
            val avatarUri = if (isMe) user?.avatarUri else (userStatuses.firstOrNull()?.userAvatarUri ?: contactList.find { it.contactEmail == email }?.contactAvatarUri)
            val sortedStatuses = userStatuses.sortedBy { it.createdTimestamp }
            val latestTime = sortedStatuses.lastOrNull()?.createdTimestamp ?: 0L
            val hasUnviewed = sortedStatuses.any { !it.isViewed }

            UserStatusGroup(
                userId = userId,
                userDisplayName = displayName,
                userEmail = email,
                userAvatarUri = avatarUri,
                isCurrentUser = isMe,
                statuses = sortedStatuses,
                latestTimestamp = latestTime,
                hasUnviewed = hasUnviewed
            )
        }.sortedWith(
            compareByDescending<UserStatusGroup> { it.isCurrentUser }
                .thenByDescending { it.hasUnviewed }
                .thenByDescending { it.latestTimestamp }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    private val _selectedChatId = MutableStateFlow<String?>(null)
    val selectedChatId: StateFlow<String?> = _selectedChatId.asStateFlow()

    val currentChat: StateFlow<ChatEntity?> = _selectedChatId.flatMapLatest { chatId ->
        if (chatId != null) repository.getChatByIdFlow(chatId) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeMessages: StateFlow<List<MessageEntity>> = _selectedChatId.flatMapLatest { chatId ->
        if (chatId != null) repository.getMessagesFlow(chatId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeCallState = MutableStateFlow<ActiveCallState?>(null)
    val activeCallState: StateFlow<ActiveCallState?> = _activeCallState.asStateFlow()

    private val prefs = application.getSharedPreferences("linko_settings", android.content.Context.MODE_PRIVATE)

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _selectedThemePalette = MutableStateFlow(
        try {
            com.example.ui.theme.AppThemePalette.valueOf(
                prefs.getString("theme_palette", com.example.ui.theme.AppThemePalette.EMERALD.name) ?: com.example.ui.theme.AppThemePalette.EMERALD.name
            )
        } catch (_: Exception) {
            com.example.ui.theme.AppThemePalette.EMERALD
        }
    )
    val selectedThemePalette: StateFlow<com.example.ui.theme.AppThemePalette> = _selectedThemePalette.asStateFlow()

    private val _selectedChatWallpaper = MutableStateFlow(
        try {
            com.example.ui.theme.ChatWallpaper.valueOf(
                prefs.getString("chat_wallpaper", com.example.ui.theme.ChatWallpaper.DOODLE.name) ?: com.example.ui.theme.ChatWallpaper.DOODLE.name
            )
        } catch (_: Exception) {
            com.example.ui.theme.ChatWallpaper.DOODLE
        }
    )
    val selectedChatWallpaper: StateFlow<com.example.ui.theme.ChatWallpaper> = _selectedChatWallpaper.asStateFlow()

    private val _customWallpaperUri = MutableStateFlow(prefs.getString("custom_wallpaper_uri", null))
    val customWallpaperUri: StateFlow<String?> = _customWallpaperUri.asStateFlow()

    private val _customWallpaperDimming = MutableStateFlow(prefs.getFloat("custom_wallpaper_dimming", 0.3f))
    val customWallpaperDimming: StateFlow<Float> = _customWallpaperDimming.asStateFlow()

    private val _customWallpaperScale = MutableStateFlow(prefs.getString("custom_wallpaper_scale", "CROP") ?: "CROP")
    val customWallpaperScale: StateFlow<String> = _customWallpaperScale.asStateFlow()

    private val _appLanguage = MutableStateFlow(
        com.example.util.AppLanguage.fromCode(
            prefs.getString("app_language", com.example.util.AppLanguage.ENGLISH.code) ?: com.example.util.AppLanguage.ENGLISH.code
        )
    )
    val appLanguage: StateFlow<com.example.util.AppLanguage> = _appLanguage.asStateFlow()

    private val _translatedMessages = MutableStateFlow<Map<String, String>>(emptyMap())
    val translatedMessages: StateFlow<Map<String, String>> = _translatedMessages.asStateFlow()

    private val _actionStatusMessage = MutableStateFlow<String?>(null)
    val actionStatusMessage: StateFlow<String?> = _actionStatusMessage.asStateFlow()

    private var callTimerJob: Job? = null

    fun setAppLanguage(language: com.example.util.AppLanguage) {
        _appLanguage.value = language
        prefs.edit().putString("app_language", language.code).apply()
        _actionStatusMessage.value = "App language updated to ${language.nativeName}"
        translateActiveChatMessages()
    }

    fun translateActiveChatMessages() {
        val messages = activeMessages.value
        val lang = _appLanguage.value
        if (messages.isEmpty() || lang == com.example.util.AppLanguage.ENGLISH) {
            return
        }

        viewModelScope.launch {
            val currentMap = _translatedMessages.value.toMutableMap()
            for (msg in messages) {
                if (msg.text.isNotBlank()) {
                    val cacheKey = "${msg.id}_${lang.code}"
                    if (!currentMap.containsKey(cacheKey)) {
                        val translatedText = com.example.util.LocalizationManager.translateChatMessage(msg.text, lang)
                        currentMap[cacheKey] = translatedText
                        currentMap[msg.id] = translatedText
                    }
                }
            }
            _translatedMessages.value = currentMap
        }
    }

    fun toggleDarkMode() {
        val newValue = !_isDarkMode.value
        _isDarkMode.value = newValue
        prefs.edit().putBoolean("is_dark_mode", newValue).apply()
    }

    fun setThemePalette(palette: com.example.ui.theme.AppThemePalette) {
        _selectedThemePalette.value = palette
        prefs.edit().putString("theme_palette", palette.name).apply()
        _actionStatusMessage.value = "Color theme updated to ${palette.displayName}"
    }

    fun setChatWallpaper(wallpaper: com.example.ui.theme.ChatWallpaper) {
        _selectedChatWallpaper.value = wallpaper
        prefs.edit().putString("chat_wallpaper", wallpaper.name).apply()
        _actionStatusMessage.value = "Chat wallpaper set to ${wallpaper.displayName}"
    }

    fun setCustomWallpaper(uri: String, dimming: Float = 0.3f, scale: String = "CROP") {
        _customWallpaperUri.value = uri
        _customWallpaperDimming.value = dimming
        _customWallpaperScale.value = scale
        prefs.edit()
            .putString("custom_wallpaper_uri", uri)
            .putFloat("custom_wallpaper_dimming", dimming)
            .putString("custom_wallpaper_scale", scale)
            .apply()
        setChatWallpaper(com.example.ui.theme.ChatWallpaper.CUSTOM)
        _actionStatusMessage.value = "Custom wallpaper applied!"
    }

    private val _onlineUserEmails = MutableStateFlow<Set<String>>(setOf("gemini_ai@google.com", "gemini.ai@google.com"))
    val onlineUserEmails: StateFlow<Set<String>> = _onlineUserEmails.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
        }
        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    repository.syncContactsWithFirestore(user.email)
                    com.example.data.remote.FirestorePresenceService.updatePresence(user.email, user.id, true)
                }
            }
        }
        viewModelScope.launch {
            combine(activeMessages, appLanguage) { msgs, lang -> Pair(msgs, lang) }
                .collect { (msgs, lang) ->
                    if (lang != com.example.util.AppLanguage.ENGLISH && msgs.isNotEmpty()) {
                        translateActiveChatMessages()
                    }
                }
        }
        com.example.data.remote.FirestorePresenceService.startListeningToPresence(viewModelScope) { onlineSet ->
            _onlineUserEmails.value = onlineSet
        }
    }

    fun setAppForegroundState(isForeground: Boolean) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            com.example.data.remote.FirestorePresenceService.updatePresence(user.email, user.id, isForeground)
        }
    }

    fun isUserOnline(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        val normalized = email.lowercase().trim()
        if (normalized.contains("gemini")) return true
        return _onlineUserEmails.value.contains(normalized)
    }

    fun isChatOnline(chat: ChatEntity?): Boolean {
        if (chat == null) return false
        if (chat.isGroup) return false
        if (chat.id == "chat_gemini_ai" || chat.name.contains("Gemini")) return true

        val matchedContact = contacts.value.find { "chat_${it.contactUserId}" == chat.id || it.contactDisplayName.equals(chat.name, ignoreCase = true) }
        return if (matchedContact != null) isUserOnline(matchedContact.contactEmail) else isUserOnline(chat.name)
    }

    fun getContactForChat(chat: ChatEntity?): ContactEntity? {
        if (chat == null) return null
        return contacts.value.find { "chat_${it.contactUserId}" == chat.id || it.contactDisplayName.equals(chat.name, ignoreCase = true) }
    }

    fun loginOrRegister(email: String, displayName: String, username: String) {
        viewModelScope.launch {
            try {
                repository.registerOrLogin(email, displayName, username)
                _actionStatusMessage.value = "Welcome back!"
            } catch (e: Exception) {
                _actionStatusMessage.value = e.message ?: "Authentication failed."
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _selectedChatId.value = null
        }
    }

    fun selectChat(chatId: String) {
        _selectedChatId.value = chatId
        val me = currentUser.value
        viewModelScope.launch {
            if (me != null) {
                repository.markChatMessagesAsRead(chatId, me.id)
            } else {
                repository.clearChatUnread(chatId)
            }
        }
    }

    fun markChatMessagesAsRead(chatId: String) {
        val me = currentUser.value ?: return
        viewModelScope.launch {
            repository.markChatMessagesAsRead(chatId, me.id)
        }
    }

    fun clearSelectedChat() {
        _selectedChatId.value = null
    }

    fun clearChatHistory(chatId: String) {
        viewModelScope.launch {
            repository.clearChatHistory(chatId)
            _actionStatusMessage.value = "Chat history cleared"
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            repository.deleteChat(chatId)
            _selectedChatId.value = null
            _actionStatusMessage.value = "Chat deleted"
        }
    }

    fun addContactByEmail(email: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val me = currentUser.value ?: return@launch
            val result = repository.addContactByEmail(me.email, email)
            result.onSuccess {
                _actionStatusMessage.value = "Contact '${it.contactDisplayName}' added successfully!"
                onDone(true)
            }.onFailure {
                _actionStatusMessage.value = it.message ?: "Failed to add contact."
                onDone(false)
            }
        }
    }

    fun addContactByQr(qrPayload: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val me = currentUser.value ?: return@launch
            val result = repository.addContactFromQrPayload(me.email, qrPayload)
            result.onSuccess {
                _actionStatusMessage.value = "Added friend '${it.contactDisplayName}' via QR Code!"
                onDone(true)
            }.onFailure {
                _actionStatusMessage.value = it.message ?: "Invalid QR payload."
                onDone(false)
            }
        }
    }

    fun openDirectChat(contact: ContactEntity, onChatReady: (String) -> Unit) {
        viewModelScope.launch {
            val me = currentUser.value ?: return@launch
            val chat = repository.createOrGetDirectChat(me, contact)
            _selectedChatId.value = chat.id
            onChatReady(chat.id)
        }
    }

    fun openOrCreateDirectChat(userId: String, email: String, name: String, onChatReady: () -> Unit) {
        viewModelScope.launch {
            val me = currentUser.value ?: return@launch
            var contact = contacts.value.find { it.contactUserId == userId || it.contactEmail.equals(email, ignoreCase = true) }
            if (contact == null) {
                contact = ContactEntity(
                    id = "contact_$userId",
                    userEmail = me.email,
                    contactUserId = userId,
                    contactEmail = email,
                    contactDisplayName = name,
                    contactUsername = email.substringBefore("@")
                )
            }
            val chat = repository.createOrGetDirectChat(me, contact)
            _selectedChatId.value = chat.id
            onChatReady()
        }
    }

    fun openGeminiChat(onChatReady: (String) -> Unit) {
        viewModelScope.launch {
            val me = currentUser.value ?: return@launch
            val chat = repository.createOrGetGeminiChat(me)
            _selectedChatId.value = chat.id
            onChatReady(chat.id)
        }
    }

    fun createGroupChat(groupName: String, selectedContacts: List<ContactEntity>, groupDesc: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val me = currentUser.value ?: return@launch
            val group = repository.createGroupChat(me, groupName, selectedContacts, groupDesc)
            _selectedChatId.value = group.id
            _actionStatusMessage.value = "Group '$groupName' created!"
            onDone(group.id)
        }
    }

    fun addGroupMembers(chatId: String, newContacts: List<ContactEntity>) {
        viewModelScope.launch {
            val me = currentUser.value ?: return@launch
            repository.addGroupMembers(chatId, newContacts, me)
            _actionStatusMessage.value = "Added members to group"
        }
    }

    fun toggleAdminRole(chatId: String, memberUserId: String, makeAdmin: Boolean) {
        viewModelScope.launch {
            repository.toggleAdminRole(chatId, memberUserId, makeAdmin)
            _actionStatusMessage.value = if (makeAdmin) "Member promoted to Admin" else "Admin role removed"
        }
    }

    fun removeGroupMember(chatId: String, memberUserId: String) {
        viewModelScope.launch {
            repository.removeGroupMember(chatId, memberUserId)
            _actionStatusMessage.value = "Member removed from group"
        }
    }

    fun sendMessage(
        text: String,
        mediaUri: String? = null,
        mediaType: String? = null,
        formattedRichText: String? = null
    ) {
        val chatId = _selectedChatId.value ?: return
        val sender = currentUser.value ?: return
        if (text.isBlank() && mediaUri == null) return

        viewModelScope.launch {
            repository.sendMessage(chatId, sender, text, mediaUri, mediaType, formattedRichText)
        }
    }

    fun toggleReaction(messageId: String, emoji: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.toggleMessageReaction(messageId, emoji, user.id)
        }
    }

    fun deleteMessageForMe(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessageForMe(messageId)
            _actionStatusMessage.value = "Message deleted for you"
        }
    }

    fun deleteMessageForEveryone(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessageForEveryone(messageId)
            _actionStatusMessage.value = "Message deleted for everyone"
        }
    }

    fun votePoll(messageId: String, optionIndex: Int) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.votePoll(messageId, optionIndex, user.id)
        }
    }

    fun sendPoll(question: String, options: List<String>, allowMultiple: Boolean) {
        val chatId = _selectedChatId.value ?: return
        val sender = currentUser.value ?: return
        viewModelScope.launch {
            repository.sendPollMessage(chatId, sender, question, options, allowMultiple)
        }
    }

    fun sendLocation(title: String, address: String, latitude: Double, longitude: Double) {
        val chatId = _selectedChatId.value ?: return
        val sender = currentUser.value ?: return
        viewModelScope.launch {
            repository.sendLocationMessage(chatId, sender, title, address, latitude, longitude)
        }
    }

    fun sendContactAttachment(contactName: String, email: String, phone: String) {
        val chatId = _selectedChatId.value ?: return
        val sender = currentUser.value ?: return
        viewModelScope.launch {
            repository.sendContactMessage(chatId, sender, contactName, email, phone)
        }
    }

    fun sendEventAttachment(title: String, dateText: String, locationText: String) {
        val chatId = _selectedChatId.value ?: return
        val sender = currentUser.value ?: return
        viewModelScope.launch {
            repository.sendEventMessage(chatId, sender, title, dateText, locationText)
        }
    }

    fun startCall(
        contactName: String,
        contactEmail: String,
        contactAvatar: String?,
        callType: CallType,
        context: Context? = null
    ) {
        startGoogleMeetCall(
            context = context ?: getApplication<Application>().applicationContext,
            contactName = contactName,
            contactEmail = contactEmail,
            contactAvatar = contactAvatar,
            callType = callType
        )
    }

    fun startGoogleMeetCall(
        context: Context,
        contactName: String,
        contactEmail: String,
        contactAvatar: String?,
        callType: CallType = CallType.VIDEO
    ) {
        val meetCode = generateMeetCode()
        val meetUrl = "https://meet.google.com/$meetCode"
        val callTypeLabel = if (callType == CallType.VIDEO) "Video Call" else "Audio Call"

        // 1. Send Google Meet link into current active chat if available
        val chatId = _selectedChatId.value
        val sender = currentUser.value
        if (chatId != null && sender != null) {
            viewModelScope.launch {
                repository.sendMessage(
                    chatId = chatId,
                    sender = sender,
                    text = "📹 Join Google Meet $callTypeLabel: $meetUrl"
                )
            }
        }

        // 2. Add Call Log entry
        viewModelScope.launch {
            repository.addCallLog(
                contactName = contactName,
                contactEmail = contactEmail,
                contactAvatarUri = contactAvatar,
                callType = callType,
                isIncoming = false,
                isMissed = false,
                durationSeconds = 0
            )
        }

        // 3. Launch Google Meet Intent
        try {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(meetUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            _actionStatusMessage.value = "Launching Google Meet for $contactName..."
        } catch (e: Exception) {
            _actionStatusMessage.value = "Google Meet Link Created: $meetUrl"
        }
    }

    private fun generateMeetCode(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz"
        val p1 = (1..3).map { chars.random() }.joinToString("")
        val p2 = (1..4).map { chars.random() }.joinToString("")
        val p3 = (1..3).map { chars.random() }.joinToString("")
        return "$p1-$p2-$p3"
    }

    fun toggleMute() {
        _activeCallState.value = _activeCallState.value?.let { it.copy(isMuted = !it.isMuted) }
    }

    fun toggleSpeaker() {
        _activeCallState.value = _activeCallState.value?.let { it.copy(isSpeakerOn = !it.isSpeakerOn) }
    }

    fun toggleVideo() {
        _activeCallState.value = _activeCallState.value?.let { it.copy(isVideoEnabled = !it.isVideoEnabled) }
    }

    fun switchCamera() {
        _activeCallState.value = _activeCallState.value?.let { it.copy(isFrontCamera = !it.isFrontCamera) }
    }

    fun endCall() {
        val currentCall = _activeCallState.value ?: return
        val signalingEngine = LinkoCallSignalingEngine.getInstance(getApplication())
        signalingEngine.endCall()

        viewModelScope.launch {
            repository.addCallLog(
                contactName = currentCall.contactName,
                contactEmail = currentCall.contactEmail,
                contactAvatarUri = currentCall.contactAvatarUri,
                callType = currentCall.callType,
                isIncoming = false,
                isMissed = false,
                durationSeconds = currentCall.callDurationSeconds
            )
        }
        callTimerJob?.cancel()
        _activeCallState.value = null
    }

    fun clearStatusMessage() {
        _actionStatusMessage.value = null
    }

    fun updateProfile(displayName: String, statusMessage: String) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            val updated = user.copy(
                displayName = displayName.ifBlank { user.displayName },
                statusMessage = statusMessage.ifBlank { user.statusMessage }
            )
            repository.updateProfile(updated)
            _actionStatusMessage.value = "Profile updated successfully"
        }
    }

    fun updateAvatarUri(avatarUri: String?) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            val updated = user.copy(avatarUri = avatarUri)
            repository.updateProfile(updated)
            _actionStatusMessage.value = if (!avatarUri.isNullOrBlank()) "Profile picture updated!" else "Profile picture removed"
        }
    }

    fun postTextStatus(text: String, bgHex: String, fontStyle: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            repository.postStatus(
                user = user,
                type = "TEXT",
                textContent = text,
                mediaUri = null,
                backgroundColorHex = bgHex,
                fontStyle = fontStyle
            )
            _actionStatusMessage.value = "Status posted successfully! ✨"
            onSuccess()
        }
    }

    fun postMediaStatus(mediaUri: String, mediaType: String, caption: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            repository.postStatus(
                user = user,
                type = mediaType,
                textContent = caption,
                mediaUri = mediaUri,
                backgroundColorHex = "#10B981",
                fontStyle = "DEFAULT"
            )
            _actionStatusMessage.value = "Status posted successfully! 📸"
            onSuccess()
        }
    }

    fun markStatusAsViewed(statusId: String) {
        viewModelScope.launch {
            repository.markStatusAsViewed(statusId)
        }
    }

    fun deleteStatus(statusId: String) {
        viewModelScope.launch {
            repository.deleteStatus(statusId)
            _actionStatusMessage.value = "Status removed"
        }
    }

    fun replyToStatus(contactEmail: String, replyText: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val contact = contacts.value.find { it.contactEmail == contactEmail }
            val me = currentUser.value
            if (contact != null && me != null) {
                val chat = repository.createOrGetDirectChat(me, contact)
                _selectedChatId.value = chat.id
                repository.sendMessage(
                    chatId = chat.id,
                    sender = me,
                    text = "Replying to status: $replyText"
                )
                _actionStatusMessage.value = "Reply sent to ${contact.contactDisplayName}"
                onComplete()
            } else {
                _actionStatusMessage.value = "Unable to send reply"
            }
        }
    }

    fun processNotificationIntent(
        intent: Intent,
        context: Context,
        onNavigate: (screen: String, chatId: String?) -> Unit
    ) {
        val extraScreen = intent.getStringExtra(com.example.notification.NotificationHelper.EXTRA_SCREEN)
        val extraChatId = intent.getStringExtra(com.example.notification.NotificationHelper.EXTRA_CHAT_ID)
        val extraReplyChatId = intent.getStringExtra("extra_direct_reply_chat_id")
        val extraReplyText = intent.getStringExtra("extra_direct_reply_text")
        val extraAcceptEmail = intent.getStringExtra("extra_accept_friend_email")
        val extraDeclineCall = intent.getBooleanExtra("extra_decline_call", false)

        // Process Direct Reply from Notification
        if (!extraReplyChatId.isNullOrEmpty() && !extraReplyText.isNullOrEmpty()) {
            val user = currentUser.value
            if (user != null) {
                viewModelScope.launch {
                    repository.sendMessage(extraReplyChatId, user, extraReplyText)
                    _selectedChatId.value = extraReplyChatId
                    onNavigate(com.example.notification.NotificationHelper.SCREEN_DIRECT_CHAT, extraReplyChatId)
                }
            }
        }

        // Process Accept Friend Request Action
        if (!extraAcceptEmail.isNullOrEmpty()) {
            val me = currentUser.value
            if (me != null) {
                viewModelScope.launch {
                    repository.addContactByEmail(me.email, extraAcceptEmail)
                    _actionStatusMessage.value = "Accepted friend request from $extraAcceptEmail"
                    onNavigate(com.example.notification.NotificationHelper.SCREEN_FRIENDS, null)
                }
            }
        }

        // Process Decline Call Action
        if (extraDeclineCall) {
            _activeCallState.value = null
        }

        // Process Screen Navigation Intent
        if (!extraScreen.isNullOrEmpty()) {
            when (extraScreen) {
                com.example.notification.NotificationHelper.SCREEN_DIRECT_CHAT -> {
                    if (!extraChatId.isNullOrEmpty()) {
                        selectChat(extraChatId)
                        onNavigate(com.example.notification.NotificationHelper.SCREEN_DIRECT_CHAT, extraChatId)
                    }
                }
                com.example.notification.NotificationHelper.SCREEN_INCOMING_CALL -> {
                    val callerName = intent.getStringExtra(com.example.notification.NotificationHelper.EXTRA_CALLER_NAME) ?: "Caller"
                    val callerEmail = intent.getStringExtra(com.example.notification.NotificationHelper.EXTRA_CALLER_EMAIL) ?: ""
                    val callTypeStr = intent.getStringExtra(com.example.notification.NotificationHelper.EXTRA_CALL_TYPE) ?: "VOICE"
                    val callType = if (callTypeStr.equals("VIDEO", ignoreCase = true)) CallType.VIDEO else CallType.AUDIO
                    startCall(callerName, callerEmail, null, callType)
                    onNavigate("calls", null)
                }
                com.example.notification.NotificationHelper.SCREEN_FRIENDS -> {
                    onNavigate("friends", null)
                }
            }
        }
    }

    fun triggerSimulatedMessageNotification(context: Context) {
        viewModelScope.launch {
            val firstChat = chats.value.firstOrNull()
            if (firstChat == null) {
                _actionStatusMessage.value = "No active chat found. Add a contact or start a conversation first."
                return@launch
            }
            val chatId = firstChat.id
            val senderName = firstChat.name
            val text = "Hey! Let me know if you can see this push notification reply test 🚀"

            com.example.notification.NotificationHelper.showNewMessageNotification(
                context = context,
                chatId = chatId,
                senderName = senderName,
                messageText = text
            )
            _actionStatusMessage.value = "Push notification triggered for Message from $senderName!"
        }
    }

    fun triggerSimulatedCallNotification(context: Context, isVideo: Boolean = false) {
        val firstContact = contacts.value.firstOrNull()
        val callerName = firstContact?.contactDisplayName ?: currentUser.value?.displayName ?: "Contact"
        val callerEmail = firstContact?.contactEmail ?: currentUser.value?.email ?: "contact@linko.com"
        val callType = if (isVideo) "Video" else "Voice"

        com.example.notification.NotificationHelper.showIncomingCallNotification(
            context = context,
            callerName = callerName,
            callerEmail = callerEmail,
            callType = callType
        )
        _actionStatusMessage.value = "Push notification triggered for Incoming $callType Call from $callerName!"
    }

    fun triggerSimulatedFriendRequestNotification(context: Context) {
        val requesterName = "New Connection"
        val requesterEmail = "invite@linko.com"

        com.example.notification.NotificationHelper.showFriendRequestNotification(
            context = context,
            requesterName = requesterName,
            requesterEmail = requesterEmail
        )
        _actionStatusMessage.value = "Push notification triggered for Friend Request!"
    }

}

