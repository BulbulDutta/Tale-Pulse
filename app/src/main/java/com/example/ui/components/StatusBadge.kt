package com.example.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Emerald500
import com.example.ui.theme.StatusReadBlue

@Composable
fun MessageStatusTicks(
    status: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 15.dp
) {
    when (status) {
        "READ" -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Read",
                tint = StatusReadBlue,
                modifier = modifier.size(iconSize)
            )
        }
        "DELIVERED" -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Delivered",
                tint = Color.Gray,
                modifier = modifier.size(iconSize)
            )
        }
        else -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Sent",
                tint = Color.Gray,
                modifier = modifier.size(iconSize)
            )
        }
    }
}

@Composable
fun EmailTransportBadge(
    emailStatus: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        val (icon, color, desc) = when (emailStatus) {
            "DELIVERED_INBOX" -> Triple(Icons.Default.MarkEmailRead, Emerald500, "Email Delivered")
            "DISPATCHED_SMTP" -> Triple(Icons.Default.Email, Color(0xFF3B82F6), "SMTP Dispatched")
            else -> Triple(Icons.Default.Email, Color.Gray, "Email Pending")
        }

        Icon(
            imageVector = icon,
            contentDescription = desc,
            tint = color,
            modifier = Modifier.size(13.dp)
        )
    }
}
