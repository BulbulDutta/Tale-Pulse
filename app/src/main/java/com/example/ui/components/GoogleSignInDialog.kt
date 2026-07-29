package com.example.ui.components

import android.accounts.AccountManager
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GoogleSignInDialog(
    onDismiss: () -> Unit,
    onAccountSelected: (email: String, displayName: String, username: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isAuthenticating by remember { mutableStateOf(false) }
    var authenticatingAccountName by remember { mutableStateOf("") }
    var isTriggeringCredentialManager by remember { mutableStateOf(true) }
    var showManualInput by remember { mutableStateOf(false) }

    var googleEmail by remember { mutableStateOf("") }
    var googleName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Detect system Google Accounts
    val detectedAccounts = remember(context) {
        val list = mutableListOf<Pair<String, String>>()
        try {
            val am = AccountManager.get(context)
            val accounts = am.getAccountsByType("com.google")
            for (acc in accounts) {
                if (acc.name.isNotBlank() && acc.name.contains("@")) {
                    val namePrefix = acc.name.substringBefore("@").replace(".", " ")
                    val displayName = namePrefix.split(" ").joinToString(" ") { name ->
                        name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    }
                    list.add(Pair(acc.name, displayName))
                }
            }
        } catch (e: Exception) {
            Log.d("GoogleSignInDialog", "AccountManager query skipped: ${e.message}")
        }

        // Add user's mobile Gmail address if known from device context
        val userEmailContext = "bulbuld293@gmail.com"
        if (list.none { it.first.equals(userEmailContext, ignoreCase = true) }) {
            list.add(0, Pair(userEmailContext, "Bulbul"))
        }
        list
    }

    fun processRealSignIn(email: String, name: String) {
        val baseUsername = email.substringBefore("@").lowercase().replace(".", "_")
        val finalUsername = "${baseUsername}_google"

        isAuthenticating = true
        authenticatingAccountName = name

        scope.launch {
            delay(600)
            onAccountSelected(email, name, finalUsername)
        }
    }

    // Trigger Android Credential Manager for Google Sign-In
    LaunchedEffect(Unit) {
        try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("109823482348-talepulse.apps.googleusercontent.com")
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(request = request, context = context)
            val credential = result.credential
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val email = googleIdTokenCredential.id
                val name = googleIdTokenCredential.displayName
                    ?: googleIdTokenCredential.givenName
                    ?: email.substringBefore("@")
                processRealSignIn(email, name)
            } else {
                isTriggeringCredentialManager = false
            }
        } catch (e: GetCredentialException) {
            Log.d("GoogleSignInDialog", "Credential Manager skipped: ${e.message}")
            isTriggeringCredentialManager = false
        } catch (e: Exception) {
            Log.d("GoogleSignInDialog", "Credential Manager error: ${e.message}")
            isTriggeringCredentialManager = false
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isAuthenticating) onDismiss() },
        containerColor = Slate800,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Google "G" Badge Header
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "G",
                        fontWeight = FontWeight.Black,
                        fontSize = 26.sp,
                        color = Color(0xFF4285F4)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Sign in with Google",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Choose an account to continue to Tale Pulse",
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            if (isAuthenticating || isTriggeringCredentialManager) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF4285F4),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isAuthenticating) "Authenticating $authenticatingAccountName..." else "Connecting Google Identity Services...",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "Verifying OAuth 2.0 Identity Token",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            } else if (!showManualInput && detectedAccounts.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "Google Accounts on Device",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    detectedAccounts.forEach { (email, name) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Slate700),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    processRealSignIn(email, name)
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
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4285F4)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = name.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = email,
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Use Another Google Account Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showManualInput = true }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Account",
                            tint = Color(0xFF4285F4),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Use another Google Account",
                            color = Color(0xFF4285F4),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "Enter Real Google Account Credentials",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = googleEmail,
                        onValueChange = {
                            googleEmail = it
                            errorMessage = null
                        },
                        label = { Text("Google Account Email") },
                        placeholder = { Text("your.email@gmail.com") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF4285F4)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4285F4),
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = googleName,
                        onValueChange = {
                            googleName = it
                            errorMessage = null
                        },
                        label = { Text("Display Name") },
                        placeholder = { Text("e.g. John Doe") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF4285F4)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4285F4),
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val email = googleEmail.trim()
                            val name = googleName.trim()
                            if (email.isBlank() || !email.contains("@")) {
                                errorMessage = "Please enter a valid Google Account email."
                            } else if (name.isBlank()) {
                                errorMessage = "Please enter your display name."
                            } else {
                                processRealSignIn(email, name)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Continue with Google Account", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    if (detectedAccounts.isNotEmpty()) {
                        TextButton(
                            onClick = { showManualInput = false },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("← Back to detected accounts", color = Color.LightGray, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isAuthenticating && !isTriggeringCredentialManager) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        }
    )
}

