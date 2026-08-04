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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Teal500
import com.example.util.UserPinManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AuthStep {
    OPTIONS,
    EMAIL_INPUT,
    SET_PIN,
    ENTER_PIN,
    NAME_INPUT
}

@Composable
fun AuthScreen(
    onLoginSubmit: (email: String, displayName: String, username: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(AuthStep.EMAIL_INPUT) }
    
    var email by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var loginPin by remember { mutableStateOf("") }
    var isWarningAcknowledged by remember { mutableStateOf(false) }
    var showPinVisible by remember { mutableStateOf(false) }

    var displayName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

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
            // App Branding Header (Linko Leaf Logo)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_linko_logo),
                    contentDescription = "Linko Logo",
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Linko",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp
            )

            Text(
                text = "End-to-end encrypted messaging with permanent access PIN security",
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
                        // STEP 1: EMAIL INPUT & AUTOMATIC PIN ROUTING
                        // -------------------------------------------------------------
                        AuthStep.OPTIONS, AuthStep.EMAIL_INPUT -> {
                            Text(
                                text = "Sign in to Linko",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Enter your email address to access your permanent Linko account.",
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

                            if (errorMessage != null && !errorMessage!!.contains("credential", ignoreCase = true)) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = errorMessage!!,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    val trimmed = email.trim()
                                    if (trimmed.isEmpty() || !trimmed.contains("@")) {
                                        errorMessage = "Please enter a valid email address."
                                    } else {
                                        errorMessage = null
                                        if (UserPinManager.isPinSet(context, trimmed)) {
                                            loginPin = ""
                                            currentStep = AuthStep.ENTER_PIN
                                        } else {
                                            newPin = ""
                                            confirmPin = ""
                                            isWarningAcknowledged = false
                                            currentStep = AuthStep.SET_PIN
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text(
                                    text = "Continue with Email",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        // -------------------------------------------------------------
                        // STEP 2A: FIRST-TIME REGISTRATION - SET PERMANENT PIN
                        // -------------------------------------------------------------
                        AuthStep.SET_PIN -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { currentStep = AuthStep.EMAIL_INPUT }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                                }
                                Text(
                                    text = "Set Your Permanent PIN",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Configure a permanent security PIN for:",
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

                            // PROMINENT CRITICAL WARNING BOX
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F).copy(alpha = 0.22f)),
                                border = BorderStroke(1.dp, Color(0xFFEF5350)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Warning",
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "WARNING: THIS PIN IS PERMANENT",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFFFF5252)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "There is NO 'Forgot PIN' or recovery option. If you forget this PIN, you will lose access to your Linko account forever. You MUST take a screenshot or write it down immediately!",
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Mandatory Checkbox
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isWarningAcknowledged = !isWarningAcknowledged }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isWarningAcknowledged,
                                    onCheckedChange = { isWarningAcknowledged = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Emerald500,
                                        uncheckedColor = Color.LightGray
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "I understand that this PIN cannot be recovered if forgotten.",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // PIN Input
                            OutlinedTextField(
                                value = newPin,
                                onValueChange = {
                                    newPin = it
                                    errorMessage = null
                                },
                                label = { Text("Enter Permanent PIN (min 4 digits)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Emerald500)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { showPinVisible = !showPinVisible }) {
                                        Icon(
                                            imageVector = if (showPinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle PIN Visibility",
                                            tint = Color.Gray
                                        )
                                    }
                                },
                                visualTransformation = if (showPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.NumberPassword,
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

                            Spacer(modifier = Modifier.height(10.dp))

                            // Confirm PIN Input
                            OutlinedTextField(
                                value = confirmPin,
                                onValueChange = {
                                    confirmPin = it
                                    errorMessage = null
                                },
                                label = { Text("Confirm Permanent PIN") },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Emerald500)
                                },
                                visualTransformation = if (showPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.NumberPassword,
                                    imeAction = ImeAction.Done
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
                                Spacer(modifier = Modifier.height(10.dp))
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
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    if (!isWarningAcknowledged) {
                                        errorMessage = "Please tick the mandatory checkbox acknowledging no PIN recovery."
                                    } else if (newPin.trim().length < 4) {
                                        errorMessage = "PIN must be at least 4 digits long."
                                    } else if (newPin.trim() != confirmPin.trim()) {
                                        errorMessage = "PINs do not match. Please re-enter."
                                    } else {
                                        UserPinManager.saveUserPin(context, email, newPin)
                                        errorMessage = null
                                        if (displayName.isBlank()) {
                                            val emailPrefix = email.substringBefore("@").replace(".", " ")
                                            displayName = emailPrefix.split(" ").joinToString(" ") { name ->
                                                name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                            }
                                        }
                                        currentStep = AuthStep.NAME_INPUT
                                    }
                                },
                                enabled = isWarningAcknowledged && newPin.isNotBlank() && confirmPin.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text(
                                    text = "Set PIN and Login",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        // -------------------------------------------------------------
                        // STEP 2B: SUBSEQUENT LOGINS - ENTER PERMANENT PIN
                        // -------------------------------------------------------------
                        AuthStep.ENTER_PIN -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { currentStep = AuthStep.EMAIL_INPUT }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                                }
                                Text(
                                    text = "Enter Your Access PIN",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Enter your permanent security PIN for:",
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

                            // Permanent Warning Reminder
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF374151)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Emerald500,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Remember: There is NO 'Forgot PIN' or recovery option for Linko accounts.",
                                        fontSize = 12.sp,
                                        color = Color(0xFFFFB74D),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            OutlinedTextField(
                                value = loginPin,
                                onValueChange = {
                                    loginPin = it
                                    errorMessage = null
                                },
                                label = { Text("Enter Your Permanent PIN") },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Emerald500)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { showPinVisible = !showPinVisible }) {
                                        Icon(
                                            imageVector = if (showPinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle PIN Visibility",
                                            tint = Color.Gray
                                        )
                                    }
                                },
                                visualTransformation = if (showPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.NumberPassword,
                                    imeAction = ImeAction.Done
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
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Error",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = errorMessage!!,
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    if (loginPin.isBlank()) {
                                        errorMessage = "Please enter your permanent PIN."
                                    } else {
                                        val isValid = UserPinManager.verifyUserPin(context, email, loginPin)
                                        if (isValid) {
                                            errorMessage = null
                                            val emailPrefix = email.substringBefore("@").replace(".", " ")
                                            val finalName = if (displayName.isNotBlank()) displayName else emailPrefix.split(" ").joinToString(" ") { name ->
                                                name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                            }
                                            val cleanEmail = email.substringBefore("@").lowercase().filter { it.isLetterOrDigit() || it == '_' }
                                            val autoUsername = "${cleanEmail}_user"
                                            onLoginSubmit(email.trim(), finalName, autoUsername)
                                        } else {
                                            errorMessage = "Incorrect PIN. Remember, there is no recovery option."
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text(
                                    text = "Verify PIN & Login",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
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
                                text = "PIN Verified! Enter your name to generate your unique Linko profile & QR identity.",
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
