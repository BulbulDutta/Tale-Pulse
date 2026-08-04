package com.example.notification

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class LinkoFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM Token refreshed: $token")

        // Persist token in SharedPreferences for local retrieval
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val type = data["type"]
            ?: data["notif_type"]
            ?: data["category"]
            ?: notification?.title
            ?: ""

        when {
            // New Chat Message Payload
            type.contains("message", ignoreCase = true) || type == ScheduledNotificationReceiver.TYPE_MESSAGE -> {
                val chatId = data["chat_id"] ?: data["chatId"] ?: "chat_1"
                val senderName = data["sender_name"] ?: data["sender"] ?: notification?.title ?: "Linko Contact"
                val messageText = data["message_text"] ?: data["message"] ?: notification?.body ?: "New message received on Linko"

                NotificationHelper.showNewMessageNotification(
                    context = applicationContext,
                    chatId = chatId,
                    senderName = senderName,
                    messageText = messageText
                )
            }
            // Incoming Call Payload (Voice or Video)
            type.contains("call", ignoreCase = true) || type == ScheduledNotificationReceiver.TYPE_CALL -> {
                val callerName = data["caller_name"] ?: data["caller"] ?: notification?.title ?: "Marcus Miller"
                val callerEmail = data["caller_email"] ?: data["email"] ?: "marcus@linko.com"
                val callType = data["call_type"] ?: data["callType"] ?: "Voice"

                NotificationHelper.showIncomingCallNotification(
                    context = applicationContext,
                    callerName = callerName,
                    callerEmail = callerEmail,
                    callType = callType
                )
            }
            // Friend Request Payload
            type.contains("friend", ignoreCase = true) || type == ScheduledNotificationReceiver.TYPE_FRIEND_REQUEST -> {
                val requesterName = data["requester_name"] ?: data["requester"] ?: notification?.title ?: "Sophia Chen"
                val requesterEmail = data["requester_email"] ?: data["email"] ?: "sophia@linko.com"

                NotificationHelper.showFriendRequestNotification(
                    context = applicationContext,
                    requesterName = requesterName,
                    requesterEmail = requesterEmail
                )
            }
            // Default Fallback Notification
            else -> {
                val title = notification?.title ?: data["title"] ?: "Linko Alert"
                val body = notification?.body ?: data["body"] ?: "You have a new update on Linko"
                val chatId = data["chat_id"] ?: data["chatId"] ?: "chat_1"

                NotificationHelper.showNewMessageNotification(
                    context = applicationContext,
                    chatId = chatId,
                    senderName = title,
                    messageText = body
                )
            }
        }
    }

    companion object {
        private const val TAG = "LinkoFCM"
        private const val PREFS_NAME = "linko_fcm_prefs"
        private const val KEY_FCM_TOKEN = "fcm_token"

        fun getSavedToken(context: Context): String? {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_FCM_TOKEN, null)
        }
    }
}
