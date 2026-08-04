package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.theme.Emerald500
import com.example.ui.theme.ReceivedBubbleDark
import com.example.ui.theme.ReceivedBubbleLight
import com.example.ui.theme.SentBubbleDark
import com.example.ui.theme.SentBubbleLight
import org.json.JSONObject

/**
 * Reusable ChatBubble component with modern, minimalist styling.
 * Supports text content, rich typography, media attachment indicators (Images, Audio Voice Notes, Documents),
 * Polls with live vote casting, Locations, Contacts, Events, E2EE encryption status, and email transport delivery badges.
 */
@Composable
fun ChatBubble(
    text: String,
    senderName: String? = null,
    isFromMe: Boolean,
    timestampText: String,
    status: String = "SENT", // "SENT", "DELIVERED", "READ"
    emailTransportStatus: String? = null, // "PENDING", "DISPATCHED_SMTP", "DELIVERED_INBOX"
    mediaUri: String? = null,
    mediaType: String? = null, // "IMAGE", "VIDEO", "AUDIO", "DOCUMENT", "POLL", "LOCATION", "CONTACT", "EVENT"
    formattedRichText: String? = null, // "BOLD", "ITALIC", "CODE"
    isEncrypted: Boolean = true,
    currentUserId: String? = null,
    reactionsJson: String = "{}",
    onToggleReaction: ((String) -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    onVotePoll: ((optionIndex: Int) -> Unit)? = null,
    onMediaClick: (() -> Unit)? = null,
    onEmailBadgeClick: (() -> Unit)? = null,
    onSwipeToReply: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.surface == ReceivedBubbleDark
    val bubbleColor = if (isFromMe) {
        if (isDark) SentBubbleDark else SentBubbleLight
    } else {
        if (isDark) ReceivedBubbleDark else ReceivedBubbleLight
    }

    var isPlayingAudio by remember { mutableStateOf(false) }
    var showFullscreenImage by remember { mutableStateOf(false) }
    var swipeOffsetX by remember { mutableFloatStateOf(0f) }
    val maxSwipeOffset = 100f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            if (swipeOffsetX > 5f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                        .size(32.dp)
                        .graphicsLayer {
                            alpha = (swipeOffsetX / 50f).coerceIn(0f, 1f)
                            scaleX = (swipeOffsetX / 50f).coerceIn(0.5f, 1f)
                            scaleY = (swipeOffsetX / 50f).coerceIn(0.5f, 1f)
                        }
                        .background(Emerald500, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = "Swipe to Reply",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isFromMe) 16.dp else 4.dp,
                    bottomEnd = if (isFromMe) 4.dp else 16.dp
                ),
                color = bubbleColor,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .offset { IntOffset(swipeOffsetX.roundToInt(), 0) }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                swipeOffsetX = (swipeOffsetX + dragAmount).coerceIn(0f, maxSwipeOffset)
                            },
                            onDragEnd = {
                                if (swipeOffsetX >= 45f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSwipeToReply?.invoke()
                                }
                                swipeOffsetX = 0f
                            },
                            onDragCancel = {
                                swipeOffsetX = 0f
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onLongPress?.invoke()
                            }
                        )
                    }
                    .testTag("chat_bubble_${if (isFromMe) "sent" else "received"}")
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Quoted Message Reply Header
                    if (formattedRichText?.startsWith("REPLY:") == true) {
                        val replyData = formattedRichText.removePrefix("REPLY:")
                        val parts = replyData.split("|", limit = 2)
                        val replySenderName = parts.getOrNull(0) ?: "Message"
                        val replyTextSnippet = parts.getOrNull(1) ?: ""

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(26.dp)
                                        .background(Emerald500, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = replySenderName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Emerald500
                                    )
                                    Text(
                                        text = replyTextSnippet,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Sender Label for incoming messages
                    if (!isFromMe && !senderName.isNullOrBlank()) {
                    Text(
                        text = senderName!!,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (senderName.contains("Gemini")) Color(0xFF7C4DFF) else Emerald500
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                }

                // Attachment Cards according to mediaType
                when (mediaType?.uppercase()) {
                    "POLL" -> {
                        PollBubbleContent(
                            pollJsonStr = text,
                            currentUserId = currentUserId,
                            onVotePoll = onVotePoll
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    "IMAGE" -> {
                        if (!mediaUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = mediaUri,
                                contentDescription = "Media Attachment",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        showFullscreenImage = true
                                        onMediaClick?.invoke()
                                    }
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Image Indicator",
                                    tint = Emerald500,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Photo Attachment",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    "VIDEO" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.85f))
                                .clickable { onMediaClick?.invoke() },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = "Play Video",
                                    tint = Emerald500,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Video Attachment • 0:45",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    "AUDIO" -> {
                        val context = LocalContext.current
                        val audioTargetUri = mediaUri ?: ""
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (audioTargetUri.isNotBlank()) {
                                        if (isPlayingAudio) {
                                            com.example.util.AudioPlayerManager.pauseAudio()
                                            isPlayingAudio = false
                                        } else {
                                            isPlayingAudio = true
                                            com.example.util.AudioPlayerManager.playAudio(context, audioTargetUri) {
                                                isPlayingAudio = false
                                            }
                                        }
                                    } else {
                                        isPlayingAudio = !isPlayingAudio
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlayingAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlayingAudio) "Pause Voice Note" else "Play Voice Note",
                                    tint = Emerald500
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isPlayingAudio) "🔊 Voice Note • Playing..." else "🎙️ Voice Note • AAC/m4a",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isPlayingAudio) "||l|i|l|ll|i|l|ll||l|i" else "|i|ll|i|l|ll||l|i|ll|i",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Emerald500
                                )
                            }
                            if (!mediaUri.isNullOrEmpty()) {
                                IconButton(
                                    onClick = { onMediaClick?.invoke() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Download Audio",
                                        tint = Emerald500,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    "DOCUMENT" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF7F66FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = "Document Attachment",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (text.isNotBlank() && !text.startsWith("{")) text else "Attachment_Doc.pdf",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "File Document • Tap to Save",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onMediaClick?.invoke() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download Document",
                                    tint = Emerald500,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    "LOCATION" -> {
                        LocationBubbleContent(locationJsonStr = text)
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    "CONTACT" -> {
                        ContactBubbleContent(contactJsonStr = text)
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    "EVENT" -> {
                        EventBubbleContent(eventJsonStr = text)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                // Standard Text Content if NOT a special media type like Poll
                val isDefaultImageMetadata = mediaType?.uppercase() == "IMAGE" && (
                    text.isBlank() ||
                    text.equals("Photo Attachment", ignoreCase = true) ||
                    text.equals("Photo", ignoreCase = true) ||
                    text.equals("Image", ignoreCase = true) ||
                    text.startsWith("http://", ignoreCase = true) ||
                    text.startsWith("https://", ignoreCase = true) ||
                    text.startsWith("img_", ignoreCase = true) ||
                    text.endsWith(".jpg", ignoreCase = true) ||
                    text.endsWith(".jpeg", ignoreCase = true) ||
                    text.endsWith(".png", ignoreCase = true)
                )

                if (text.isNotBlank() && mediaType?.uppercase() !in listOf("POLL", "LOCATION", "CONTACT", "EVENT") && !isDefaultImageMetadata) {
                    val fontStyle = if (formattedRichText == "ITALIC") FontStyle.Italic else FontStyle.Normal
                    val fontWeight = if (formattedRichText == "BOLD") FontWeight.Bold else FontWeight.Normal
                    val fontFamily = if (formattedRichText == "CODE") FontFamily.Monospace else FontFamily.Default

                    Text(
                        text = text,
                        fontSize = 14.sp,
                        fontStyle = fontStyle,
                        fontWeight = fontWeight,
                        fontFamily = fontFamily,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (text.contains("Google Meet") || text.contains("meet.google.com") || text.contains("Join my") || text.contains("meet.")) {
                        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                        val extractedUrl = text.lines().firstOrNull { it.contains("http") }?.trim()
                            ?: if (text.contains("https://")) "https://" + text.substringAfter("https://").substringBefore(" ").substringBefore("\n") else "https://meet.google.com"

                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = Color(0xFF00897B).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF00897B), RoundedCornerShape(12.dp))
                                .clickable {
                                    try {
                                        uriHandler.openUri(if (extractedUrl.startsWith("http")) extractedUrl else "https://$extractedUrl")
                                    } catch (_: Exception) {
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00897B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Videocam, contentDescription = "Google Meet", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Google Meet Session", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF00897B))
                                    Text("Tap to join Google Meet call", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Icon(Icons.Default.OpenInNew, contentDescription = "Open Google Meet", tint = Color(0xFF00897B), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Footer Bar (Encryption Lock + Email Status + Time + Read Ticks)
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    if (isEncrypted) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted Message",
                            tint = Emerald500.copy(alpha = 0.8f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    if (emailTransportStatus != null) {
                        Box(modifier = Modifier.clickable(enabled = onEmailBadgeClick != null) { onEmailBadgeClick?.invoke() }) {
                            EmailTransportBadge(emailStatus = emailTransportStatus)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Text(
                        text = timestampText,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isFromMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        MessageStatusTicks(status = status)
                    }
                }

                if (onToggleReaction != null && reactionsJson.isNotBlank() && reactionsJson != "{}") {
                    MessageReactionBadges(
                        reactionsJson = reactionsJson,
                        currentUserId = currentUserId,
                        onToggleReaction = onToggleReaction
                    )
                }
            }
        }

        if (showFullscreenImage && !mediaUri.isNullOrEmpty()) {
            Dialog(
                onDismissRequest = { showFullscreenImage = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.95f))
                        .clickable { showFullscreenImage = false },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = mediaUri,
                        contentDescription = "Full Screen Photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                    IconButton(
                        onClick = { showFullscreenImage = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
}

data class PollOptionData(
    val index: Int,
    val text: String,
    val votes: List<String>
)

@Composable
fun PollBubbleContent(
    pollJsonStr: String,
    currentUserId: String?,
    onVotePoll: ((optionIndex: Int) -> Unit)?
) {
    var question = "Poll"
    var allowMultiple = false
    val optionsList = mutableListOf<PollOptionData>()
    var totalVotes = 0

    try {
        val json = JSONObject(pollJsonStr)
        question = json.optString("question", "Poll")
        allowMultiple = json.optBoolean("allowMultiple", false)
        val arr = json.optJSONArray("options")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val optObj = arr.getJSONObject(i)
                val optText = optObj.optString("text", "Option")
                val votesArr = optObj.optJSONArray("votes")
                val votes = mutableListOf<String>()
                if (votesArr != null) {
                    for (j in 0 until votesArr.length()) {
                        votes.add(votesArr.getString(j))
                    }
                }
                totalVotes += votes.size
                optionsList.add(PollOptionData(i, optText, votes))
            }
        }
    } catch (e: Exception) {
        question = pollJsonStr
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Poll,
                contentDescription = "Poll",
                tint = Emerald500,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = question,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = if (allowMultiple) "Select one or more options" else "Select one option",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 10.dp, top = 2.dp)
        )

        optionsList.forEachIndexed { index, option ->
            val hasVoted = currentUserId != null && option.votes.contains(currentUserId)
            val percentage = if (totalVotes > 0) option.votes.size.toFloat() / totalVotes else 0f
            val animatedProgress by animateFloatAsState(targetValue = percentage, label = "poll_progress")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onVotePoll?.invoke(index) }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (hasVoted) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Vote option",
                        tint = if (hasVoted) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = option.text,
                        fontSize = 13.sp,
                        fontWeight = if (hasVoted) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${option.votes.size} (${(percentage * 100).toInt()}%)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald500
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Emerald500,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Total votes: $totalVotes",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LocationBubbleContent(locationJsonStr: String) {
    var title = "Pinned Location"
    var address = "San Francisco, CA"
    var lat = 37.7749
    var lng = -122.4194

    try {
        val json = JSONObject(locationJsonStr)
        title = json.optString("title", "Pinned Location")
        address = json.optString("address", "San Francisco, CA")
        lat = json.optDouble("latitude", 37.7749)
        lng = json.optDouble("longitude", -122.4194)
    } catch (e: Exception) {
        title = locationJsonStr
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00C853)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(text = address, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "GPS: $lat, $lng",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = Emerald500
        )
    }
}

@Composable
fun ContactBubbleContent(contactJsonStr: String) {
    var name = "Shared Contact"
    var email = "contact@linko.com"
    var phone = "+1 (555) 012-3456"

    try {
        val json = JSONObject(contactJsonStr)
        name = json.optString("name", "Shared Contact")
        email = json.optString("email", "contact@linko.com")
        phone = json.optString("phone", "+1 (555) 012-3456")
    } catch (e: Exception) {
        name = contactJsonStr
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(name = name, size = 40.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(text = email, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = phone, fontSize = 11.sp, color = Emerald500)
            }
        }
    }
}

@Composable
fun EventBubbleContent(eventJsonStr: String) {
    var title = "Scheduled Event"
    var dateText = "Tomorrow, 3:00 PM"
    var location = "Online Room"

    try {
        val json = JSONObject(eventJsonStr)
        title = json.optString("title", "Scheduled Event")
        dateText = json.optString("dateText", "Tomorrow, 3:00 PM")
        location = json.optString("location", "Online Room")
    } catch (e: Exception) {
        title = eventJsonStr
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3F51B5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = "Event",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(text = "📅 $dateText", fontSize = 11.sp, color = Emerald500, fontWeight = FontWeight.Bold)
                Text(text = "📍 $location", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
