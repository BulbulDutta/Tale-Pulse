package com.example.data.repository

import com.example.data.crypto.EncryptionManager
import com.example.data.local.dao.CallLogDao
import com.example.data.local.dao.ChatDao
import com.example.data.local.dao.ContactDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.dao.StatusDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.CallLogEntity
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.StatusEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.CallType
import com.example.data.remote.FirestoreContactService
import com.example.data.remote.GeminiClient
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class TalePulseRepository(
    private val userDao: UserDao,
    private val contactDao: ContactDao,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val callLogDao: CallLogDao,
    private val statusDao: StatusDao
) {
    private val ioScope = CoroutineScope(Dispatchers.IO)

    val currentUserFlow: Flow<UserEntity?> = userDao.getCurrentUserFlow()
    val allChatsFlow: Flow<List<ChatEntity>> = chatDao.getAllChatsFlow()
    val allCallLogsFlow: Flow<List<CallLogEntity>> = callLogDao.getAllCallLogsFlow()
    val activeStatusesFlow: Flow<List<StatusEntity>> = statusDao.getActiveStatusesFlow(System.currentTimeMillis())


    fun getContactsFlow(ownerEmail: String): Flow<List<ContactEntity>> {
        return contactDao.getContactsFlow(ownerEmail)
    }

    fun getMessagesFlow(chatId: String): Flow<List<MessageEntity>> {
        return messageDao.getMessagesForChatFlow(chatId).map { list ->
            list.map { msg ->
                val plain = EncryptionManager.decrypt(msg.text, chatId)
                msg.copy(text = plain)
            }
        }
    }

    fun getChatByIdFlow(chatId: String): Flow<ChatEntity?> {
        return chatDao.getChatByIdFlow(chatId)
    }

    suspend fun seedSampleDataIfEmpty() {
        // Production mode: Database is clean and empty by default on first install.
        // User chats, contacts, and messages are only populated via authentication, QR code scanning, or Firestore sync.
    }

    suspend fun postStatus(
        user: UserEntity,
        type: String,
        textContent: String?,
        mediaUri: String? = null,
        backgroundColorHex: String = "#10B981",
        fontStyle: String = "DEFAULT"
    ): StatusEntity {
        val now = System.currentTimeMillis()
        val newStatus = StatusEntity(
            id = "status_" + UUID.randomUUID().toString().take(8),
            userId = user.id,
            userDisplayName = user.displayName,
            userEmail = user.email,
            userAvatarUri = user.avatarUri,
            type = type,
            textContent = textContent,
            mediaUri = mediaUri,
            backgroundColorHex = backgroundColorHex,
            fontStyle = fontStyle,
            createdTimestamp = now,
            expiresTimestamp = now + (24 * 60 * 60 * 1000L),
            isViewed = false
        )
        statusDao.insertStatus(newStatus)
        return newStatus
    }

    suspend fun markStatusAsViewed(statusId: String) {
        statusDao.markStatusAsViewed(statusId)
    }

    suspend fun deleteStatus(statusId: String) {
        statusDao.deleteStatusById(statusId)
    }

    suspend fun cleanExpiredStatuses() {
        statusDao.deleteExpiredStatuses(System.currentTimeMillis())
    }


    suspend fun createOrGetGeminiChat(me: UserEntity): ChatEntity {
        val existing = chatDao.getChatById("chat_gemini_ai")
        if (existing != null) return existing

        val geminiContact = ContactEntity(
            id = "c_gemini",
            userEmail = me.email,
            contactUserId = "user_gemini",
            contactEmail = "gemini.ai@talepulse.com",
            contactDisplayName = "Gemini AI Assistant",
            contactUsername = "gemini_ai",
            contactStatus = "Powered by Gemini 3.5 Flash • Ask me anything! ✨"
        )
        contactDao.insertContact(geminiContact)

        val newGeminiChat = ChatEntity(
            id = "chat_gemini_ai",
            isGroup = false,
            name = "Gemini AI Assistant",
            participantIdsJson = "[\"${me.id}\", \"user_gemini\"]",
            lastMessageText = "Hello! I'm Gemini, your AI assistant on Tale Pulse. Ask me anything! ✨",
            lastMessageTimestamp = System.currentTimeMillis()
        )
        chatDao.insertOrUpdateChat(newGeminiChat)

        val welcomeText = "Hello! 👋 I'm your Gemini AI Assistant on Tale Pulse.\n\nAsk me questions, draft messages, analyze attached images, or summarize conversations! ✨"
        val welcomeMessage = MessageEntity(
            id = "m_gemini_welcome",
            chatId = "chat_gemini_ai",
            senderId = "user_gemini",
            senderName = "Gemini AI Assistant",
            senderEmail = "gemini.ai@talepulse.com",
            text = EncryptionManager.encrypt(welcomeText, "chat_gemini_ai"),
            timestamp = System.currentTimeMillis(),
            status = "READ",
            emailTransportStatus = "DELIVERED_INBOX",
            emailSubject = "Welcome to Gemini AI on Tale Pulse",
            emailMessageId = "<gemini-welcome@talepulse.net>",
            isEncrypted = true,
            encryptionAlgorithm = "AES-256-GCM"
        )
        messageDao.insertMessage(welcomeMessage)

        return newGeminiChat
    }

    suspend fun registerOrLogin(email: String, displayName: String, username: String): UserEntity {
        userDao.clearCurrentUser()
        val existing = userDao.getUserByEmail(email)
        val user = if (existing != null) {
            val updated = existing.copy(
                displayName = if (displayName.isNotBlank()) displayName else existing.displayName,
                username = if (username.isNotBlank()) username else existing.username,
                isCurrentUser = true
            )
            userDao.updateUser(updated)
            updated
        } else {
            val newUserId = "user_${UUID.randomUUID().toString().take(8)}"
            val newUser = UserEntity(
                id = newUserId,
                email = email.trim(),
                username = if (username.isBlank()) email.substringBefore("@") else username.trim(),
                displayName = if (displayName.isBlank()) email.substringBefore("@") else displayName.trim(),
                avatarUri = null,
                qrPayload = "talepulse://user?email=${email.trim()}&id=$newUserId",
                statusMessage = "Hey! I am using Tale Pulse.",
                isCurrentUser = true
            )
            userDao.insertUser(newUser)
            newUser
        }
        syncContactsWithFirestore(user.email)
        return user
    }

    suspend fun logout() {
        FirestoreContactService.stopRealtimeSync()
        userDao.clearCurrentUser()
    }

    suspend fun addContactByEmail(ownerEmail: String, emailInput: String): Result<ContactEntity> {
        val trimmed = emailInput.trim()
        if (trimmed.isEmpty() || !trimmed.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }

        val existing = contactDao.getContactByEmail(ownerEmail, trimmed)
        if (existing != null) {
            FirestoreContactService.uploadContact(existing)
            return Result.success(existing)
        }

        val contactName = trimmed.substringBefore("@").replace(".", " ").capitalizeWords()
        val newContact = ContactEntity(
            id = "c_${UUID.randomUUID().toString().take(8)}",
            userEmail = ownerEmail,
            contactUserId = "user_${UUID.randomUUID().toString().take(8)}",
            contactEmail = trimmed,
            contactDisplayName = contactName,
            contactUsername = trimmed.substringBefore("@"),
            contactStatus = "Added via Email Connection"
        )
        contactDao.insertContact(newContact)
        FirestoreContactService.uploadContact(newContact)
        return Result.success(newContact)
    }

    suspend fun addContactFromQrPayload(ownerEmail: String, qrPayload: String): Result<ContactEntity> {
        var email = ""
        var qrUserId = ""
        var displayName = ""

        val trimmed = qrPayload.trim()

        if (trimmed.startsWith("talepulse://user") || trimmed.contains("email=")) {
            email = trimmed.substringAfter("email=", "").substringBefore("&")
            qrUserId = trimmed.substringAfter("id=", "").substringBefore("&")
            displayName = trimmed.substringAfter("name=", "").substringBefore("&")
        } else if (trimmed.startsWith("{")) {
            try {
                val json = org.json.JSONObject(trimmed)
                email = json.optString("email", "")
                qrUserId = json.optString("id", "")
                displayName = json.optString("displayName", json.optString("name", ""))
            } catch (_: Exception) {}
        } else if (trimmed.contains("@")) {
            email = trimmed
        }

        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException("Could not extract contact email from QR Code payload."))
        }

        val result = addContactByEmail(ownerEmail, email)
        result.onSuccess { contact ->
            val updated = contact.copy(
                contactUserId = if (qrUserId.isNotBlank()) qrUserId else contact.contactUserId,
                contactDisplayName = if (displayName.isNotBlank()) displayName else contact.contactDisplayName,
                contactStatus = "Scanned & Paired via QR Code 📷"
            )
            contactDao.insertContact(updated)
            FirestoreContactService.uploadContact(updated)
        }
        return result
    }

    fun syncContactsWithFirestore(ownerEmail: String) {
        if (ownerEmail.isBlank()) return

        // 1. Fetch existing contacts from Firestore and sync to Room
        FirestoreContactService.fetchAndSyncContacts(ownerEmail, ioScope) { remoteContacts ->
            remoteContacts.forEach { contactDao.insertContact(it) }
        }

        // 2. Start real-time snapshot listener
        FirestoreContactService.startRealtimeContactSync(ownerEmail, ioScope) { liveContacts ->
            liveContacts.forEach { contactDao.insertContact(it) }
        }

        // 3. Upload local contacts to Firestore
        ioScope.launch {
            val localContacts = contactDao.getContactsList(ownerEmail)
            FirestoreContactService.uploadContactsBatch(localContacts)
        }
    }

    suspend fun createOrGetDirectChat(me: UserEntity, contact: ContactEntity): ChatEntity {
        val chatId = "chat_${contact.contactUserId}"
        val existing = chatDao.getChatById(chatId)
        if (existing != null) return existing

        val newChat = ChatEntity(
            id = chatId,
            isGroup = false,
            name = contact.contactDisplayName,
            participantIdsJson = "[\"${me.id}\", \"${contact.contactUserId}\"]",
            lastMessageText = "Chat started",
            lastMessageTimestamp = System.currentTimeMillis()
        )
        chatDao.insertOrUpdateChat(newChat)
        return newChat
    }

    suspend fun createGroupChat(me: UserEntity, groupName: String, selectedContacts: List<ContactEntity>, groupDesc: String?): ChatEntity {
        val groupId = "group_${UUID.randomUUID().toString().take(8)}"
        val participantIds = mutableListOf(me.id)
        selectedContacts.forEach { participantIds.add(it.contactUserId) }
        val idsJson = participantIds.joinToString(prefix = "[\"", postfix = "\"]", separator = "\",\"")

        val newGroup = ChatEntity(
            id = groupId,
            isGroup = true,
            name = groupName.ifBlank { "Custom Group" },
            groupDescription = groupDesc ?: "Group created by ${me.displayName}",
            participantIdsJson = idsJson,
            lastMessageText = "${me.displayName} created group \"$groupName\"",
            lastMessageTimestamp = System.currentTimeMillis()
        )
        chatDao.insertOrUpdateChat(newGroup)

        // System message inside group
        val systemMessage = MessageEntity(
            id = "m_${UUID.randomUUID().toString().take(8)}",
            chatId = groupId,
            senderId = me.id,
            senderName = me.displayName,
            senderEmail = me.email,
            text = "🎉 Welcome to $groupName! Group chat created and email notifications active.",
            timestamp = System.currentTimeMillis(),
            status = "READ",
            emailTransportStatus = "DELIVERED_INBOX",
            emailSubject = "TalePulse Group: $groupName",
            emailMessageId = "<group-${UUID.randomUUID().toString().take(6)}@talepulse.net>"
        )
        messageDao.insertMessage(systemMessage)

        return newGroup
    }

    suspend fun sendMessage(
        chatId: String,
        sender: UserEntity,
        text: String,
        mediaUri: String? = null,
        mediaType: String? = null,
        formattedRichText: String? = null
    ) {
        val msgId = "m_${UUID.randomUUID().toString().take(8)}"
        val chat = chatDao.getChatById(chatId)
        val subject = if (chat?.isGroup == true) "Group [${chat.name}]: ${text.take(25)}" else "TalePulse Message from ${sender.displayName}"
        val emailMsgId = "<msg-${UUID.randomUUID().toString().take(8)}@talepulse.net>"

        val encryptedText = EncryptionManager.encrypt(text, chatId)

        val message = MessageEntity(
            id = msgId,
            chatId = chatId,
            senderId = sender.id,
            senderName = sender.displayName,
            senderEmail = sender.email,
            text = encryptedText,
            mediaUri = mediaUri,
            mediaType = mediaType,
            formattedRichText = formattedRichText,
            timestamp = System.currentTimeMillis(),
            status = "SENT",
            emailTransportStatus = "DISPATCHED_SMTP",
            emailSubject = subject,
            emailMessageId = emailMsgId,
            isEncrypted = true,
            encryptionAlgorithm = "AES-256-GCM"
        )

        messageDao.insertMessage(message)
        val previewText = when {
            mediaType == "IMAGE" -> "📷 Photo"
            mediaType == "AUDIO" -> "🎙️ Voice note (${text.ifBlank { "0:05" }})"
            mediaType == "DOCUMENT" -> "📄 Attachment"
            else -> text
        }
        chatDao.updateLastMessage(chatId, previewText, System.currentTimeMillis())

        // Handle delivery & replies in background
        ioScope.launch {
            delay(800)
            messageDao.updateMessageStatus(msgId, "DELIVERED")
            delay(1000)
            messageDao.updateMessageStatus(msgId, "READ")
            messageDao.updateEmailTransportStatus(msgId, "DELIVERED_INBOX")

            if (chatId == "chat_gemini_ai" || chat?.name?.contains("Gemini") == true) {
                val promptText = if (text.isNotBlank()) text else "Analyze the attached item."
                val aiResponse = GeminiClient.generateResponse(promptText)

                val replyMsgId = "m_gemini_${UUID.randomUUID().toString().take(8)}"
                val encryptedAiResponse = EncryptionManager.encrypt(aiResponse, chatId)
                val replyMsg = MessageEntity(
                    id = replyMsgId,
                    chatId = chatId,
                    senderId = "user_gemini",
                    senderName = "Gemini AI Assistant",
                    senderEmail = "gemini.ai@talepulse.com",
                    text = encryptedAiResponse,
                    timestamp = System.currentTimeMillis(),
                    status = "READ",
                    emailTransportStatus = "DELIVERED_INBOX",
                    emailSubject = "Gemini AI Response",
                    emailMessageId = "<gemini-${UUID.randomUUID().toString().take(6)}@talepulse.net>",
                    isEncrypted = true,
                    encryptionAlgorithm = "AES-256-GCM"
                )
                messageDao.insertMessage(replyMsg)
                chatDao.updateLastMessage(chatId, aiResponse.take(60), System.currentTimeMillis())
            }
        }
    }

    suspend fun clearChatUnread(chatId: String) {
        chatDao.clearUnreadCount(chatId)
    }

    suspend fun addCallLog(
        contactName: String,
        contactEmail: String,
        contactAvatarUri: String?,
        callType: CallType,
        isIncoming: Boolean,
        isMissed: Boolean,
        durationSeconds: Int
    ) {
        val callLog = CallLogEntity(
            id = "call_${UUID.randomUUID().toString().take(8)}",
            contactName = contactName,
            contactEmail = contactEmail,
            contactAvatarUri = contactAvatarUri,
            callType = callType.name,
            isIncoming = isIncoming,
            isMissed = isMissed,
            timestamp = System.currentTimeMillis(),
            durationSeconds = durationSeconds
        )
        callLogDao.insertCallLog(callLog)
    }

    suspend fun updateProfile(updatedUser: UserEntity) {
        userDao.updateUser(updatedUser)
    }

    suspend fun votePoll(messageId: String, optionIndex: Int, userId: String) {
        val message = messageDao.getMessageById(messageId) ?: return
        val plainText = EncryptionManager.decrypt(message.text, message.chatId)
        try {
            val json = JSONObject(plainText)
            val optionsArray = json.getJSONArray("options")
            val allowMultiple = json.optBoolean("allowMultiple", false)

            for (i in 0 until optionsArray.length()) {
                val optObj = optionsArray.getJSONObject(i)
                val votesArray = optObj.optJSONArray("votes") ?: JSONArray()
                val votesList = mutableListOf<String>()
                for (j in 0 until votesArray.length()) {
                    votesList.add(votesArray.getString(j))
                }

                if (i == optionIndex) {
                    if (votesList.contains(userId)) {
                        votesList.remove(userId)
                    } else {
                        votesList.add(userId)
                    }
                } else if (!allowMultiple) {
                    votesList.remove(userId)
                }

                optObj.put("votes", JSONArray(votesList))
            }

            val updatedText = json.toString()
            val encryptedUpdatedText = EncryptionManager.encrypt(updatedText, message.chatId)
            val updatedMessage = message.copy(text = encryptedUpdatedText)
            messageDao.insertMessage(updatedMessage)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun sendPollMessage(
        chatId: String,
        sender: UserEntity,
        question: String,
        options: List<String>,
        allowMultiple: Boolean
    ) {
        val optionsArray = JSONArray()
        options.forEachIndexed { index, opt ->
            val optObj = JSONObject()
            optObj.put("id", index)
            optObj.put("text", opt)
            optObj.put("votes", JSONArray())
            optionsArray.put(optObj)
        }

        val pollObj = JSONObject()
        pollObj.put("question", question)
        pollObj.put("allowMultiple", allowMultiple)
        pollObj.put("options", optionsArray)

        sendMessage(
            chatId = chatId,
            sender = sender,
            text = pollObj.toString(),
            mediaType = "POLL"
        )
    }

    suspend fun sendLocationMessage(
        chatId: String,
        sender: UserEntity,
        title: String,
        address: String,
        latitude: Double,
        longitude: Double
    ) {
        val locObj = JSONObject()
        locObj.put("title", title)
        locObj.put("address", address)
        locObj.put("latitude", latitude)
        locObj.put("longitude", longitude)

        sendMessage(
            chatId = chatId,
            sender = sender,
            text = locObj.toString(),
            mediaType = "LOCATION"
        )
    }

    suspend fun sendContactMessage(
        chatId: String,
        sender: UserEntity,
        contactName: String,
        email: String,
        phone: String
    ) {
        val cObj = JSONObject()
        cObj.put("name", contactName)
        cObj.put("email", email)
        cObj.put("phone", phone)

        sendMessage(
            chatId = chatId,
            sender = sender,
            text = cObj.toString(),
            mediaType = "CONTACT"
        )
    }

    suspend fun sendEventMessage(
        chatId: String,
        sender: UserEntity,
        title: String,
        dateText: String,
        locationText: String
    ) {
        val evObj = JSONObject()
        evObj.put("title", title)
        evObj.put("dateText", dateText)
        evObj.put("location", locationText)

        sendMessage(
            chatId = chatId,
            sender = sender,
            text = evObj.toString(),
            mediaType = "EVENT"
        )
    }
}

private fun String.capitalizeWords(): String {
    return split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}
