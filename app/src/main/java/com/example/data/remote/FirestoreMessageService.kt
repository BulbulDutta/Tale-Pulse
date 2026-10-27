package com.example.data.remote

import android.util.Log
import com.example.data.local.entity.MessageEntity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Service managing message status synchronization ('SENT', 'DELIVERED', 'READ')
 * using Cloud Firestore timestamp updates (FieldValue.serverTimestamp()).
 */
object FirestoreMessageService {
    private const val TAG = "FirestoreMessageService"
    private const val COLLECTION_MESSAGES = "chat_messages"

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseFirestore not initialized: ${e.message}")
            null
        }
    }

    private val activeChatListeners = mutableMapOf<String, ListenerRegistration>()

    /**
     * Upload newly dispatched message to Firestore with serverTimestamp for sentAt.
     */
    fun uploadMessage(
        message: MessageEntity,
        participants: List<String> = emptyList()
    ) {
        val db = getFirestore() ?: return
        try {
            val payload = hashMapOf<String, Any?>(
                "id" to message.id,
                "chatId" to message.chatId,
                "senderId" to message.senderId,
                "senderName" to message.senderName,
                "senderEmail" to message.senderEmail.lowercase().trim(),
                "text" to message.text,
                "mediaUri" to message.mediaUri,
                "mediaType" to message.mediaType,
                "formattedRichText" to message.formattedRichText,
                "status" to message.status,
                "clientTimestamp" to message.timestamp,
                "sentAt" to FieldValue.serverTimestamp(),
                "deliveredAt" to if (message.deliveredAt != null) com.google.firebase.Timestamp(java.util.Date(message.deliveredAt)) else null,
                "readAt" to if (message.readAt != null) com.google.firebase.Timestamp(java.util.Date(message.readAt)) else null,
                "emailTransportStatus" to message.emailTransportStatus,
                "emailSubject" to message.emailSubject,
                "emailMessageId" to message.emailMessageId,
                "isEncrypted" to message.isEncrypted,
                "participants" to participants
            )

            db.collection(COLLECTION_MESSAGES)
                .document(message.id)
                .set(payload, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Uploaded message ${message.id} to Firestore (Status: ${message.status})")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed uploading message ${message.id} to Firestore: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in uploadMessage: ${e.message}")
        }
    }

    /**
     * Mark message as DELIVERED using Firestore FieldValue.serverTimestamp().
     */
    fun markMessageDelivered(
        messageId: String,
        onComplete: ((deliveredAt: Long) -> Unit)? = null
    ) {
        val db = getFirestore() ?: return
        try {
            val docRef = db.collection(COLLECTION_MESSAGES).document(messageId)
            val updates = mapOf<String, Any>(
                "status" to "DELIVERED",
                "deliveredAt" to FieldValue.serverTimestamp()
            )

            docRef.update(updates)
                .addOnSuccessListener {
                    Log.d(TAG, "Message $messageId marked DELIVERED with server timestamp")
                    docRef.get().addOnSuccessListener { snapshot ->
                        val timestamp = snapshot.getTimestamp("deliveredAt")?.toDate()?.time
                            ?: System.currentTimeMillis()
                        onComplete?.invoke(timestamp)
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Direct update failed, merging status for $messageId: ${e.message}")
                    docRef.set(updates, SetOptions.merge())
                        .addOnSuccessListener {
                            onComplete?.invoke(System.currentTimeMillis())
                        }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in markMessageDelivered: ${e.message}")
        }
    }

    /**
     * Mark a single message as READ using Firestore FieldValue.serverTimestamp().
     */
    fun markMessageRead(
        messageId: String,
        onComplete: ((readAt: Long) -> Unit)? = null
    ) {
        val db = getFirestore() ?: return
        try {
            val docRef = db.collection(COLLECTION_MESSAGES).document(messageId)
            val updates = mapOf<String, Any>(
                "status" to "READ",
                "readAt" to FieldValue.serverTimestamp()
            )

            docRef.update(updates)
                .addOnSuccessListener {
                    Log.d(TAG, "Message $messageId marked READ with server timestamp")
                    docRef.get().addOnSuccessListener { snapshot ->
                        val timestamp = snapshot.getTimestamp("readAt")?.toDate()?.time
                            ?: System.currentTimeMillis()
                        onComplete?.invoke(timestamp)
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Direct update failed, merging read status for $messageId: ${e.message}")
                    docRef.set(updates, SetOptions.merge())
                        .addOnSuccessListener {
                            onComplete?.invoke(System.currentTimeMillis())
                        }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in markMessageRead: ${e.message}")
        }
    }

    /**
     * Mark unread messages in a chat as READ using Firestore FieldValue.serverTimestamp().
     */
    fun markChatMessagesRead(
        chatId: String,
        currentUserId: String,
        currentUserEmail: String = "",
        onComplete: ((readAt: Long) -> Unit)? = null
    ) {
        val db = getFirestore() ?: return
        try {
            db.collection(COLLECTION_MESSAGES)
                .whereEqualTo("chatId", chatId)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot == null || snapshot.isEmpty) {
                        onComplete?.invoke(System.currentTimeMillis())
                        return@addOnSuccessListener
                    }

                    val normalizedEmail = currentUserEmail.lowercase().trim()
                    val batch = db.batch()
                    var batchCount = 0

                    for (doc in snapshot.documents) {
                        val senderId = doc.getString("senderId") ?: ""
                        val senderEmail = doc.getString("senderEmail")?.lowercase()?.trim() ?: ""
                        val status = doc.getString("status") ?: ""

                        // Only mark as read messages sent by others that are not already READ
                        val isFromOthers = (senderId.isNotBlank() && senderId != currentUserId) ||
                                (normalizedEmail.isNotBlank() && senderEmail != normalizedEmail)

                        if (isFromOthers && status != "READ") {
                            batch.update(
                                doc.reference,
                                mapOf(
                                    "status" to "READ",
                                    "readAt" to FieldValue.serverTimestamp()
                                )
                            )
                            batchCount++
                        }
                    }

                    if (batchCount > 0) {
                        batch.commit()
                            .addOnSuccessListener {
                                Log.d(TAG, "Batch marked $batchCount messages as READ in Firestore for $chatId")
                                onComplete?.invoke(System.currentTimeMillis())
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Batch commit failed for chat $chatId: ${e.message}")
                                onComplete?.invoke(System.currentTimeMillis())
                            }
                    } else {
                        onComplete?.invoke(System.currentTimeMillis())
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to query chat messages for marking read: ${e.message}")
                    onComplete?.invoke(System.currentTimeMillis())
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in markChatMessagesRead: ${e.message}")
            onComplete?.invoke(System.currentTimeMillis())
        }
    }

    /**
     * Listen in real-time to Firestore message receipt updates (deliveredAt, readAt, status) for a specific chat.
     */
    fun startListeningToChatReceipts(
        chatId: String,
        currentUserId: String,
        scope: CoroutineScope,
        onReceiptUpdated: suspend (messageId: String, status: String, deliveredAt: Long?, readAt: Long?) -> Unit
    ) {
        stopListening(chatId)
        val db = getFirestore() ?: return

        try {
            val listener = db.collection(COLLECTION_MESSAGES)
                .whereEqualTo("chatId", chatId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Chat receipts listener error for $chatId: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        for (doc in snapshot.documents) {
                            val msgId = doc.getString("id") ?: doc.id
                            val status = doc.getString("status") ?: "SENT"
                            val deliveredAt = doc.getTimestamp("deliveredAt")?.toDate()?.time
                                ?: doc.getLong("deliveredAt")
                            val readAt = doc.getTimestamp("readAt")?.toDate()?.time
                                ?: doc.getLong("readAt")

                            val senderId = doc.getString("senderId") ?: ""
                            // If this message was sent to me and not yet marked delivered in Firestore, auto-mark it delivered
                            if (senderId.isNotBlank() && senderId != currentUserId && deliveredAt == null && status == "SENT") {
                                markMessageDelivered(msgId)
                            }

                            scope.launch(Dispatchers.IO) {
                                onReceiptUpdated(msgId, status, deliveredAt, readAt)
                            }
                        }
                    }
                }
            activeChatListeners[chatId] = listener
        } catch (e: Exception) {
            Log.e(TAG, "Exception in startListeningToChatReceipts: ${e.message}")
        }
    }

    /**
     * Stop active snapshot listener for a chat.
     */
    fun stopListening(chatId: String? = null) {
        if (chatId != null) {
            activeChatListeners.remove(chatId)?.remove()
        } else {
            activeChatListeners.values.forEach { it.remove() }
            activeChatListeners.clear()
        }
    }
}
