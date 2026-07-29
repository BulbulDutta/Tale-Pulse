package com.example.ui.screens.auth

/*
 * ====================================================================================
 * DEVELOPER GUIDANCE NOTE: AUTHENTICATION & OTP CONFIGURATION
 * ====================================================================================
 * To connect live Firebase Auth, Google Sign-In credentials, or EmailJS OTP APIs:
 * 1. Google Sign-In: Replace Google OAuth Client ID in google-services.json or
 *    obtain webClientId via BuildConfig.GOOGLE_WEB_CLIENT_ID.
 * 2. Email OTP Service (EmailJS / SendGrid / Firebase Auth):
 *    - Service ID: BuildConfig.EMAILJS_SERVICE_ID
 *    - Template ID: BuildConfig.EMAILJS_TEMPLATE_ID
 *    - Public Key: BuildConfig.EMAILJS_PUBLIC_KEY
 * ====================================================================================
 */

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GoogleSignInDialog
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Teal500
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class AuthStep {
    OPTIONS,
    EMAIL_INPUT,
    OTP_VERIFY,
    NAME_INPUT
}

@Composable
fun AuthScreen(
    onLoginSubmit: (email: String, displayName: String, username: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableStateOf(AuthStep.OPTIONS) }
    
    var email by remember { mutableStateOf("") }
    var generatedOtp by remember { mutableStateOf("") }
    
    // 4 OTP digits
    var otpDigit1 by remember { mutableStateOf("") }
    var otpDigit2 by remember { mutableStateOf("") }
    var otpDigit3 by remember { mutableStateOf("") }
    var otpDigit4 by remember { mutableStateOf("") }
    
    var displayName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    var showGoogleDialog by remember { mutableStateOf(false) }

    val focusRequester1 = remember { FocusRequester() }
    val focusRequester2 = remember { FocusRequester() }
    val focusRequester3 = remember { FocusRequester() }
    val focusRequester4 = remember { FocusRequester() }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    fun resetOtpForm() {
        otpDigit1 = ""
        otpDigit2 = ""
        otpDigit3 = ""
        otpDigit4 = ""
        errorMessage = null
        infoMessage = null
        isVerifying = false
    }

    fun fillOtp(code: String) {
        val digits = code.filter { it.isDigit() }.take(4)
        otpDigit1 = digits.getOrNull(0)?.toString() ?: ""
        otpDigit2 = digits.getOrNull(1)?.toString() ?: ""
        otpDigit3 = digits.getOrNull(2)?.toString() ?: ""
        otpDigit4 = digits.getOrNull(3)?.toString() ?: ""
        errorMessage = null
    }

    fun verifyOtp() {
        val entered = "$otpDigit1$otpDigit2$otpDigit3$otpDigit4"
        if (entered.length < 4) {
            errorMessage = "Please enter all 4 digits."
        } else if (entered != generatedOtp) {
            errorMessage = "Incorrect OTP code. Please try again or tap Auto-fill."
            otpDigit1 = ""
            otpDigit2 = ""
            otpDigit3 = ""
            otpDigit4 = ""
            scope.launch {
                delay(100)
                try { focusRequester1.requestFocus() } catch (_: Exception) {}
            }
        } else {
            errorMessage = null
            if (displayName.isBlank()) {
                val emailPrefix = email.substringBefore("@").replace(".", " ")
                displayName = emailPrefix.split(" ").joinToString(" ") { name ->
                    name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }
            }
            currentStep = AuthStep.NAME_INPUT
        }
    }

    if (showGoogleDialog) {
        GoogleSignInDialog(
            onDismiss = { showGoogleDialog = false },
            onAccountSelected = { gEmail, gName, gUsername ->
                showGoogleDialog = false
                onLoginSubmit(gEmail, gName, gUsername)
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Slate900),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Branding Header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Emerald500, Teal500))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = "Tale Pulse Logo",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tale Pulse",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp
            )

            Text(
                text = "End-to-end encrypted messaging with email routing & instant QR discovery",
                color = Color.LightGray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Main Auth Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    when (currentStep) {
                        // -------------------------------------------------------------
                        // STEP 0: CHOOSING AUTH METHOD (Google or Email)
                        // -------------------------------------------------------------
                        AuthStep.OPTIONS -> {
                            Text(
                                text = "Welcome to Tale Pulse",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Text(
                                text = "Choose your preferred sign in method to continue",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Google Sign In Button
                            Button(
                                onClick = {
                                    showGoogleDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Google Icon",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Sign in with Google",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Email Sign In Button
                            Button(
                                onClick = {
                                    errorMessage = null
                                    currentStep = AuthStep.EMAIL_INPUT
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = "Email Icon",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Sign in with Email",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Secured with OAuth 2.0 & E2E encryption",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }

                        // -------------------------------------------------------------
                        // STEP 1: EMAIL INPUT
                        // -------------------------------------------------------------
                        AuthStep.EMAIL_INPUT -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { currentStep = AuthStep.OPTIONS }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                                }
                                Text(
                                    text = "Step 1: Enter Email",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Enter your email address to receive a 4-digit verification code.",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    errorMessage = null
                                },
                                label = { Text("Email Address") },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = Emerald500)
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Emerald500,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedLabelColor = Emerald500,
                                    unfocusedLabelColor = Color.LightGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (errorMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = errorMessage!!,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    val trimmed = email.trim()
                                    if (trimmed.isEmpty() || !trimmed.contains("@")) {
                                        errorMessage = "Please enter a valid email address."
                                    } else {
                                        generatedOtp = (1000..9999).random().toString()
                                        resetOtpForm()
                                        currentStep = AuthStep.OTP_VERIFY
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text(
                                    text = "Send Verification Code",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        // -------------------------------------------------------------
                        // STEP 2: 4-DIGIT OTP VERIFICATION
                        // -------------------------------------------------------------
                        AuthStep.OTP_VERIFY -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { currentStep = AuthStep.EMAIL_INPUT }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                                }
                                Text(
                                    text = "Step 2: OTP Verification",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Enter the 4-digit security code sent to:",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            Text(
                                text = email,
                                color = Emerald500,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Demo OTP Notification Banner with Auto-Fill Action
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Emerald500.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { fillOtp(generatedOtp) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = Emerald500,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Verification Code Sent!",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = Emerald500
                                            )
                                            Text(
                                                text = "Your 4-Digit OTP is: $generatedOtp",
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 14.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    
                                    Button(
                                        onClick = { fillOtp(generatedOtp) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Auto-fill", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // 4 Separate OTP Digit Boxes with Paste/Auto advance support
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OtpDigitBox(
                                    value = otpDigit1,
                                    onValueChange = { input ->
                                        if (input.length > 1) {
                                            fillOtp(input)
                                        } else {
                                            otpDigit1 = input
                                            errorMessage = null
                                            if (input.isNotEmpty()) {
                                                try { focusRequester2.requestFocus() } catch (_: Exception) {}
                                            }
                                        }
                                    },
                                    focusRequester = focusRequester1,
                                    isError = errorMessage != null
                                )

                                OtpDigitBox(
                                    value = otpDigit2,
                                    onValueChange = { input ->
                                        if (input.length > 1) {
                                            fillOtp(input)
                                        } else {
                                            otpDigit2 = input
                                            errorMessage = null
                                            if (input.isNotEmpty()) {
                                                try { focusRequester3.requestFocus() } catch (_: Exception) {}
                                            } else {
                                                try { focusRequester1.requestFocus() } catch (_: Exception) {}
                                            }
                                        }
                                    },
                                    focusRequester = focusRequester2,
                                    isError = errorMessage != null
                                )

                                OtpDigitBox(
                                    value = otpDigit3,
                                    onValueChange = { input ->
                                        if (input.length > 1) {
                                            fillOtp(input)
                                        } else {
                                            otpDigit3 = input
                                            errorMessage = null
                                            if (input.isNotEmpty()) {
                                                try { focusRequester4.requestFocus() } catch (_: Exception) {}
                                            } else {
                                                try { focusRequester2.requestFocus() } catch (_: Exception) {}
                                            }
                                        }
                                    },
                                    focusRequester = focusRequester3,
                                    isError = errorMessage != null
                                )

                                OtpDigitBox(
                                    value = otpDigit4,
                                    onValueChange = { input ->
                                        if (input.length > 1) {
                                            fillOtp(input)
                                        } else {
                                            otpDigit4 = input
                                            errorMessage = null
                                            if (input.isEmpty()) {
                                                try { focusRequester3.requestFocus() } catch (_: Exception) {}
                                            }
                                        }
                                    },
                                    focusRequester = focusRequester4,
                                    isError = errorMessage != null
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Didn't receive code?",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                                TextButton(
                                    onClick = {
                                        generatedOtp = (1000..9999).random().toString()
                                        resetOtpForm()
                                    }
                                ) {
                                    Text("Resend Code", color = Emerald500, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            if (errorMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = errorMessage!!,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = { verifyOtp() },
                                enabled = !isVerifying,
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                if (isVerifying) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Text(
                                        text = "Verify Code",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }

                        // -------------------------------------------------------------
                        // STEP 3: NAME INPUT & USERNAME GENERATION
                        // -------------------------------------------------------------
                        AuthStep.NAME_INPUT -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified",
                                    tint = Emerald500,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Step 3: Profile Setup",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "OTP Verified! Enter your name to generate your unique profile & QR identity.",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            OutlinedTextField(
                                value = displayName,
                                onValueChange = {
                                    displayName = it
                                    errorMessage = null
                                },
                                label = { Text("Full Name") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Emerald500)
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Emerald500,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedLabelColor = Emerald500,
                                    unfocusedLabelColor = Color.LightGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            val autoUsername = remember(displayName, email) {
                                val cleanName = displayName.trim().lowercase().replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }
                                val cleanEmail = email.substringBefore("@").lowercase().filter { it.isLetterOrDigit() || it == '_' }
                                val base = if (cleanName.isNotBlank()) cleanName else cleanEmail
                                "${base}_user"
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Auto-Generated Username: @$autoUsername",
                                color = Teal500,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (errorMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = errorMessage!!,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    if (displayName.isBlank()) {
                                        errorMessage = "Please enter your full name."
                                    } else {
                                        onLoginSubmit(email.trim(), displayName.trim(), autoUsername)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text(
                                    text = "Complete Registration",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OtpDigitBox(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    isError: Boolean
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isError) MaterialTheme.colorScheme.error else Emerald500,
            unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error else Color.Gray,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        textStyle = androidx.compose.ui.text.TextStyle(
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            fontFamily = FontFamily.Monospace
        ),
        modifier = Modifier
            .width(58.dp)
            .height(64.dp)
            .focusRequester(focusRequester)
    )
}
