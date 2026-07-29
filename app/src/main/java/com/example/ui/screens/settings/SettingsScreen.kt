package com.example.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.entity.UserEntity
import com.example.ui.components.QrCodeGenerator
import com.example.ui.components.UserAvatar
import com.example.ui.components.WallpaperPreviewDialog
import com.example.ui.theme.AppThemePalette
import com.example.ui.theme.ChatWallpaper
import com.example.ui.theme.ChatWallpaperBackground

@Composable
fun SettingsScreen(
    user: UserEntity?,
    isDarkMode: Boolean,
    currentPalette: AppThemePalette = AppThemePalette.EMERALD,
    currentWallpaper: ChatWallpaper = ChatWallpaper.DOODLE,
    customWallpaperUri: String? = null,
    customWallpaperDimming: Float = 0.3f,
    customWallpaperScale: String = "CROP",
    onToggleDarkMode: () -> Unit,
    onSelectPalette: (AppThemePalette) -> Unit,
    onSelectWallpaper: (ChatWallpaper) -> Unit,
    onApplyCustomWallpaper: (uri: String, dimming: Float, scale: String) -> Unit = { _, _, _ -> },
    onTriggerMessageNotification: () -> Unit = {},
    onTriggerCallNotification: (isVideo: Boolean) -> Unit = {},
    onTriggerFriendRequestNotification: () -> Unit = {},
    onUpdateProfile: (displayName: String, statusMessage: String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showQrModal by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    var editDisplayName by remember { mutableStateOf(user?.displayName ?: "") }
    var editStatusMessage by remember { mutableStateOf(user?.statusMessage ?: "") }

    var pendingCustomWallpaperUri by remember { mutableStateOf<String?>(null) }
    var showWallpaperPreviewDialog by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingCustomWallpaperUri = uri.toString()
            showWallpaperPreviewDialog = true
        }
    }

    if (showWallpaperPreviewDialog && pendingCustomWallpaperUri != null) {
        WallpaperPreviewDialog(
            imageUri = pendingCustomWallpaperUri!!,
            initialDimming = customWallpaperDimming,
            initialScale = customWallpaperScale,
            isDarkMode = isDarkMode,
            onDismiss = { showWallpaperPreviewDialog = false },
            onApplyWallpaper = { dimming, scale ->
                onApplyCustomWallpaper(pendingCustomWallpaperUri!!, dimming, scale)
                showWallpaperPreviewDialog = false
            }
        )
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Profile Card Header
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    UserAvatar(name = user?.displayName ?: "User", size = 80.dp)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = user?.displayName ?: "User Name",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = user?.email ?: "email@talepulse.com",
                        fontSize = 13.sp,
                        color = currentPalette.primaryColor
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "\"${user?.statusMessage}\"",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row {
                        Button(
                            onClick = { showQrModal = !showQrModal },
                            colors = ButtonDefaults.buttonColors(containerColor = currentPalette.primaryColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.QrCode2, contentDescription = "QR Code")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("My Profile QR", fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        IconButton(
                            onClick = {
                                editDisplayName = user?.displayName ?: ""
                                editStatusMessage = user?.statusMessage ?: ""
                                showEditProfileDialog = true
                            },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                .size(48.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = currentPalette.primaryColor)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Expandable Profile QR Code Viewer Card
            AnimatedVisibility(visible = showQrModal) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Scan QR to Add Me",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Friends can scan this QR code with their camera to instantly add you to their Tale Pulse contacts.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        QrCodeGenerator(payload = user?.qrPayload ?: "talepulse://user?email=${user?.email}")

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = user?.qrPayload ?: "",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Customization & Appearance Panel
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("appearance_customization_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Theme Customization",
                            tint = currentPalette.primaryColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Theme & Chat Appearance",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dark Mode Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = "Dark Mode",
                            tint = currentPalette.primaryColor
                        )
                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dark Theme Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Sleek night palette & safe contrast", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { onToggleDarkMode() },
                            colors = SwitchDefaults.colors(checkedThumbColor = currentPalette.primaryColor)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Color Theme Selector
                    Text(
                        text = "Accent Color Theme",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Switch main app highlight & active tab color",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(AppThemePalette.values(), key = { it.id }) { palette ->
                            val isSelected = palette == currentPalette
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isSelected) palette.primaryColor.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) palette.primaryColor else Color.Transparent,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onSelectPalette(palette) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("theme_chip_${palette.id}")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(palette.primaryColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = palette.displayName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (isSelected) palette.primaryColor else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Chat Wallpaper Background Selector
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wallpaper,
                            contentDescription = "Wallpaper",
                            tint = currentPalette.primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Chat Background Wallpaper",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Customize chat backdrops with built-in themes or photos from your gallery",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Prominent Custom Theme Gallery Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentWallpaper == ChatWallpaper.CUSTOM) currentPalette.primaryColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (currentWallpaper == ChatWallpaper.CUSTOM) 2.dp else 1.dp,
                                color = if (currentWallpaper == ChatWallpaper.CUSTOM) currentPalette.primaryColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .testTag("add_custom_wallpaper_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Custom Thumbnail or Add Icon
                            Box(
                                modifier = Modifier
                                    .size(width = 68.dp, height = 80.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(currentPalette.primaryColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!customWallpaperUri.isNullOrBlank()) {
                                    val scale = when (customWallpaperScale) {
                                        "FIT" -> ContentScale.Fit
                                        "FILL" -> ContentScale.FillBounds
                                        else -> ContentScale.Crop
                                    }
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(customWallpaperUri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Custom Wallpaper",
                                        contentScale = scale,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = customWallpaperDimming))
                                    )
                                    if (currentWallpaper == ChatWallpaper.CUSTOM) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(currentPalette.primaryColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = "Add Custom Photo",
                                        tint = currentPalette.primaryColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (!customWallpaperUri.isNullOrBlank()) "Custom Gallery Wallpaper" else "Custom Theme / Gallery",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (!customWallpaperUri.isNullOrBlank()) "Photo selected. Adjust scale or dimming overlay anytime." else "Choose any photo or design from your device gallery",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { galleryLauncher.launch("image/*") },
                                        colors = ButtonDefaults.buttonColors(containerColor = currentPalette.primaryColor),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddPhotoAlternate,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (!customWallpaperUri.isNullOrBlank()) "Change Photo" else "+ Add Custom Wallpaper",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    if (!customWallpaperUri.isNullOrBlank()) {
                                        OutlinedButton(
                                            onClick = {
                                                pendingCustomWallpaperUri = customWallpaperUri
                                                showWallpaperPreviewDialog = true
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Tune,
                                                contentDescription = "Adjust",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Adjust", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Built-in Themes & Patterns",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(ChatWallpaper.values().filter { it != ChatWallpaper.CUSTOM }, key = { it.id }) { wallpaper ->
                            val isSelected = wallpaper == currentWallpaper
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectWallpaper(wallpaper) }
                                    .testTag("wallpaper_item_${wallpaper.id}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 84.dp, height = 110.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) currentPalette.primaryColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    ChatWallpaperBackground(
                                        wallpaper = wallpaper,
                                        isDarkMode = isDarkMode,
                                        customWallpaperUri = customWallpaperUri,
                                        customWallpaperDimming = customWallpaperDimming,
                                        customWallpaperScale = customWallpaperScale
                                    ) {
                                        // Mini Chat Bubble Mock inside wallpaper thumbnail
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(6.dp),
                                            verticalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                            ) {
                                                Text("Hey 👋", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.End)
                                                    .background(currentPalette.primaryColor, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                            ) {
                                                Text("Hi!", fontSize = 8.sp, color = Color.White)
                                            }
                                        }

                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(currentPalette.primaryColor),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = wallpaper.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) currentPalette.primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Live Interactive Preview Box
                    Text(
                        text = "Live Chat Preview",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    ) {
                        ChatWallpaperBackground(
                            wallpaper = currentWallpaper,
                            isDarkMode = isDarkMode,
                            customWallpaperUri = customWallpaperUri,
                            customWallpaperDimming = customWallpaperDimming,
                            customWallpaperScale = customWallpaperScale
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Received bubble
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isDarkMode) Color(0xFF1E293B) else Color.White,
                                            RoundedCornerShape(topStart = 4.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text("How do you like this wallpaper & theme?", fontSize = 12.sp, color = if (isDarkMode) Color.White else Color(0xFF0F172A))
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Sent bubble
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .background(
                                            if (isDarkMode) currentPalette.sentBubbleDark else currentPalette.sentBubbleLight,
                                            RoundedCornerShape(topStart = 12.dp, topEnd = 4.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text("Looks amazing! Perfect colors ✨", fontSize = 12.sp, color = if (isDarkMode) Color.White else Color(0xFF065F46))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Actionable Push Notifications Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("push_notifications_settings_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Push Notifications",
                            tint = currentPalette.primaryColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Push Notification Center",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Alerts for new messages (with direct inline reply), incoming calls (with answer/decline), and friend requests.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Interactive Live Triggers",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap any button below to trigger a live actionable system notification:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Button 1: Simulate Message Notification
                    Button(
                        onClick = onTriggerMessageNotification,
                        colors = ButtonDefaults.buttonColors(containerColor = currentPalette.primaryColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("simulate_message_notification_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simulate Message Alert (Direct Reply)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Button 2: Simulate Call Notification
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onTriggerCallNotification(false) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("simulate_voice_call_btn")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Voice Call Alert", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = { onTriggerCallNotification(true) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("simulate_video_call_btn")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Video Call Alert", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Button 3: Simulate Friend Request Notification
                    Button(
                        onClick = onTriggerFriendRequestNotification,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("simulate_friend_request_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = currentPalette.primaryColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simulate Friend Request Alert", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = currentPalette.primaryColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Channel Status Toggles
                    var notifyMessages by remember { mutableStateOf(true) }
                    var notifyCalls by remember { mutableStateOf(true) }
                    var notifyFriends by remember { mutableStateOf(true) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("New Message Push Notifications", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = notifyMessages,
                            onCheckedChange = { notifyMessages = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = currentPalette.primaryColor)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Voice & Video Incoming Call Banner", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = notifyCalls,
                            onCheckedChange = { notifyCalls = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = currentPalette.primaryColor)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Friend Request Connection Alerts", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = notifyFriends,
                            onCheckedChange = { notifyFriends = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = currentPalette.primaryColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Email Transport Backend Server Status Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, contentDescription = "Email Transport", tint = currentPalette.primaryColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Email Transport Integration",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Server: smtp.talepulse.net (Port 587)", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Encryption: TLS 1.3 Active", fontSize = 12.sp, color = currentPalette.primaryColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Auto-mirroring chat dispatches to inbox verified.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Logout Button
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Logout",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sign Out Account",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Edit Profile Dialog
        if (showEditProfileDialog) {
            AlertDialog(
                onDismissRequest = { showEditProfileDialog = false },
                title = { Text("Edit Profile Details") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editDisplayName,
                            onValueChange = { editDisplayName = it },
                            label = { Text("Display Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = editStatusMessage,
                            onValueChange = { editStatusMessage = it },
                            label = { Text("Status Message") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onUpdateProfile(editDisplayName, editStatusMessage)
                            showEditProfileDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = currentPalette.primaryColor)
                    ) {
                        Text("Save Changes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditProfileDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

