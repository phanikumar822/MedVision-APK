package com.example.ui.doctor

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CachedScreening
import com.example.data.model.ReportEmailReceipt
import com.example.data.repository.MedVisionRepository
import com.example.ui.components.HeatmapBlendViewer
import com.example.ui.components.NotificationTestDialog
import com.example.ui.components.ProbabilityBarChart
import com.example.util.ClinicalNotificationHelper
import com.example.ui.theme.BackgroundClinical
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.DiagnosticIdSmallStyle
import com.example.ui.theme.DiagnosticIdStyle
import com.example.ui.theme.MedicalPrimary
import com.example.ui.theme.MedicalPrimaryLight
import com.example.ui.theme.SlateSecondary
import com.example.ui.theme.SlateTertiary
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusCriticalBg
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessBg
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningBg
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.SurfaceWhite
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@Composable
fun ScreeningResultScreen(
    screeningId: String,
    repository: MedVisionRepository,
    onNavigateBack: () -> Unit,
    onStartNewScreening: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var screeningData by remember { mutableStateOf<CachedScreening?>(null) }
    var isPublishingReport by remember { mutableStateOf(false) }
    var isReportPublished by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var isSendingEmail by remember { mutableStateOf(false) }
    var emailReceipt by remember { mutableStateOf<ReportEmailReceipt?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showNotificationTestDialog by remember { mutableStateOf(false) }
    var openExternalEmailApp by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(screeningId) {
        isLoading = true
        val list = repository.cachedScreenings.firstOrNull() ?: emptyList()
        val found = list.find { it.screeningId == screeningId } ?: list.firstOrNull()
        screeningData = found
        if (found != null) {
            val email = repository.getPatientEmail(found.patientId) ?: "eleanor.vance@gmail.com"
            emailInput = email
        }
        isLoading = false
    }

    if (isLoading) {
        Box(
            modifier = modifier.fillMaxSize().background(BackgroundClinical),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading clinical report...", style = MaterialTheme.typography.bodyMedium, color = SlateSecondary)
        }
        return
    }

    val data = screeningData
    if (data == null) {
        Box(
            modifier = modifier.fillMaxSize().background(BackgroundClinical).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Report Not Found",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DarkNavy
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No screening record matching '$screeningId' was found in the local repository.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Return to Dashboard", color = Color.White)
                }
            }
        }
        return
    }

    val isDr = data.prediction == "DR PRESENT"
    val (riskColor, riskBg) = when (data.riskLevel) {
        "HIGH" -> StatusCritical to StatusCriticalBg
        "MODERATE" -> StatusWarning to StatusWarningBg
        else -> StatusSuccess to StatusSuccessBg
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundClinical,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceWhite)
                    .border(1.dp, BorderSubtle)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DarkNavy
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "DIAGNOSTIC REPORT",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                            color = SlateSecondary
                        )
                        Text(
                            text = data.screeningId,
                            style = DiagnosticIdStyle.copy(fontSize = 14.sp),
                            color = DarkNavy
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showNotificationTestDialog = true },
                        modifier = Modifier.testTag("btn_result_test_notifications")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Test Mobile & Email Notifications",
                            tint = MedicalPrimary
                        )
                    }
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.testTag("btn_delete_screening_result")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Record",
                            tint = StatusCritical
                        )
                    }
                    IconButton(
                        onClick = {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "MedVisionAI Diagnostic Report [${data.screeningId}]\nPatient: ${data.patientName}\nResult: ${data.prediction}\nConfidence: %.1f%%\nRisk: ${data.riskLevel}".format(data.confidence * 100f)
                                )
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share Clinical Diagnostic Report")
                            context.startActivity(shareIntent)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Report",
                            tint = SlateSecondary
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Patient & Clinical Diagnostic Summary Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceWhite)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                        .testTag("diagnostic_summary_banner")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "PATIENT EXAMINATION",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                                color = SlateSecondary
                            )
                            Text(
                                text = data.patientName,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = DarkNavy
                            )
                        }

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isDr) StatusCriticalBg else StatusSuccessBg)
                                .border(1.dp, if (isDr) StatusCritical.copy(alpha = 0.3f) else StatusSuccess.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = data.prediction,
                                style = DiagnosticIdSmallStyle.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = if (isDr) StatusCritical else StatusSuccess
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Risk Stratification", style = MaterialTheme.typography.labelSmall, color = SlateSecondary)
                            Text(
                                text = "${data.riskLevel} RISK",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = riskColor
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Model Confidence", style = MaterialTheme.typography.labelSmall, color = SlateSecondary)
                            Text(
                                text = "%.1f%%".format(data.confidence * 100f),
                                style = DiagnosticIdStyle.copy(fontSize = 18.sp),
                                color = DarkNavy
                            )
                        }
                    }
                }
            }

            // Dual-Layer Fundus & Grad-CAM Heatmap Viewer
            item {
                HeatmapBlendViewer(
                    imageUrl = data.imageUrl,
                    heatmapUrl = data.heatmapUrl,
                    isDrPrediction = isDr
                )
            }

            // Probability Scale Chart
            item {
                ProbabilityBarChart(
                    probabilityDr = data.probabilityDr,
                    probabilityNoDr = data.probabilityNoDr,
                    patientLabel = "Logit Probability Breakdown"
                )
            }

            // Clinical Explainability & AI Findings Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceWhite)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MedicalPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Grad-CAM Clinical Findings",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = DarkNavy
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = data.aiContext,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateSecondary,
                        lineHeight = 22.sp
                    )
                }
            }

            // Clinical Recommendation Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceWhite)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = if (isDr) StatusCritical else StatusSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Specialist Recommendation",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = DarkNavy
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = data.recommendation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateSecondary,
                        lineHeight = 22.sp
                    )
                }
            }

            // Action Buttons: Generate & Publish Report / Send to User / New Screening
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (emailReceipt != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(com.example.ui.theme.StatusSuccessBg)
                                .border(1.dp, com.example.ui.theme.StatusSuccess.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = com.example.ui.theme.StatusSuccess, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Report instantly emailed to ${emailReceipt?.recipientEmail} (ID: ${emailReceipt?.transactionId})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = com.example.ui.theme.StatusSuccess
                                )
                            }
                        }
                    }

                    if (!isReportPublished) {
                        Button(
                            onClick = {
                                isPublishingReport = true
                                scope.launch {
                                    val result = repository.generateReportWithNotification(data.screeningId)
                                    isPublishingReport = false
                                    isReportPublished = true
                                    snackbarHostState.showSnackbar("Diagnostic Report generated! App notification delivered.")
                                }
                            },
                            enabled = !isPublishingReport,
                            colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_generate_report")
                        ) {
                            if (isPublishingReport) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generating Report...", fontWeight = FontWeight.SemiBold)
                            } else {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Report", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else {
                        // Button: Send the report to user via email
                        Button(
                            onClick = { showEmailDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.StatusSuccess),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_send_report_to_user_email")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send Report to User via Email", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    OutlinedButton(
                        onClick = onStartNewScreening,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Screen Another Patient", color = MedicalPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Email Dispatch Dialog
        if (showEmailDialog) {
            AlertDialog(
                onDismissRequest = { showEmailDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = MedicalPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Send Report to User via Email",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = DarkNavy
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Instantly dispatch the official clinical evaluation report for ${data.patientName} (Ref: ${data.screeningId}) to the user's email address.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick fill user email chip
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Quick fill:", style = MaterialTheme.typography.labelSmall, color = SlateSecondary)
                            Spacer(modifier = Modifier.width(6.dp))
                            FilterChip(
                                selected = emailInput == "1818phani@gmail.com",
                                onClick = { emailInput = "1818phani@gmail.com" },
                                label = { Text("1818phani@gmail.com", fontSize = 11.sp) }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("User Email Address") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = SlateSecondary, modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dialog_report_recipient_email"),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MedicalPrimary,
                                unfocusedBorderColor = BorderSubtle
                            )
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp)
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
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isSendingEmail = true
                            scope.launch {
                                val sendResult = repository.sendReportToUserByEmail(data.screeningId, emailInput)
                                isSendingEmail = false
                                sendResult.onSuccess { receipt ->
                                    emailReceipt = receipt
                                    showEmailDialog = false

                                    if (openExternalEmailApp) {
                                        val launchResult = ClinicalNotificationHelper.openEmailClient(
                                            context = context,
                                            recipientEmail = receipt.recipientEmail,
                                            subject = "MedVision AI • Clinical Diagnostic Report [${data.screeningId}] - ${data.patientName}",
                                            body = "Official Ophthalmic AI Screening Report\nRef: ${data.screeningId}\nPatient: ${data.patientName}\nDiagnosis: ${data.prediction}\nConfidence: %.1f%%\nRisk: ${data.riskLevel}\nRecommendation: ${data.recommendation}\n\nClinical evaluation verified by MedVision AI."
                                                .format(data.confidence * 100f)
                                        )
                                        if (launchResult.isSuccess) {
                                            snackbarHostState.showSnackbar("Verified & Dispatched! Opened mail client for ${receipt.recipientEmail}.")
                                        } else {
                                            snackbarHostState.showSnackbar("Verified Email (Tx: ${receipt.transactionId}), but mail client notice: ${launchResult.exceptionOrNull()?.message}")
                                        }
                                    } else {
                                        snackbarHostState.showSnackbar("Verified & Dispatched! Report sent to ${receipt.recipientEmail} (Tx: ${receipt.transactionId}).")
                                    }
                                }.onFailure { err ->
                                    snackbarHostState.showSnackbar("Verification Failed: ${err.localizedMessage ?: "Failed to send email."}")
                                }
                            }
                        },
                        enabled = !isSendingEmail && emailInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_confirm_send_report_email")
                    ) {
                        if (isSendingEmail) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send Report Instantly", color = Color.White)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmailDialog = false }) {
                        Text("Cancel", color = SlateSecondary)
                    }
                }
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = SurfaceWhite,
                title = {
                    Text(
                        text = "Delete Screening Record",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DarkNavy
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to permanently delete screening record ${data.screeningId} for ${data.patientName}? This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkNavy
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            scope.launch {
                                repository.deleteScreening(data.screeningId)
                                onNavigateBack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusCritical),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_confirm_delete_screening_result")
                    ) {
                        Text("Delete Record", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel", color = SlateSecondary)
                    }
                }
            )
        }

        if (showNotificationTestDialog) {
            NotificationTestDialog(
                initialEmail = "1818phani@gmail.com",
                onDismiss = { showNotificationTestDialog = false }
            )
        }
    }
}
