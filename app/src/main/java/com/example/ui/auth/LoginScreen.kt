package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.BiometricLaserScanner
import com.example.ui.theme.BackgroundClinical
import com.example.ui.theme.BorderStrong
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.MedicalPrimary
import com.example.ui.theme.MedicalPrimaryDark
import com.example.ui.theme.SlateSecondary
import com.example.ui.theme.SlateTertiary
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusCriticalBg
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessBg
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.SurfaceWhite

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onNavigateToDoctor: () -> Unit,
    onNavigateToPatient: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.LoginSuccess -> {
                    if (event.role == "PATIENT") {
                        onNavigateToPatient()
                    } else {
                        onNavigateToDoctor()
                    }
                }
                is LoginEvent.ShowToast -> {}
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundClinical)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Professional Hospital / Ophthalmology Brand Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.testTag("brand_header")
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MedicalPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = "MedVision Logo",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "MedVision AI",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        ),
                        color = DarkNavy
                    )
                    Text(
                        text = "Clinical Retinal Screening System",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Role Segmented Switcher: Clinician vs Patient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceSubtle)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                    .padding(3.dp)
                    .testTag("portal_segment_toggle")
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Doctor Portal Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (uiState.isDoctorPortal) SurfaceWhite else Color.Transparent)
                            .then(
                                if (uiState.isDoctorPortal) Modifier.border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                                else Modifier
                            )
                            .clickable { viewModel.onPortalToggled(true) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Clinician Portal",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (uiState.isDoctorPortal) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (uiState.isDoctorPortal) DarkNavy else SlateSecondary
                        )
                    }

                    // Patient Portal Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!uiState.isDoctorPortal) SurfaceWhite else Color.Transparent)
                            .then(
                                if (!uiState.isDoctorPortal) Modifier.border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                                else Modifier
                            )
                            .clickable { viewModel.onPortalToggled(false) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Patient Portal",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (!uiState.isDoctorPortal) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (!uiState.isDoctorPortal) DarkNavy else SlateSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Clean Authentication Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceWhite)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                    .padding(24.dp)
            ) {
                Text(
                    text = if (uiState.isDoctorPortal) "Clinician Sign In" else "Patient Access",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DarkNavy
                )
                Text(
                    text = if (uiState.isDoctorPortal)
                        "Enter your hospital credentials to access patient screenings."
                    else
                        "Access your ophthalmic screening results and reports.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                // Identifier Label & Input
                Text(
                    text = if (uiState.isDoctorPortal) "Username or NPI" else "Health Record ID",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = DarkNavy,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = uiState.username,
                    onValueChange = { viewModel.onUsernameChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_input"),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = SlateSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MedicalPrimary,
                        unfocusedBorderColor = BorderSubtle,
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite,
                        focusedTextColor = DarkNavy,
                        unfocusedTextColor = DarkNavy
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Label & Input
                Text(
                    text = "Password",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = DarkNavy,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = { viewModel.onPasswordChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input"),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = SlateSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide Password" else "Show Password",
                                tint = SlateSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { viewModel.login() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MedicalPrimary,
                        unfocusedBorderColor = BorderSubtle,
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite,
                        focusedTextColor = DarkNavy,
                        unfocusedTextColor = DarkNavy
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                // Success message alert banner
                AnimatedVisibility(visible = uiState.successMessage != null) {
                    uiState.successMessage?.let { success ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(StatusSuccessBg)
                                .border(1.dp, StatusSuccess.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusSuccess,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = success,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StatusSuccess
                                )
                            }
                        }
                    }
                }

                // Error message alert banner
                AnimatedVisibility(visible = uiState.errorMessage != null) {
                    uiState.errorMessage?.let { error ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(StatusCriticalBg)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = StatusCritical
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Row: Primary Login Button & Clean Biometric Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.login() },
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("login_submit_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MedicalPrimary,
                            disabledContainerColor = MedicalPrimary.copy(alpha = 0.5f)
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (uiState.isDoctorPortal) "Sign In as Clinician" else "Access Patient Portal",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    BiometricLaserScanner(
                        size = 48.dp,
                        onClick = { viewModel.authenticateWithBiometrics() }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Email Verification & Setup Trigger
                OutlinedButton(
                    onClick = { viewModel.onOpenVerificationDialog() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_verify_email_setup"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MedicalPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Verify Email & Set Up Login Credentials",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // HIPAA / Security compliance indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = SlateTertiary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "End-to-End Encrypted • HIPAA & DICOM Compliant",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateSecondary
                )
            }
        }

        // --- Dialog 1: Instant Email Verification ---
        if (uiState.showEmailVerificationModal) {
            AlertDialog(
                onDismissRequest = { viewModel.onCloseVerificationDialog() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = MedicalPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Email Account Verification",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = DarkNavy
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Enter your clinical or patient email address. An instant verification email will be dispatched with a secure link to configure your username and password.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = uiState.verificationEmailInput,
                            onValueChange = { viewModel.onVerificationEmailInputChanged(it) },
                            label = { Text("Email Address") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = SlateSecondary, modifier = Modifier.size(20.dp))
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("verification_email_input"),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MedicalPrimary,
                                unfocusedBorderColor = BorderSubtle
                            )
                        )

                        if (uiState.verificationEmailSent && uiState.generatedSetupLink != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StatusSuccessBg)
                                    .border(1.dp, StatusSuccess.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Verification Email Sent Instantly!",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = StatusSuccess
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Click the verification link below to configure your login username and password:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DarkNavy
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SurfaceWhite)
                                            .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                                            .clickable { viewModel.onOpenCredentialSetupFromLink() }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Link, contentDescription = null, tint = MedicalPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = uiState.generatedSetupLink ?: "",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                            color = MedicalPrimary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (uiState.verificationEmailSent) {
                        Button(
                            onClick = { viewModel.onOpenCredentialSetupFromLink() },
                            colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_open_setup_link")
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Link & Set Credentials", color = Color.White)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.sendInstantVerificationEmail() },
                            enabled = !uiState.isSendingVerificationEmail,
                            colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_send_verification_email")
                        ) {
                            if (uiState.isSendingVerificationEmail) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send Verification Email", color = Color.White)
                            }
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onCloseVerificationDialog() }) {
                        Text("Cancel", color = SlateSecondary)
                    }
                }
            )
        }

        // --- Dialog 2: Username & Password Credential Setup ---
        if (uiState.showCredentialSetupModal) {
            var setupPassVisible by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { viewModel.onCloseCredentialSetupModal() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = MedicalPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Set Up Login Credentials",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = DarkNavy
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Email verified: ${uiState.setupEmail}\nConfigure your permanent username and password. Both will be your login credentials.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateSecondary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Username Input
                        Text(
                            text = "Username",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = DarkNavy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = uiState.setupUsernameInput,
                            onValueChange = { viewModel.onSetupUsernameChanged(it) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = SlateSecondary, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_username_input"),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MedicalPrimary,
                                unfocusedBorderColor = BorderSubtle
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Password Input
                        Text(
                            text = "Password",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = DarkNavy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = uiState.setupPasswordInput,
                            onValueChange = { viewModel.onSetupPasswordChanged(it) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = SlateSecondary, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                IconButton(onClick = { setupPassVisible = !setupPassVisible }) {
                                    Icon(
                                        imageVector = if (setupPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = SlateSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            visualTransformation = if (setupPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_password_input"),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MedicalPrimary,
                                unfocusedBorderColor = BorderSubtle
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Confirm Password Input
                        Text(
                            text = "Confirm Password",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = DarkNavy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = uiState.setupConfirmPasswordInput,
                            onValueChange = { viewModel.onSetupConfirmPasswordChanged(it) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = SlateSecondary, modifier = Modifier.size(18.dp))
                            },
                            visualTransformation = if (setupPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_confirm_password_input"),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MedicalPrimary,
                                unfocusedBorderColor = BorderSubtle
                            )
                        )

                        // Error indicator
                        if (uiState.credentialSetupError != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = uiState.credentialSetupError ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusCritical
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.saveCredentialsAndCompleteSetup() },
                        enabled = !uiState.isSavingCredentials,
                        colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_save_setup_credentials")
                    ) {
                        if (uiState.isSavingCredentials) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Save Credentials & Login", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onCloseCredentialSetupModal() }) {
                        Text("Cancel", color = SlateSecondary)
                    }
                }
            )
        }
    }
}
