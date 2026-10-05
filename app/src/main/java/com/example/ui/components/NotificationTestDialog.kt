package com.example.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.MedicalPrimary
import com.example.ui.theme.MedicalPrimaryLight
import com.example.ui.theme.SlateSecondary
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessBg
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningBg
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.SurfaceWhite
import com.example.util.ClinicalNotificationHelper

@Composable
fun NotificationTestDialog(
    initialEmail: String = "1818phani@gmail.com",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var testEmail by remember { mutableStateOf(initialEmail) }
    var openExternalEmailApp by remember { mutableStateOf(false) }
    var lastStatusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessStatus by remember { mutableStateOf(true) }

    var hasPermission by remember {
        mutableStateOf(ClinicalNotificationHelper.hasNotificationPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            lastStatusMessage = "Notification permission granted! System alerts enabled."
            isSuccessStatus = true
        } else {
            lastStatusMessage = "Notification permission denied. In-app banner will still be shown."
            isSuccessStatus = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MedicalPrimaryLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MedicalPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Notification & Email Test",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DarkNavy
                    )
                    Text(
                        text = "Verify real-time mobile and email alerts",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateSecondary
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Permission Status Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (hasPermission) StatusSuccessBg else StatusWarningBg)
                        .border(
                            1.dp,
                            if (hasPermission) StatusSuccess.copy(alpha = 0.3f) else StatusWarning.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (hasPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (hasPermission) StatusSuccess else StatusWarning,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (hasPermission) "Mobile Alerts Active (System Status Bar)" else "System Permission Required for Tray",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (hasPermission) StatusSuccess else StatusWarning
                            )
                        }
                        if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Spacer(modifier = Modifier.width(6.dp))
                            TextButton(
                                onClick = {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Allow", color = MedicalPrimary, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: Mobile Notification
                Text(
                    text = "1. MOBILE NOTIFICATION",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                    color = SlateSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        val posted = ClinicalNotificationHelper.showTestMobileNotification(
                            context = context,
                            title = "MedVision AI • Diagnostic Alert",
                            message = "High-priority test notification successfully received on this mobile device."
                        )
                        isSuccessStatus = true
                        lastStatusMessage = if (posted) {
                            "Mobile notification sent to notification tray & top banner!"
                        } else {
                            "In-app notification banner shown. (Enable system notification permission for status bar)."
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("btn_test_mobile_notification")
                ) {
                    Icon(Icons.Default.Smartphone, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trigger Mobile Notification", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = BorderSubtle)
                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: Email Notification
                Text(
                    text = "2. EMAIL NOTIFICATION",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                    color = SlateSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = testEmail,
                    onValueChange = { testEmail = it },
                    label = { Text("Recipient Email") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = SlateSecondary, modifier = Modifier.size(18.dp))
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_test_notification_email"),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MedicalPrimary,
                        unfocusedBorderColor = BorderSubtle
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Checkbox(
                        checked = openExternalEmailApp,
                        onCheckedChange = { openExternalEmailApp = it },
                        colors = CheckboxDefaults.colors(checkedColor = MedicalPrimary),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Also launch Gmail / default email client",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = SlateSecondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val result = ClinicalNotificationHelper.showTestEmailNotification(
                            context = context,
                            recipientEmail = testEmail.trim(),
                            openEmailClient = openExternalEmailApp
                        )
                        isSuccessStatus = result.isVerified
                        lastStatusMessage = result.statusMessage
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.StatusSuccess),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("btn_test_email_notification")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verify & Send Email Notification", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }

                // Feedback status message
                if (lastStatusMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSuccessStatus) StatusSuccessBg else StatusWarningBg)
                            .border(
                                1.dp,
                                if (isSuccessStatus) StatusSuccess.copy(alpha = 0.3f) else StatusWarning.copy(alpha = 0.3f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Text(
                            text = lastStatusMessage!!,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                            color = if (isSuccessStatus) StatusSuccess else StatusWarning
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    ClinicalNotificationHelper.showTestMobileNotification(
                        context = context,
                        title = "MedVision AI • Combined Alert",
                        message = "Mobile and Email alert channel test triggered for $testEmail."
                    )
                    val result = ClinicalNotificationHelper.showTestEmailNotification(
                        context = context,
                        recipientEmail = testEmail.trim(),
                        openEmailClient = openExternalEmailApp
                    )
                    isSuccessStatus = result.isVerified
                    lastStatusMessage = result.statusMessage
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimary),
                modifier = Modifier.testTag("btn_run_combined_test")
            ) {
                Text("Test Both (Mobile + Email)", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = SlateSecondary)
            }
        }
    )
}
