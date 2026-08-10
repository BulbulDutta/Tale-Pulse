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

enum class NotificationSoundProfile(
    val id: String,
    val title: String,
    val description: String,
    val systemSoundType: Int,
    val toneType: Int
) {
    CHIME_PULSE(
        id = "chime_pulse",
        title = "Chime & Pulse 🔔",
        description = "High-pitch soft notification chime",
        systemSoundType = android.media.RingtoneManager.TYPE_NOTIFICATION,
        toneType = android.media.ToneGenerator.TONE_PROP_BEEP
    ),
    SUBTLE_POP(
        id = "subtle_pop",
        title = "Subtle Pop 💬",
        description = "Quiet double-tap pop for discrete alerts",
        systemSoundType = android.media.RingtoneManager.TYPE_NOTIFICATION,
        toneType = android.media.ToneGenerator.TONE_PROP_PROMPT
    ),
    GENTLE_BELL(
        id = "gentle_bell",
        title = "Gentle Bell 🛎️",
        description = "Melodic bell echo tone",
        systemSoundType = android.media.RingtoneManager.TYPE_NOTIFICATION,
        toneType = android.media.ToneGenerator.TONE_SUP_RINGTONE
    ),
    UPBEAT_SPARK(
        id = "upbeat_spark",
        title = "Upbeat Spark ✨",
        description = "Energetic multi-tone alert",
        systemSoundType = android.media.RingtoneManager.TYPE_NOTIFICATION,
        toneType = android.media.ToneGenerator.TONE_CDMA_HIGH_L
    ),
    GROUP_HARMONY(
        id = "group_harmony",
        title = "Group Harmony 👥",
        description = "Multi-step chord tuned for lively group discussions",
        systemSoundType = android.media.RingtoneManager.TYPE_NOTIFICATION,
        toneType = android.media.ToneGenerator.TONE_CDMA_INTERCEPT
    ),
    DOUBLE_CHIME(
        id = "double_chime",
        title = "Double Chime 🎶",
        description = "Distinctive double-pulse chime for group messages",
        systemSoundType = android.media.RingtoneManager.TYPE_NOTIFICATION,
        toneType = android.media.ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE
    ),
    SYSTEM_DEFAULT(
        id = "system_default",
        title = "Default System Tone 📱",
        description = "Standard system notification sound",
        systemSoundType = android.media.RingtoneManager.TYPE_NOTIFICATION,
        toneType = android.media.ToneGenerator.TONE_PROP_BEEP
    ),
    SILENT(
        id = "silent",
        title = "Silent / Vibrate Only 🔇",
        description = "No audio, tactile vibration only",
        systemSoundType = -1,
        toneType = -1
    );

    companion object {
        fun fromId(id: String, default: NotificationSoundProfile = SYSTEM_DEFAULT): NotificationSoundProfile {
            return values().find { it.id == id } ?: default
        }

        val directMessageProfiles = listOf(
            CHIME_PULSE,
            SUBTLE_POP,
            GENTLE_BELL,
            UPBEAT_SPARK,
            SYSTEM_DEFAULT,
            SILENT
        )

        val groupMessageProfiles = listOf(
            GROUP_HARMONY,
            DOUBLE_CHIME,
            SUBTLE_POP,
            UPBEAT_SPARK,
            SYSTEM_DEFAULT,
            SILENT
        )
    }
}

object NotificationHelper {

    private const val PREFS_SOUNDS = "linko_notification_sound_prefs"
    private const val KEY_DM_SOUND = "dm_sound_profile"
    private const val KEY_GROUP_SOUND = "group_sound_profile"

    const val CHANNEL_ID_MESSAGES = "channel_messages_linko"
    const val CHANNEL_ID_DM_MESSAGES = "channel_dm_messages_linko"
    const val CHANNEL_ID_GROUP_MESSAGES = "channel_group_messages_linko"
    const val CHANNEL_ID_CALLS = "channel_calls_linko"
    const val CHANNEL_ID_FRIENDS = "channel_friends_linko"

    const val KEY_TEXT_REPLY = "key_text_reply"
    const val ACTION_REPLY_MESSAGE = "com.example.linko.ACTION_REPLY_MESSAGE"
    const val ACTION_MARK_READ = "com.example.linko.ACTION_MARK_READ"
    const val ACTION_ACCEPT_FRIEND_REQUEST = "com.example.linko.ACTION_ACCEPT_FRIEND_REQUEST"
    const val ACTION_DECLINE_CALL = "com.example.linko.ACTION_DECLINE_CALL"

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

    fun getDirectMessageSoundProfile(context: Context): NotificationSoundProfile {
        val prefs = context.getSharedPreferences(PREFS_SOUNDS, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_DM_SOUND, NotificationSoundProfile.CHIME_PULSE.id)
        return NotificationSoundProfile.fromId(saved ?: "", NotificationSoundProfile.CHIME_PULSE)
    }

    fun setDirectMessageSoundProfile(context: Context, profile: NotificationSoundProfile) {
        val prefs = context.getSharedPreferences(PREFS_SOUNDS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DM_SOUND, profile.id).apply()
        createNotificationChannels(context)
    }

    fun getGroupMessageSoundProfile(context: Context): NotificationSoundProfile {
        val prefs = context.getSharedPreferences(PREFS_SOUNDS, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_GROUP_SOUND, NotificationSoundProfile.GROUP_HARMONY.id)
        return NotificationSoundProfile.fromId(saved ?: "", NotificationSoundProfile.GROUP_HARMONY)
    }

    fun setGroupMessageSoundProfile(context: Context, profile: NotificationSoundProfile) {
        val prefs = context.getSharedPreferences(PREFS_SOUNDS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_GROUP_SOUND, profile.id).apply()
        createNotificationChannels(context)
    }

    fun playSoundPreview(context: Context, profile: NotificationSoundProfile) {
        if (profile == NotificationSoundProfile.SILENT) return
        try {
            if (profile.toneType != -1) {
                val toneGenerator = android.media.ToneGenerator(
                    android.media.AudioManager.STREAM_NOTIFICATION,
                    100
                )
                toneGenerator.startTone(profile.toneType, 250)
            } else {
                val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = android.media.RingtoneManager.getRingtone(context, uri)
                ringtone?.play()
            }
        } catch (_: Throwable) {
            try {
                val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = android.media.RingtoneManager.getRingtone(context, uri)
                ringtone?.play()
            } catch (_: Throwable) {}
        }
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val dmSound = getDirectMessageSoundProfile(context)
            val groupSound = getGroupMessageSoundProfile(context)

            // Direct Messages Channel
            val dmChannel = NotificationChannel(
                "${CHANNEL_ID_DM_MESSAGES}_${dmSound.id}",
                "Direct Messages (${dmSound.title})",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for 1-on-1 direct messages (Tone: ${dmSound.title})"
                enableVibration(true)
                setShowBadge(true)
                if (dmSound == NotificationSoundProfile.SILENT) {
                    setSound(null, null)
                } else {
                    val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                    setSound(
                        soundUri,
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                }
            }

            // Group Messages Channel
            val groupChannel = NotificationChannel(
                "${CHANNEL_ID_GROUP_MESSAGES}_${groupSound.id}",
                "Group Messages (${groupSound.title})",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for group chat discussions (Tone: ${groupSound.title})"
                enableVibration(true)
                setShowBadge(true)
                if (groupSound == NotificationSoundProfile.SILENT) {
                    setSound(null, null)
                } else {
                    val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                    setSound(
                        soundUri,
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                }
            }

            // Legacy Messages Channel
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

            notificationManager.createNotificationChannels(listOf(dmChannel, groupChannel, msgChannel, callChannel, friendChannel))
        }
    }

    /**
     * Show actionable notification for a new message with Direct Reply input.
     * Supports sound profiles for Direct vs Group messages.
     */
    fun showNewMessageNotification(
        context: Context,
        chatId: String,
        senderName: String,
        messageText: String,
        isGroup: Boolean = false
    ) {
        createNotificationChannels(context)

        val soundProfile = if (isGroup) getGroupMessageSoundProfile(context) else getDirectMessageSoundProfile(context)
        val channelId = if (isGroup) "${CHANNEL_ID_GROUP_MESSAGES}_${soundProfile.id}" else "${CHANNEL_ID_DM_MESSAGES}_${soundProfile.id}"

        // Play audio feedback preview
        playSoundPreview(context, soundProfile)

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

        val notification = NotificationCompat.Builder(context, channelId)
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
     * Show actionable notification for a missed call with Call Back action button.
     */
    fun showMissedCallNotification(
        context: Context,
        callerName: String,
        callerEmail: String,
        callType: String
    ) {
        createNotificationChannels(context)
        val notificationId = 2002

        // Call back intent -> opens app directly to call screen
        val callBackIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SCREEN, SCREEN_INCOMING_CALL)
            putExtra(EXTRA_CALLER_NAME, callerName)
            putExtra(EXTRA_CALLER_EMAIL, callerEmail)
            putExtra(EXTRA_CALL_TYPE, callType)
        }
        val callBackPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            callBackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callBackAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_call,
            "Call Back",
            callBackPendingIntent
        ).build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_CALLS)
            .setSmallIcon(android.R.drawable.stat_notify_missed_call)
            .setContentTitle("Missed $callType Call")
            .setContentText("You missed a $callType call from $callerName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .setAutoCancel(true)
            .setContentIntent(callBackPendingIntent)
            .addAction(callBackAction)
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
