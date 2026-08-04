package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class ScheduledNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(EXTRA_NOTIF_TYPE) ?: TYPE_MESSAGE

        when (type) {
            TYPE_MESSAGE -> {
                val chatId = intent.getStringExtra(NotificationHelper.EXTRA_CHAT_ID) ?: "chat_1"
                val senderName = intent.getStringExtra(EXTRA_SENDER_NAME) ?: "Elena Vance"
                val text = intent.getStringExtra(EXTRA_MESSAGE_TEXT)
                    ?: "Hey! Here is a message update from Tale Pulse 🌟"

                NotificationHelper.showNewMessageNotification(
                    context = context,
                    chatId = chatId,
                    senderName = senderName,
                    messageText = text
                )
            }
            TYPE_CALL -> {
                val callerName = intent.getStringExtra(NotificationHelper.EXTRA_CALLER_NAME) ?: "Marcus Miller"
                val callerEmail = intent.getStringExtra(NotificationHelper.EXTRA_CALLER_EMAIL) ?: "marcus@talepulse.com"
                val callType = intent.getStringExtra(NotificationHelper.EXTRA_CALL_TYPE) ?: "Voice"

                NotificationHelper.showIncomingCallNotification(
                    context = context,
                    callerName = callerName,
                    callerEmail = callerEmail,
                    callType = callType
                )
            }
            TYPE_FRIEND_REQUEST -> {
                val requesterName = intent.getStringExtra(NotificationHelper.EXTRA_REQUESTER_NAME) ?: "Sophia Chen"
                val requesterEmail = intent.getStringExtra(NotificationHelper.EXTRA_REQUESTER_EMAIL) ?: "sophia@talepulse.com"

                NotificationHelper.showFriendRequestNotification(
                    context = context,
                    requesterName = requesterName,
                    requesterEmail = requesterEmail
                )
            }
        }
    }

    companion object {
        const val EXTRA_NOTIF_TYPE = "extra_notif_type"
        const val EXTRA_SENDER_NAME = "extra_sender_name"
        const val EXTRA_MESSAGE_TEXT = "extra_message_text"

        const val TYPE_MESSAGE = "TYPE_MESSAGE"
        const val TYPE_CALL = "TYPE_CALL"
        const val TYPE_FRIEND_REQUEST = "TYPE_FRIEND_REQUEST"

        fun scheduleBackgroundPush(
            context: Context,
            type: String,
            delaySeconds: Long = 5,
            chatId: String = "chat_1",
            senderOrCallerName: String? = null,
            messageText: String? = null,
            isVideoCall: Boolean = false
        ) {
            val intent = Intent(context, ScheduledNotificationReceiver::class.java).apply {
                putExtra(EXTRA_NOTIF_TYPE, type)
                putExtra(NotificationHelper.EXTRA_CHAT_ID, chatId)
                if (type == TYPE_MESSAGE) {
                    putExtra(EXTRA_SENDER_NAME, senderOrCallerName ?: "Elena Vance")
                    putExtra(EXTRA_MESSAGE_TEXT, messageText ?: "Background message notification from Tale Pulse! 💬")
                } else if (type == TYPE_CALL) {
                    putExtra(NotificationHelper.EXTRA_CALLER_NAME, senderOrCallerName ?: "Marcus Miller")
                    putExtra(NotificationHelper.EXTRA_CALLER_EMAIL, "marcus@talepulse.com")
                    putExtra(NotificationHelper.EXTRA_CALL_TYPE, if (isVideoCall) "Video" else "Voice")
                } else if (type == TYPE_FRIEND_REQUEST) {
                    putExtra(NotificationHelper.EXTRA_REQUESTER_NAME, senderOrCallerName ?: "Sophia Chen")
                    putExtra(NotificationHelper.EXTRA_REQUESTER_EMAIL, "sophia@talepulse.com")
                }
            }

            val requestCode = (System.currentTimeMillis() % 10000).toInt()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val triggerAtMs = System.currentTimeMillis() + (delaySeconds * 1000)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent
                )
            }
        }
    }
}
