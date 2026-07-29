package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.example.MainActivity

object NotificationHelper {

    const val CHANNEL_ID_MESSAGES = "channel_messages_talepulse"
    const val CHANNEL_ID_CALLS = "channel_calls_talepulse"
    const val CHANNEL_ID_FRIENDS = "channel_friends_talepulse"

    const val KEY_TEXT_REPLY = "key_text_reply"
    const val ACTION_REPLY_MESSAGE = "com.example.talepulse.ACTION_REPLY_MESSAGE"
    const val ACTION_MARK_READ = "com.example.talepulse.ACTION_MARK_READ"
    const val ACTION_ACCEPT_FRIEND_REQUEST = "com.example.talepulse.ACTION_ACCEPT_FRIEND_REQUEST"
    const val ACTION_DECLINE_CALL = "com.example.talepulse.ACTION_DECLINE_CALL"

    const val EXTRA_SCREEN = "extra_screen"
    const val EXTRA_CHAT_ID = "extra_chat_id"
    const val EXTRA_CALLER_NAME = "extra_caller_name"
    const val EXTRA_CALLER_EMAIL = "extra_caller_email"
    const val EXTRA_CALL_TYPE = "extra_call_type"
    const val EXTRA_REQUESTER_NAME = "extra_requester_name"
    const val EXTRA_REQUESTER_EMAIL = "extra_requester_email"

    const val SCREEN_DIRECT_CHAT = "direct_chat"
    const val SCREEN_INCOMING_CALL = "incoming_call"
    const val SCREEN_FRIENDS = "friends"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Messages Channel
            val msgChannel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time alerts for incoming chat messages & direct replies"
                enableVibration(true)
                setShowBadge(true)
            }

            // Calls Channel
            val callChannel = NotificationChannel(
                CHANNEL_ID_CALLS,
                "Voice & Video Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for incoming encrypted voice & video calls"
                enableVibration(true)
            }

            // Friends Channel
            val friendChannel = NotificationChannel(
                CHANNEL_ID_FRIENDS,
                "Friend Requests",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for new friend connections & requests"
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(listOf(msgChannel, callChannel, friendChannel))
        }
    }

    /**
     * Show actionable notification for a new message with Direct Reply input.
     */
    fun showNewMessageNotification(
        context: Context,
        chatId: String,
        senderName: String,
        messageText: String
    ) {
        createNotificationChannels(context)

        // Intent to open DirectChatScreen when clicked
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SCREEN, SCREEN_DIRECT_CHAT)
            putExtra(EXTRA_CHAT_ID, chatId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            chatId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Direct Reply RemoteInput
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Reply to $senderName...")
            .build()

        val replyIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_REPLY_MESSAGE
            putExtra(EXTRA_CHAT_ID, chatId)
            putExtra(EXTRA_CALLER_NAME, senderName)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            chatId.hashCode() + 1,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "Reply",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .build()

        // Mark as Read Action
        val markReadIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_MARK_READ
            putExtra(EXTRA_CHAT_ID, chatId)
        }
        val markReadPendingIntent = PendingIntent.getBroadcast(
            context,
            chatId.hashCode() + 2,
            markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markReadAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Mark as Read",
            markReadPendingIntent
        ).build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(senderName)
            .setContentText(messageText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(replyAction)
            .addAction(markReadAction)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(chatId.hashCode(), notification)
        } catch (_: SecurityException) {}
    }

    /**
     * Show actionable notification for an incoming call with Answer & Decline buttons.
     */
    fun showIncomingCallNotification(
        context: Context,
        callerName: String,
        callerEmail: String,
        callType: String
    ) {
        createNotificationChannels(context)
        val notificationId = 2001

        // Answer Intent -> opens app directly to incoming/active call overlay
        val answerIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SCREEN, SCREEN_INCOMING_CALL)
            putExtra(EXTRA_CALLER_NAME, callerName)
            putExtra(EXTRA_CALLER_EMAIL, callerEmail)
            putExtra(EXTRA_CALL_TYPE, callType)
        }
        val answerPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Decline Intent -> broadcast receiver cancels call
        val declineIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_DECLINE_CALL
            putExtra(EXTRA_CALLER_NAME, callerName)
        }
        val declinePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val answerAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_call,
            "Answer",
            answerPendingIntent
        ).build()

        val declineAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Decline",
            declinePendingIntent
        ).build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_CALLS)
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .setContentTitle("Incoming $callType Call")
            .setContentText("$callerName is calling you...")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setAutoCancel(true)
            .setContentIntent(answerPendingIntent)
            .addAction(answerAction)
            .addAction(declineAction)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {}
    }

    /**
     * Show actionable notification for a new friend request with Accept & View buttons.
     */
    fun showFriendRequestNotification(
        context: Context,
        requesterName: String,
        requesterEmail: String
    ) {
        createNotificationChannels(context)
        val notificationId = 3001

        // View Intent -> opens app to Friends tab
        val viewIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SCREEN, SCREEN_FRIENDS)
        }
        val viewPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            viewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Accept Intent -> broadcast receiver auto-accepts
        val acceptIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_ACCEPT_FRIEND_REQUEST
            putExtra(EXTRA_REQUESTER_NAME, requesterName)
            putExtra(EXTRA_REQUESTER_EMAIL, requesterEmail)
        }
        val acceptPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val acceptAction = NotificationCompat.Action.Builder(
            android.R.drawable.checkbox_on_background,
            "Accept",
            acceptPendingIntent
        ).build()

        val viewAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_view,
            "View Request",
            viewPendingIntent
        ).build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_FRIENDS)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("New Friend Request")
            .setContentText("$requesterName ($requesterEmail) wants to connect with you.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setAutoCancel(true)
            .setContentIntent(viewPendingIntent)
            .addAction(acceptAction)
            .addAction(viewAction)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {}
    }

    fun cancelNotification(context: Context, id: Int) {
        try {
            NotificationManagerCompat.from(context).cancel(id)
        } catch (_: Exception) {}
    }
}
