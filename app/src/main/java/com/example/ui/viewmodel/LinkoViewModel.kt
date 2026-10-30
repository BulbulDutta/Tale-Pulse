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
import com.example.data.repository.LinkoRepository
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
class LinkoViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = LinkoRepository(
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
        .map { user ->
            if (user != null && user.email != "guest@linko.app" && user.id != "user_guest_local") {
                AuthState.Authenticated(user)
            } else {
                AuthState.Unauthenticated
            }
        }
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

    private val _isAppInForeground = MutableStateFlow(true)
    private val _pendingNotificationIntent = MutableStateFlow<Intent?>(null)
    val pendingNotificationIntent: StateFlow<Intent?> = _pendingNotificationIntent.asStateFlow()

    private val _pendingSharedContent = MutableStateFlow<SharedIncomingContent?>(null)
    val pendingSharedContent: StateFlow<SharedIncomingContent?> = _pendingSharedContent.asStateFlow()

    private val _fcmToken = MutableStateFlow(
        com.example.notification.LinkoFirebaseMessagingService.getSavedToken(application) ?: "Initializing FCM..."
    )
    val fcmToken: StateFlow<String> = _fcmToken.asStateFlow()

    private val _isFirebaseAuthenticated = MutableStateFlow(
        com.example.data.remote.FirestoreProvider.isAuthenticated()
    )

    private val firebaseAuthListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { auth ->
        _isFirebaseAuthenticated.value = auth.currentUser != null
    }

    init {
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener(firebaseAuthListener)
        } catch (_: Throwable) {}

        com.example.notification.LinkoFirebaseMessagingService.initializeFcmAndRegisterToken(application) { token ->
            _fcmToken.value = token
        }

        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
            val fbUser = try {
                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            } catch (_: Throwable) {
                null
            }
            if (fbUser != null && !fbUser.email.isNullOrBlank()) {
                prefs.edit().remove("is_guest_mode").remove("guest_display_name").apply()
                val email = fbUser.email ?: ""
                val displayName = fbUser.displayName ?: email.substringBefore("@")
                val username = email.substringBefore("@").replace(".", "_")
                repository.registerOrLogin(email, displayName, username)
            } else {
                prefs.edit().remove("is_guest_mode").remove("guest_display_name").apply()
                repository.logout()
            }
        }
        viewModelScope.launch {
            combine(currentUser, _isFirebaseAuthenticated) { user, isFbAuth ->
                Pair(user, isFbAuth)
            }.collect { (user, isFbAuth) ->
                if (user != null && isFbAuth) {
                    com.example.notification.LinkoFirebaseMessagingService.initializeFcmAndRegisterToken(
                        context = getApplication(),
                        userEmail = user.email
                    ) { token ->
                        _fcmToken.value = token
                    }

                    repository.syncContactsWithFirestore(user.email) { newContact ->
                        com.example.notification.NotificationHelper.showFriendRequestNotification(
                            context = getApplication(),
                            requesterName = newContact.contactDisplayName,
                            requesterEmail = newContact.contactEmail
                        )
                    }
                    repository.syncGroupsWithFirestore(user)
                    repository.syncMessagesWithFirestore(user) { incomingMessage, plainPreview, isGroupChat ->
                        val isCurrentlyViewingThisChat = _isAppInForeground.value && _selectedChatId.value == incomingMessage.chatId
                        if (!isCurrentlyViewingThisChat) {
                            com.example.notification.NotificationHelper.showNewMessageNotification(
                                context = getApplication(),
                                chatId = incomingMessage.chatId,
                                senderName = incomingMessage.senderName.ifBlank { "Linko Contact" },
                                messageText = plainPreview,
                                isGroup = isGroupChat,
                                senderEmail = incomingMessage.senderEmail
                            )
                        }
                    }
                    com.example.data.remote.FirestorePresenceService.updatePresence(user.email, user.id, true)
                    com.example.data.remote.FirestorePresenceService.startListeningToPresence(viewModelScope) { onlineSet ->
                        _onlineUserEmails.value = onlineSet
                    }
                } else {
                    com.example.data.remote.FirestorePresenceService.stopListening()
                    com.example.data.remote.FirestoreContactService.stopRealtimeSync()
                    com.example.data.remote.FirestoreGroupService.stopRealtimeGroupSync()
                    com.example.data.remote.FirestoreMessageService.stopListening()
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
    }

    override fun onCleared() {
        super.onCleared()
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().removeAuthStateListener(firebaseAuthListener)
        } catch (_: Throwable) {}
        com.example.data.remote.FirestorePresenceService.stopListening()
    }

    fun enqueueNotificationIntent(intent: Intent?) {
        if (intent == null) return
        handleIncomingShareIntent(intent)
        val hasNotifExtras = intent.hasExtra(com.example.notification.NotificationHelper.EXTRA_SCREEN) ||
            intent.hasExtra("extra_direct_reply_chat_id") ||
            intent.hasExtra("extra_accept_friend_email") ||
            intent.hasExtra("extra_decline_call") ||
            intent.action == com.example.notification.NotificationHelper.ACTION_ANSWER_CALL
        if (hasNotifExtras) {
            _pendingNotificationIntent.value = intent
        }
    }

    /**
     * Extracts shared text/URLs when Linko is chosen from the Android system Share Sheet
     * (Intent.ACTION_SEND with mimeType text/plain from apps like YouTube, Instagram, Chrome, etc.).
     */
    fun handleIncomingShareIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
                ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()?.trim()
                ?: ""
            val sharedSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT)?.trim()?.takeIf { it.isNotBlank() }
                ?: intent.getStringExtra(Intent.EXTRA_TITLE)?.trim()?.takeIf { it.isNotBlank() }

            if (sharedText.isNotBlank()) {
                _pendingSharedContent.value = SharedIncomingContent(
                    sharedText = sharedText,
                    sharedSubject = sharedSubject,
                    sourceMimeType = intent.type ?: "text/plain"
                )
                // Clear intent extras/action so configuration changes don't re-trigger the share sheet
                intent.removeExtra(Intent.EXTRA_TEXT)
                intent.removeExtra(Intent.EXTRA_SUBJECT)
                intent.action = null
            }
        }
    }

    fun clearPendingSharedContent() {
        _pendingSharedContent.value = null
    }

    fun sendSharedContentToRecipients(
        finalMessageText: String,
        selectedContacts: List<ContactEntity>,
        selectedChatIds: List<String>,
        onCompleteNavigateToChat: (String) -> Unit
    ) {
        val textToSend = finalMessageText.trim()
        if (textToSend.isBlank()) return
        val sender = currentUser.value ?: return

        viewModelScope.launch {
            val targetChatIds = mutableListOf<String>()

            // 1. Resolve or create unified direct chats for all selected contacts
            for (contact in selectedContacts) {
                val chat = repository.createOrGetDirectChat(sender, contact)
                if (chat.id !in targetChatIds) {
                    targetChatIds.add(chat.id)
                }
            }

            // 2. Include any directly selected chats (e.g., Groups, Gemini AI)
            for (chatId in selectedChatIds) {
                if (chatId !in targetChatIds) {
                    targetChatIds.add(chatId)
                }
            }

            // 3. Dispatch the shared message to every selected recipient chat
            for (chatId in targetChatIds) {
                repository.sendMessage(
                    chatId = chatId,
                    sender = sender,
                    text = textToSend
                )
            }

            _pendingSharedContent.value = null
            val firstChatId = targetChatIds.firstOrNull()
            if (firstChatId != null) {
                selectChat(firstChatId)
                val countLabel = if (targetChatIds.size == 1) "1 conversation" else "${targetChatIds.size} conversations"
                _actionStatusMessage.value = "Shared link/text sent to $countLabel!"
                onCompleteNavigateToChat(firstChatId)
            }
        }
    }

    fun consumeNotificationIntent() {
        _pendingNotificationIntent.value = null
    }

    fun refreshFcmToken(context: Context) {
        com.example.notification.LinkoFirebaseMessagingService.initializeFcmAndRegisterToken(
            context = context,
            userEmail = currentUser.value?.email
        ) { token ->
            _fcmToken.value = token
            _actionStatusMessage.value = "FCM Registration Token synced & active!"
        }
    }

    fun setAppForegroundState(isForeground: Boolean) {
        _isAppInForeground.value = isForeground
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

    fun getContactForChat(chat: ChatEntity?): ContactEntity? {
        if (chat == null || chat.isGroup) return null
        val me = currentUser.value
        val participantIds = try {
            if (chat.participantIdsJson.isBlank()) emptyList()
            else {
                val array = org.json.JSONArray(chat.participantIdsJson)
                (0 until array.length()).map { array.getString(it) }
            }
        } catch (_: Exception) {
            emptyList()
        }
        val myId = me?.id?.lowercase()?.trim() ?: ""
        val myEmail = me?.email?.lowercase()?.trim() ?: ""
        val myCanonical = if (myEmail.isNotBlank()) LinkoRepository.canonicalUserIdFromEmail(myEmail, myId) else myId

        val otherTokens = participantIds
            .map { it.lowercase().trim() }
            .filter { it.isNotBlank() && it != myId && it != myEmail && it != myCanonical }

        return contacts.value.find { c ->
            val cId = c.contactUserId.lowercase().trim()
            val cEmail = c.contactEmail.lowercase().trim()
            val cKey = LinkoRepository.canonicalParticipantKey(cEmail.ifBlank { cId })
            otherTokens.any { token ->
                token == cId || token == cEmail || LinkoRepository.canonicalParticipantKey(token) == cKey
            } ||
            LinkoRepository.generateDirectChatId(myId, myEmail, cId, cEmail) == chat.id ||
            "chat_${c.contactUserId}" == chat.id ||
            (!LinkoRepository.isGenericPlaceholderName(chat.name, me?.displayName, myEmail) &&
                (c.contactDisplayName.equals(chat.name.trim(), ignoreCase = true) ||
                    c.contactEmail.equals(chat.name.trim(), ignoreCase = true)))
        }
    }

    fun isChatOnline(chat: ChatEntity?): Boolean {
        if (chat == null) return false
        if (chat.isGroup) return false
        if (chat.id == "chat_gemini_ai" || chat.name.contains("Gemini")) return true

        val contact = getContactForChat(chat)
        return if (contact != null) isUserOnline(contact.contactEmail) else isUserOnline(chat.name)
    }

    fun loginOrRegister(email: String, displayName: String, username: String) {
        viewModelScope.launch {
            try {
                prefs.edit().remove("is_guest_mode").remove("guest_display_name").apply()
                repository.registerOrLogin(email, displayName, username)
                _actionStatusMessage.value = "Welcome back!"
            } catch (e: Exception) {
                _actionStatusMessage.value = e.message ?: "Authentication failed."
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            prefs.edit().remove("is_guest_mode").remove("guest_display_name").apply()
            repository.logout()
            _selectedChatId.value = null
        }
    }

    fun selectChat(chatId: String) {
        val prevChatId = _selectedChatId.value
        if (prevChatId != null && prevChatId != chatId) {
            repository.stopChatReceiptSync(prevChatId)
        }
        _selectedChatId.value = chatId
        val me = currentUser.value
        viewModelScope.launch {
            if (me != null) {
                repository.clearChatUnread(chatId)
                repository.startChatReceiptSync(chatId, me.id, me.email)
            } else {
                repository.clearChatUnread(chatId)
            }
        }
    }

    fun markVisibleMessagesAsRead(chatId: String, visibleMessageIds: List<String>) {
        if (visibleMessageIds.isEmpty()) return
        val me = currentUser.value ?: return
        viewModelScope.launch {
            repository.markVisibleMessagesAsRead(
                chatId = chatId,
                currentUserId = me.id,
                currentUserEmail = me.email,
                visibleMessageIds = visibleMessageIds
            )
        }
    }

    fun markChatMessagesAsRead(chatId: String) {
        val me = currentUser.value ?: return
        viewModelScope.launch {
            repository.markChatMessagesAsRead(chatId, me.id, me.email)
        }
    }

    fun clearSelectedChat() {
        val prevChatId = _selectedChatId.value
        if (prevChatId != null) {
            repository.stopChatReceiptSync(prevChatId)
        }
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
            selectChat(chat.id)
            onChatReady(chat.id)
        }
    }

    fun openOrCreateDirectChat(userId: String, email: String, name: String, onChatReady: () -> Unit) {
        viewModelScope.launch {
            val me = currentUser.value ?: return@launch
            val canonicalId = LinkoRepository.canonicalUserIdFromEmail(email, userId)
            var contact = contacts.value.find {
                it.contactUserId == userId ||
                it.contactUserId == canonicalId ||
                (email.isNotBlank() && it.contactEmail.equals(email, ignoreCase = true))
            }
            if (contact == null) {
                contact = ContactEntity(
                    id = "c_${LinkoRepository.canonicalParticipantKey(email.ifBlank { canonicalId })}",
                    userEmail = me.email.lowercase().trim(),
                    contactUserId = canonicalId,
                    contactEmail = email.lowercase().trim(),
                    contactDisplayName = name,
                    contactUsername = email.substringBefore("@")
                )
            }
            val chat = repository.createOrGetDirectChat(me, contact)
            selectChat(chat.id)
            onChatReady()
        }
    }

    fun openGeminiChat(onChatReady: (String) -> Unit) {
        viewModelScope.launch {
            val me = currentUser.value ?: return@launch
            val chat = repository.createOrGetGeminiChat(me)
            selectChat(chat.id)
            onChatReady(chat.id)
        }
    }

    fun createGroupChat(
        groupName: String,
        selectedContacts: List<ContactEntity>,
        groupDesc: String,
        channelType: String = "GROUP",
        onDone: (String) -> Unit
    ) {
        viewModelScope.launch {
            val me = currentUser.value ?: return@launch
            val group = repository.createGroupChat(me, groupName, selectedContacts, groupDesc, channelType)
            selectChat(group.id)
            val kindLabel = if (channelType.equals("CHANNEL", ignoreCase = true)) "Channel" else "Group"
            _actionStatusMessage.value = "$kindLabel '$groupName' created (${selectedContacts.size} invited)!"
            onDone(group.id)
        }
    }

    fun addGroupMembers(chatId: String, newContacts: List<ContactEntity>) {
        viewModelScope.launch {
            val me = currentUser.value ?: return@launch
            repository.addGroupMembers(chatId, newContacts, me)
            _actionStatusMessage.value = "Invited ${newContacts.size} contact(s) to conversation"
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

        // 2. Set active call state so user can interact and click Fechar Google Meet
        _activeCallState.value = ActiveCallState(
            contactName = contactName,
            contactEmail = contactEmail,
            contactAvatarUri = contactAvatar,
            callType = callType,
            isMuted = false,
            isSpeakerOn = true,
            isVideoEnabled = (callType == CallType.VIDEO),
            isFrontCamera = true,
            callDurationSeconds = 0,
            statusText = "Google Meet: $meetCode"
        )

        // 3. Start call duration timer
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _activeCallState.value = _activeCallState.value?.let {
                    it.copy(callDurationSeconds = it.callDurationSeconds + 1)
                } ?: break
            }
        }

        // 4. Add Call Log entry
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

        // 5. Launch Google Meet Intent
        try {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(meetUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            _actionStatusMessage.value = "Google Meet aberto: $meetCode"
        } catch (e: Exception) {
            _actionStatusMessage.value = "Google Meet Link: $meetUrl"
        }
    }

    fun fecharGoogleMeet(chatId: String? = null) {
        endCall()
        val targetChatId = chatId ?: _selectedChatId.value
        val user = currentUser.value
        if (targetChatId != null && user != null) {
            viewModelScope.launch {
                repository.sendMessage(
                    chatId = targetChatId,
                    sender = user,
                    text = "🔴 Google Meet finalizado / fechado por ${user.displayName}."
                )
            }
        }
        _actionStatusMessage.value = "Google Meet fechado com sucesso."
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

    fun answerIncomingCall(
        callerName: String,
        callerEmail: String,
        callerAvatar: String?,
        callType: CallType
    ) {
        com.example.notification.NotificationHelper.cancelNotification(
            getApplication(),
            com.example.notification.NotificationHelper.NOTIFICATION_ID_INCOMING_CALL
        )
        com.example.notification.NotificationHelper.cancelNotification(
            getApplication(),
            com.example.notification.NotificationHelper.NOTIFICATION_ID_MISSED_CALL
        )

        val signalingEngine = LinkoCallSignalingEngine.getInstance(getApplication())
        signalingEngine.initiateCall(
            peerEmail = callerEmail,
            peerName = callerName,
            callType = callType,
            onCallConnected = { _ ->
                _activeCallState.value = _activeCallState.value?.copy(
                    statusText = "Connected • E2EE Secured"
                )
            },
            onCallEnded = { _ ->
                callTimerJob?.cancel()
                _activeCallState.value = null
            }
        )

        _activeCallState.value = ActiveCallState(
            contactName = callerName,
            contactEmail = callerEmail,
            contactAvatarUri = callerAvatar,
            callType = callType,
            isMuted = false,
            isSpeakerOn = (callType == CallType.VIDEO),
            isVideoEnabled = (callType == CallType.VIDEO),
            isFrontCamera = true,
            callDurationSeconds = 0,
            statusText = "Connecting Encrypted Call..."
        )

        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _activeCallState.value = _activeCallState.value?.let {
                    it.copy(callDurationSeconds = it.callDurationSeconds + 1)
                } ?: break
            }
        }

        viewModelScope.launch {
            repository.addCallLog(
                contactName = callerName,
                contactEmail = callerEmail,
                contactAvatarUri = callerAvatar,
                callType = callType,
                isIncoming = true,
                isMissed = false,
                durationSeconds = 0
            )
        }
    }

    fun processNotificationIntent(
        intent: Intent,
        context: Context,
        onNavigate: (screen: String, chatId: String?) -> Unit
    ) {
        val extraScreen = intent.getStringExtra(com.example.notification.NotificationHelper.EXTRA_SCREEN)
        val extraChatId = intent.getStringExtra(com.example.notification.NotificationHelper.EXTRA_CHAT_ID)
        val extraSenderName = intent.getStringExtra(com.example.notification.NotificationHelper.EXTRA_SENDER_NAME) ?: ""
        val extraSenderEmail = intent.getStringExtra(com.example.notification.NotificationHelper.EXTRA_SENDER_EMAIL) ?: ""
        val extraMessageText = intent.getStringExtra(com.example.notification.NotificationHelper.EXTRA_MESSAGE_TEXT) ?: ""
        val extraIsGroup = intent.getBooleanExtra(com.example.notification.NotificationHelper.EXTRA_IS_GROUP, false)
        val extraReplyChatId = intent.getStringExtra("extra_direct_reply_chat_id")
        val extraReplyText = intent.getStringExtra("extra_direct_reply_text")
        val extraAcceptEmail = intent.getStringExtra("extra_accept_friend_email")
        val extraAcceptName = intent.getStringExtra("extra_accept_friend_name") ?: ""
        val extraDeclineCall = intent.getBooleanExtra("extra_decline_call", false)

        // Clear extras from intent so they aren't re-consumed on rotation
        intent.removeExtra(com.example.notification.NotificationHelper.EXTRA_SCREEN)
        intent.removeExtra("extra_direct_reply_chat_id")
        intent.removeExtra("extra_accept_friend_email")
        intent.removeExtra("extra_decline_call")

        // Process Direct Reply from Notification
        if (!extraReplyChatId.isNullOrEmpty() && !extraReplyText.isNullOrEmpty()) {
            val user = currentUser.value
            if (user != null) {
                viewModelScope.launch {
                    repository.sendMessage(extraReplyChatId, user, extraReplyText)
                    selectChat(extraReplyChatId)
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
                    _actionStatusMessage.value = "Accepted friend request from ${extraAcceptName.ifBlank { extraAcceptEmail }}!"
                    onNavigate(com.example.notification.NotificationHelper.SCREEN_FRIENDS, null)
                }
            } else {
                onNavigate(com.example.notification.NotificationHelper.SCREEN_FRIENDS, null)
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
                        viewModelScope.launch {
                            val ensuredChat = repository.ensureChatForNotification(
                                chatId = extraChatId,
                                senderName = extraSenderName,
                                senderEmail = extraSenderEmail,
                                messageText = extraMessageText,
                                isGroup = extraIsGroup,
                                currentUser = currentUser.value
                            )
                            selectChat(ensuredChat.id)
                            onNavigate(com.example.notification.NotificationHelper.SCREEN_DIRECT_CHAT, ensuredChat.id)
                        }
                    }
                }
                com.example.notification.NotificationHelper.SCREEN_INCOMING_CALL -> {
                    val callerName = intent.getStringExtra(com.example.notification.NotificationHelper.EXTRA_CALLER_NAME) ?: "Caller"
                    val callerEmail = intent.getStringExtra(com.example.notification.NotificationHelper.EXTRA_CALLER_EMAIL) ?: "caller@linko.com"
                    val callTypeStr = intent.getStringExtra(com.example.notification.NotificationHelper.EXTRA_CALL_TYPE) ?: "VOICE"
                    val callType = if (callTypeStr.equals("VIDEO", ignoreCase = true)) CallType.VIDEO else CallType.AUDIO
                    val contactAvatar = contacts.value.find { it.contactEmail.equals(callerEmail, ignoreCase = true) }?.contactAvatarUri
                    answerIncomingCall(callerName, callerEmail, contactAvatar, callType)
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
            val chatId = firstChat?.id ?: "chat_elena_vance"
            val senderName = firstChat?.name ?: "Elena Vance"
            val text = "Hey! Tap this notification to open our chat or reply directly from the notification bar 🚀"

            repository.ensureChatForNotification(
                chatId = chatId,
                senderName = senderName,
                senderEmail = "elena.vance@linko.com",
                messageText = text,
                isGroup = firstChat?.isGroup ?: false,
                currentUser = currentUser.value
            )

            com.example.notification.LinkoFirebaseMessagingService.dispatchRemotePayload(
                context = context,
                data = mapOf(
                    "type" to "message",
                    "chat_id" to chatId,
                    "sender_name" to senderName,
                    "sender_email" to "elena.vance@linko.com",
                    "message_text" to text,
                    "is_group" to (firstChat?.isGroup ?: false).toString()
                )
            )
            _actionStatusMessage.value = "FCM Push notification sent for Message from $senderName!"
        }
    }

    fun triggerSimulatedCallNotification(context: Context, isVideo: Boolean = false) {
        val firstContact = contacts.value.firstOrNull()
        val callerName = firstContact?.contactDisplayName ?: "Marcus Miller"
        val callerEmail = firstContact?.contactEmail ?: "marcus.miller@linko.com"
        val callType = if (isVideo) "Video" else "Voice"

        com.example.notification.LinkoFirebaseMessagingService.dispatchRemotePayload(
            context = context,
            data = mapOf(
                "type" to "incoming_call",
                "caller_name" to callerName,
                "caller_email" to callerEmail,
                "call_type" to callType
            )
        )
        _actionStatusMessage.value = "FCM Push notification sent for Incoming $callType Call from $callerName!"
    }

    fun triggerSimulatedFriendRequestNotification(context: Context) {
        val requesterName = "Sophia Chen"
        val requesterEmail = "sophia.chen@linko.com"

        com.example.notification.LinkoFirebaseMessagingService.dispatchRemotePayload(
            context = context,
            data = mapOf(
                "type" to "friend_request",
                "requester_name" to requesterName,
                "requester_email" to requesterEmail
            )
        )
        _actionStatusMessage.value = "FCM Push notification sent for Friend Request from $requesterName!"
    }

}

data class SharedIncomingContent(
    val sharedText: String,
    val sharedSubject: String? = null,
    val sourceMimeType: String = "text/plain"
)

