package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.example.MainActivity

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        when (action) {
            NotificationHelper.ACTION_REPLY_MESSAGE -> {
                val results = RemoteInput.getResultsFromIntent(intent)
                val replyText = results?.getCharSequence(NotificationHelper.KEY_TEXT_REPLY)?.toString()
                val chatId = intent.getStringExtra(NotificationHelper.EXTRA_CHAT_ID) ?: ""
                val senderName = intent.getStringExtra(NotificationHelper.EXTRA_CALLER_NAME) ?: "Chat"

                if (!replyText.isNull_or_blank()) {
                    Toast.makeText(context, "Reply sent to $senderName: \"$replyText\"", Toast.LENGTH_SHORT).show()

                    // Forward message reply to MainActivity if active or launch background processing
                    val forwardIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("extra_direct_reply_chat_id", chatId)
                        putExtra("extra_direct_reply_text", replyText)
                    }
                    context.startActivity(forwardIntent)

                    // Post a confirmation notification update
                    val confirmationNotif = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID_MESSAGES)
                        .setSmallIcon(android.R.drawable.stat_notify_chat)
                        .setContentTitle("Replied to $senderName")
                        .setContentText("You: $replyText")
                        .setAutoCancel(true)
                        .setTimeoutAfter(3000)
                        .build()

                    try {
                        NotificationManagerCompat.from(context).notify(chatId.hashCode(), confirmationNotif)
                    } catch (_: SecurityException) {}
                }
            }

            NotificationHelper.ACTION_MARK_READ -> {
                val chatId = intent.getStringExtra(NotificationHelper.EXTRA_CHAT_ID) ?: ""
                NotificationHelper.cancelNotification(context, chatId.hashCode())
                Toast.makeText(context, "Marked as read", Toast.LENGTH_SHORT).show()
            }

            NotificationHelper.ACTION_ACCEPT_FRIEND_REQUEST -> {
                val requesterName = intent.getStringExtra(NotificationHelper.EXTRA_REQUESTER_NAME) ?: "Friend"
                val requesterEmail = intent.getStringExtra(NotificationHelper.EXTRA_REQUESTER_EMAIL) ?: ""

                NotificationHelper.cancelNotification(context, 3001)
                Toast.makeText(context, "Accepted friend request from $requesterName!", Toast.LENGTH_LONG).show()

                // Send forward intent to MainActivity to trigger ViewModel logic
                val forwardIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("extra_accept_friend_email", requesterEmail)
                }
                context.startActivity(forwardIntent)
            }

            NotificationHelper.ACTION_DECLINE_CALL -> {
                val callerName = intent.getStringExtra(NotificationHelper.EXTRA_CALLER_NAME) ?: "Caller"
                NotificationHelper.cancelNotification(context, 2001)
                Toast.makeText(context, "Declined call from $callerName", Toast.LENGTH_SHORT).show()

                val forwardIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("extra_decline_call", true)
                }
                context.startActivity(forwardIntent)
            }
        }
    }

    private fun CharSequence?.isNull_or_blank(): Boolean {
        return this == null || this.trim().isEmpty()
    }
}
