package com.example.ui.patient

import android.content.Intent
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.LocalChatMessage
import com.example.data.model.ReportItemDto
import com.example.ui.components.HeatmapBlendViewer
import com.example.ui.components.NotificationTestDialog
import com.example.ui.theme.BackgroundClinical
import com.example.ui.theme.BorderStrong
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.DiagnosticIdSmallStyle
import com.example.ui.theme.DiagnosticIdStyle
import com.example.ui.theme.MedicalPrimary
import com.example.ui.theme.MedicalPrimaryLight
import com.example.ui.theme.MedicalPrimarySubtle
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
import kotlinx.coroutines.launch

@Composable
fun PatientPortalScreen(
    viewModel: PatientViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()

    var selectedSectionIndex by remember { mutableIntStateOf(0) } // 0: Reports, 1: AI Assistant
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var reportToDelete by remember { mutableStateOf<ReportItemDto?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showNotificationTestDialog by remember { mutableStateOf(false) }

    if (reportToDelete != null) {
        val report = reportToDelete!!
        AlertDialog(
            onDismissRequest = { reportToDelete = null },
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
                    text = "Are you sure you want to permanently delete screening record ${report.screeningId}? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkNavy
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = report.screeningId
                        reportToDelete = null
                        viewModel.deleteReport(id)
                        scope.launch {
                            snackbarHostState.showSnackbar("Screening record $id deleted.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCritical),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_confirm_delete_report")
                ) {
                    Text("Delete Record", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { reportToDelete = null }) {
                    Text("Cancel", color = SlateSecondary)
                }
            }
        )
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            containerColor = SurfaceWhite,
            title = {
                Text(
                    text = "Delete All Patient Records",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = StatusCritical
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete ALL medical screening records and diagnostic history for this patient profile? This action is irreversible.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkNavy
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAllDialog = false
                        viewModel.deleteAllPatientRecords()
                        scope.launch {
                            snackbarHostState.showSnackbar("All patient records deleted.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCritical),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_confirm_delete_all_patient_records")
                ) {
                    Text("Delete All Records", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel", color = SlateSecondary)
                }
            }
        )
    }

    if (showApiKeyDialog) {
        var apiKeyText by remember { mutableStateOf(viewModel.getCustomApiKey() ?: "") }
        val hasCustomKey = !viewModel.getCustomApiKey().isNullOrBlank()

        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            containerColor = SurfaceWhite,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = MedicalPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Insert Gemini API Key",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DarkNavy
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Connect Google Gemini API to power the Clinical AI Chatbot for answering retinal exam inquiries and health recommendations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (viewModel.isGeminiConfigured()) StatusSuccessBg else StatusWarningBg)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (viewModel.isGeminiConfigured()) "Status: Gemini Clinical AI Ready" else "Status: No Key Saved (Using App Default)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (viewModel.isGeminiConfigured()) StatusSuccess else StatusWarning
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = { apiKeyText = it },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("Paste your API key (AIzaSy...)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_custom_gemini_api_key"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MedicalPrimary,
                            unfocusedBorderColor = BorderSubtle
                        )
                    )

                    if (hasCustomKey) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                apiKeyText = ""
                                viewModel.setCustomApiKey(null)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Custom API key removed.")
                                }
                            }
                        ) {
                            Text("Reset / Remove Custom Key", color = StatusCritical, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = apiKeyText.trim()
                        if (trimmed.isNotBlank()) {
                            viewModel.setCustomApiKey(trimmed)
                            scope.launch {
                                snackbarHostState.showSnackbar("Gemini API Key saved and active!")
                            }
                        } else {
                            viewModel.setCustomApiKey(null)
                        }
                        showApiKeyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_save_gemini_api_key")
                ) {
                    Text("Save API Key", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundClinical,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedSectionIndex == 0) {
                ExtendedFloatingActionButton(
                    onClick = { selectedSectionIndex = 1 },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White) },
                    text = { Text("Ask Assistant", fontWeight = FontWeight.SemiBold, color = Color.White) },
                    containerColor = MedicalPrimary,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("fab_ai_assistant")
                )
            }
        },
        topBar = {
            // Patient Header Bar
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
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MedicalPrimaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MedicalPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = uiState.patientName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = DarkNavy
                        )
                        Text(
                            text = "Patient ID: ${uiState.patientIdTag}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showNotificationTestDialog = true },
                        modifier = Modifier.testTag("btn_patient_test_notifications")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Test Notifications & Email Alerts",
                            tint = MedicalPrimary
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.logout()
                            onLogout()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = SlateSecondary
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Section Switcher: Segmented control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceSubtle)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selectedSectionIndex == 0) SurfaceWhite else Color.Transparent)
                        .then(
                            if (selectedSectionIndex == 0) Modifier.border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                            else Modifier
                        )
                        .clickable { selectedSectionIndex = 0 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = if (selectedSectionIndex == 0) MedicalPrimary else SlateSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Reports & Scans",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selectedSectionIndex == 0) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (selectedSectionIndex == 0) DarkNavy else SlateSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selectedSectionIndex == 1) SurfaceWhite else Color.Transparent)
                        .then(
                            if (selectedSectionIndex == 1) Modifier.border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                            else Modifier
                        )
                        .clickable { selectedSectionIndex = 1 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (selectedSectionIndex == 1) MedicalPrimary else SlateSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Health Assistant",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selectedSectionIndex == 1) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (selectedSectionIndex == 1) DarkNavy else SlateSecondary
                        )
                    }
                }
            }

            if (selectedSectionIndex == 0) {
                // Reports & Heatmap Section
                ReportsAndViewerSection(
                    uiState = uiState,
                    onSelectReport = { viewModel.selectReport(it) },
                    onDownloadPdf = { report ->
                        scope.launch {
                            snackbarHostState.showSnackbar("Downloading official signed report for ${report.screeningId}...")
                        }
                    },
                    onOpenChat = { query ->
                        selectedSectionIndex = 1
                        if (!query.isNullOrBlank()) {
                            viewModel.sendChatMessage(query)
                        }
                    },
                    onDeleteReport = { reportToDelete = it },
                    onDeleteAllReports = { showDeleteAllDialog = true }
                )
            } else {
                // AI Health Assistant
                ChatAssistantSection(
                    uiState = uiState,
                    chatMessages = chatMessages,
                    onSendQuery = { viewModel.sendChatMessage(it) },
                    onInputChange = { viewModel.updateChatInput(it) },
                    onClearChat = { viewModel.clearChat() },
                    onConfigureApiKey = { showApiKeyDialog = true },
                    isApiConfigured = viewModel.isGeminiConfigured()
                )
            }
        }
    }
}

@Composable
private fun ReportsAndViewerSection(
    uiState: PatientPortalUiState,
    onSelectReport: (ReportItemDto) -> Unit,
    onDownloadPdf: (ReportItemDto) -> Unit,
    onOpenChat: (String?) -> Unit,
    onDeleteReport: (ReportItemDto) -> Unit,
    onDeleteAllReports: () -> Unit
) {
    val selectedReport = uiState.selectedReportForViewer

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.reports.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceWhite)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MedicalPrimaryLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = MedicalPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Screening Reports Available",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = DarkNavy
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "When your eye care specialist conducts a retinal fundus examination, your reports and Grad-CAM scans will be available here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else if (selectedReport != null) {
            val isDr = selectedReport.prediction == "DR PRESENT"

            // Active Report Overview Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceWhite)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                        .testTag("patient_report_overview")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SCREENING RESULT",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                                color = SlateSecondary
                            )
                            Text(
                                text = selectedReport.screeningId,
                                style = DiagnosticIdStyle.copy(fontSize = 14.sp),
                                color = DarkNavy
                            )
                        }

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isDr) StatusCriticalBg else StatusSuccessBg)
                                .border(1.dp, if (isDr) StatusCritical.copy(alpha = 0.3f) else StatusSuccess.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = selectedReport.prediction,
                                style = DiagnosticIdSmallStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                color = if (isDr) StatusCritical else StatusSuccess
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    selectedReport.summary?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateSecondary,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Download PDF Action
                    Button(
                        onClick = { onDownloadPdf(selectedReport) },
                        colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_download_pdf")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Download Signed PDF Report",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Ask Assistant About This Scan Action
                    OutlinedButton(
                        onClick = {
                            onOpenChat("Explain the findings and scan for report ${selectedReport.screeningId}")
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_ask_ai_scan")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MedicalPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ask Assistant About This Scan",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MedicalPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Delete This Active Record Action
                    OutlinedButton(
                        onClick = { onDeleteReport(selectedReport) },
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusCritical),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_delete_active_report")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = StatusCritical, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delete This Screening Record",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = StatusCritical
                        )
                    }
                }
            }

            // Dual-Layer Heatmap Viewer
            item {
                HeatmapBlendViewer(
                    imageUrl = null,
                    heatmapUrl = null,
                    isDrPrediction = isDr
                )
            }
        }

        // Screening Records History List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Screening History Records",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DarkNavy
                )
                if (uiState.reports.isNotEmpty()) {
                    TextButton(
                        onClick = onDeleteAllReports,
                        modifier = Modifier.testTag("btn_delete_all_patient_records")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = StatusCritical,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Delete All",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = StatusCritical
                        )
                    }
                }
            }
        }

        items(uiState.reports) { report ->
            val isSelected = report.id == selectedReport?.id
            val isDr = report.prediction == "DR PRESENT"

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceWhite)
                    .border(
                        1.dp,
                        if (isSelected) MedicalPrimary else BorderSubtle,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelectReport(report) }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = report.screeningId,
                            style = DiagnosticIdStyle.copy(fontSize = 13.sp),
                            color = DarkNavy
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isDr) StatusCriticalBg else StatusSuccessBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = report.prediction,
                                style = DiagnosticIdSmallStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                color = if (isDr) StatusCritical else StatusSuccess
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Date: ${report.date} • Risk: ${report.riskLevel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onDeleteReport(report) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_delete_report_${report.screeningId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Report",
                            tint = StatusCritical,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "View Scan",
                        tint = if (isSelected) MedicalPrimary else SlateSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatAssistantSection(
    uiState: PatientPortalUiState,
    chatMessages: List<LocalChatMessage>,
    onSendQuery: (String?) -> Unit,
    onInputChange: (String) -> Unit,
    onClearChat: () -> Unit,
    onConfigureApiKey: () -> Unit,
    isApiConfigured: Boolean
) {
    val listState = rememberLazyListState()

    LaunchedEffect(chatMessages.size, uiState.isSendingChatMessage) {
        val count = chatMessages.size + if (uiState.isSendingChatMessage) 1 else 0
        if (count > 0) {
            listState.animateScrollToItem(count - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Clinical Grounding Bar
        val activeReportId = uiState.selectedReportForViewer?.screeningId
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceWhite)
                .border(1.dp, BorderSubtle)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MedicalPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (activeReportId != null) "Screening Report: $activeReportId" else "Retinopathy Clinical Knowledge Base",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = DarkNavy
                )
                Text(
                    text = if (activeReportId != null) "Grounded in patient examination and clinical guidelines" else "General retinal health and screening guidelines",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = SlateSecondary
                )
            }

            IconButton(
                onClick = onClearChat,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Reset Chat",
                    tint = SlateSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onConfigureApiKey,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("btn_configure_api_key"),
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = "Configure Chatbot Gemini API Key",
                    tint = MedicalPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // API Status Indicator Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isApiConfigured) MedicalPrimarySubtle else StatusWarningBg)
                .clickable { onConfigureApiKey() }
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isApiConfigured) StatusSuccess else StatusWarning)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isApiConfigured) "Gemini Clinical API Active • Tap to view or edit key" else "Tap here to insert your Gemini API Key for AI Chatbot",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp),
                color = if (isApiConfigured) DarkNavy else StatusWarning
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (isApiConfigured) "CONFIGURED" else "INSERT API",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                color = if (isApiConfigured) MedicalPrimary else StatusWarning
            )
        }

        // Suggestion Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val suggestions = listOf(
                "Explain my Grad-CAM scan",
                "What does DR Present mean?",
                "What are my next steps?",
                "What symptoms should I watch for?",
                "What causes microaneurysms?",
                "How do I download my report?"
            )
            items(suggestions) { prompt ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceWhite)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        .clickable { onSendQuery(prompt) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkNavy
                    )
                }
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(chatMessages) { msg ->
                val isUser = msg.role == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (isUser) 0.85f else 0.92f)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                    bottomStart = if (isUser) 12.dp else 2.dp,
                                    bottomEnd = if (isUser) 2.dp else 12.dp
                                )
                            )
                            .background(if (isUser) MedicalPrimary else SurfaceWhite)
                            .border(
                                1.dp,
                                if (isUser) MedicalPrimary else BorderSubtle,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!isUser) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MedicalPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = if (isUser) "You" else "Clinical Assistant",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isUser) Color.White.copy(alpha = 0.9f) else MedicalPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            FormattedChatMessageText(
                                text = msg.message,
                                isUser = isUser
                            )
                        }
                    }
                }
            }

            if (uiState.isSendingChatMessage) {
                item {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceWhite)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = MedicalPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Analyzing clinical context...",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateSecondary
                        )
                    }
                }
            }
        }

        // Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceWhite)
                .border(1.dp, BorderSubtle)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.currentChatInput,
                onValueChange = onInputChange,
                placeholder = { Text("Ask about your report or retinopathy...", color = SlateSecondary) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send,
                    keyboardType = KeyboardType.Text
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (uiState.currentChatInput.isNotBlank() && !uiState.isSendingChatMessage) {
                            onSendQuery(null)
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MedicalPrimary,
                    unfocusedBorderColor = BorderSubtle,
                    focusedContainerColor = SurfaceSubtle,
                    unfocusedContainerColor = SurfaceSubtle,
                    focusedTextColor = DarkNavy,
                    unfocusedTextColor = DarkNavy
                ),
                shape = RoundedCornerShape(20.dp),
                maxLines = 3
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { onSendQuery(null) },
                enabled = uiState.currentChatInput.isNotBlank() && !uiState.isSendingChatMessage,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (uiState.currentChatInput.isNotBlank()) MedicalPrimary else SurfaceSubtle)
                    .testTag("chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (uiState.currentChatInput.isNotBlank()) Color.White else SlateTertiary
                )
            }
        }
    }
}

@Composable
private fun FormattedChatMessageText(
    text: String,
    isUser: Boolean
) {
    val annotated = remember(text) {
        buildAnnotatedString {
            val parts = text.split("**")
            for (i in parts.indices) {
                if (i % 2 == 1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = if (isUser) Color.White else DarkNavy)) {
                        append(parts[i])
                    }
                } else {
                    append(parts[i])
                }
            }
        }
    }

    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isUser) Color.White else DarkNavy,
        lineHeight = 20.sp
    )
}
