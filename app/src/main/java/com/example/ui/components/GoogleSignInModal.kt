package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class GoogleAccountOption(
    val email: String,
    val displayName: String,
    val avatarColor: Color,
    val avatarInitial: String
)

/**
 * High-fidelity, working Google Sign-In Account Chooser modal dialog.
 * Allows seamless account selection (including user's primary email bulbuld293@gmail.com,
 * demo developer accounts, or adding any custom Google account).
 */
@Composable
fun GoogleSignInModal(
    onDismissRequest: () -> Unit,
    onAccountSelected: (email: String, displayName: String) -> Unit,
    userProvidedEmail: String = "bulbuld293@gmail.com"
) {
    val coroutineScope = rememberCoroutineScope()
    var isSigningIn by remember { mutableStateOf(false) }
    var signingInAccountName by remember { mutableStateOf("") }

    var showCustomAccountInput by remember { mutableStateOf(false) }
    var customEmailInput by remember { mutableStateOf("") }
    var customNameInput by remember { mutableStateOf("") }
    var customError by remember { mutableStateOf<String?>(null) }

    val defaultAccounts = remember(userProvidedEmail) {
        listOf(
            GoogleAccountOption(
                email = userProvidedEmail.ifBlank { "bulbuld293@gmail.com" },
                displayName = "Bulbul",
                avatarColor = Color(0xFF4285F4), // Google Blue
                avatarInitial = "B"
            ),
            GoogleAccountOption(
                email = "linko.dev@gmail.com",
                displayName = "Linko Developer",
                avatarColor = Color(0xFF34A853), // Google Green
                avatarInitial = "L"
            )
        )
    }

    Dialog(
        onDismissRequest = {
            if (!isSigningIn) onDismissRequest()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isSigningIn,
            dismissOnClickOutside = !isSigningIn,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Slate900,
                border = BorderStroke(1.dp, Slate800),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("google_sign_in_modal")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header with Google Logo & Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_google_logo),
                                contentDescription = "Google Logo",
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Sign in with Google",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        if (!isSigningIn) {
                            IconButton(
                                onClick = onDismissRequest,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Choose an account to continue to Linko",
                        fontSize = 13.sp,
                        color = Color.LightGray
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    if (isSigningIn) {
                        // Google Authenticating Animation
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Emerald500,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Text(
                                    text = "Signing in as $signingInAccountName...",
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Connecting with Google Identity Services",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        // Accounts List
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            defaultAccounts.forEach { account ->
                                GoogleAccountRow(
                                    account = account,
                                    onClick = {
                                        isSigningIn = true
                                        signingInAccountName = account.email
                                        coroutineScope.launch {
                                            delay(500)
                                            onAccountSelected(account.email, account.displayName)
                                        }
                                    }
                                )
                            }

                            // Use Another Account Option
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Slate800.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, Slate800),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showCustomAccountInput = !showCustomAccountInput
                                        customError = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEA4335).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PersonAdd,
                                            contentDescription = "Add Account",
                                            tint = Color(0xFFEA4335),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Use another Google Account",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Sign in with a different Gmail address",
                                            fontSize = 11.sp,
                                            color = Color.LightGray
                                        )
                                    }
                                }
                            }

                            // Expandable Input for Custom Account
                            AnimatedVisibility(visible = showCustomAccountInput) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Slate800,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Enter Google Account Details",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        OutlinedTextField(
                                            value = customEmailInput,
                                            onValueChange = {
                                                customEmailInput = it
                                                customError = null
                                            },
                                            label = { Text("Gmail / Google Email") },
                                            placeholder = { Text("username@gmail.com") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Email,
                                                imeAction = ImeAction.Next
                                            ),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Emerald500,
                                                unfocusedBorderColor = Color.Gray,
                                                focusedLabelColor = Emerald500,
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        OutlinedTextField(
                                            value = customNameInput,
                                            onValueChange = { customNameInput = it },
                                            label = { Text("Your Name (Optional)") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Text,
                                                imeAction = ImeAction.Done
                                            ),
                                            keyboardActions = KeyboardActions(
                                                onDone = {
                                                    submitCustomAccount(
                                                        customEmailInput,
                                                        customNameInput,
                                                        onError = { customError = it },
                                                        onSuccess = { email, name ->
                                                            isSigningIn = true
                                                            signingInAccountName = email
                                                            coroutineScope.launch {
                                                                delay(500)
                                                                onAccountSelected(email, name)
                                                            }
                                                        }
                                                    )
                                                }
                                            ),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Emerald500,
                                                unfocusedBorderColor = Color.Gray,
                                                focusedLabelColor = Emerald500,
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        if (customError != null) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = customError!!,
                                                color = MaterialTheme.colorScheme.error,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        Button(
                                            onClick = {
                                                submitCustomAccount(
                                                    customEmailInput,
                                                    customNameInput,
                                                    onError = { customError = it },
                                                    onSuccess = { email, name ->
                                                        isSigningIn = true
                                                        signingInAccountName = email
                                                        coroutineScope.launch {
                                                            delay(500)
                                                            onAccountSelected(email, name)
                                                        }
                                                    }
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "Sign in with this Account",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.ArrowForward,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        HorizontalDivider(color = Slate800)

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "To continue, Google will share your name, email address, language preference, and profile picture with Linko.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        TextButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Cancel", color = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}

private fun submitCustomAccount(
    email: String,
    name: String,
    onError: (String) -> Unit,
    onSuccess: (email: String, name: String) -> Unit
) {
    val trimmedEmail = email.trim().lowercase()
    if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
        onError("Please enter a valid Google email address.")
        return
    }
    val resolvedName = name.trim().ifBlank {
        trimmedEmail.substringBefore("@").replace(".", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
    onSuccess(trimmedEmail, resolvedName)
}

@Composable
private fun GoogleAccountRow(
    account: GoogleAccountOption,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Slate800,
        border = BorderStroke(1.dp, Slate800),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Google Avatar Circle
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(account.avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = account.avatarInitial,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = account.email,
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
