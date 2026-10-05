package com.example.ui.doctor

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.CachedPatient
import com.example.data.local.CachedScreening
import com.example.ui.components.NotificationTestDialog
import com.example.ui.components.ProbabilityBarChart
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
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.SurfaceWhite
import kotlinx.coroutines.launch

@Composable
fun DoctorDashboardScreen(
    viewModel: DoctorViewModel,
    onNavigateToCamera: (patientId: Int) -> Unit,
    onNavigateToResult: (screeningId: String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val patients by viewModel.patients.collectAsStateWithLifecycle()
    val screenings by viewModel.screenings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var patientToDelete by remember { mutableStateOf<CachedPatient?>(null) }
    var screeningToDelete by remember { mutableStateOf<CachedScreening?>(null) }
    var showNotificationTestDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundClinical,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceWhite,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .border(
                        width = 1.dp,
                        color = BorderSubtle,
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    )
            ) {
                NavigationBarItem(
                    selected = uiState.selectedTab == DoctorTab.SCREENING,
                    onClick = { viewModel.selectTab(DoctorTab.SCREENING) },
                    icon = { Icon(Icons.Default.MedicalServices, contentDescription = "Clinical Screening & Analytics Hub") },
                    label = { Text("Clinical Hub") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MedicalPrimary,
                        selectedTextColor = MedicalPrimary,
                        indicatorColor = MedicalPrimaryLight,
                        unselectedIconColor = SlateSecondary,
                        unselectedTextColor = SlateSecondary
                    ),
                    modifier = Modifier.testTag("nav_tab_screening")
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == DoctorTab.PATIENTS,
                    onClick = { viewModel.selectTab(DoctorTab.PATIENTS) },
                    icon = { Icon(Icons.Default.People, contentDescription = "Patient Directory") },
                    label = { Text("Patients") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MedicalPrimary,
                        selectedTextColor = MedicalPrimary,
                        indicatorColor = MedicalPrimaryLight,
                        unselectedIconColor = SlateSecondary,
                        unselectedTextColor = SlateSecondary
                    ),
                    modifier = Modifier.testTag("nav_tab_patients")
                )
            }
        },
        floatingActionButton = {
            if (uiState.selectedTab == DoctorTab.PATIENTS) {
                FloatingActionButton(
                    onClick = { viewModel.openAddPatientDialog() },
                    containerColor = MedicalPrimary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("fab_add_patient")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add New Patient")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top Bar
            DoctorTopBar(
                doctorName = uiState.doctorName,
                onRefresh = { viewModel.loadDashboardData() },
                onOpenNotificationTest = { showNotificationTestDialog = true },
                onLogout = {
                    viewModel.logout()
                    onLogout()
                }
            )

            // Body content based on tab
            when (uiState.selectedTab) {
                DoctorTab.SCREENING -> {
                    ScreeningTabContent(
                        uiState = uiState,
                        screenings = screenings,
                        patients = patients,
                        onStartCapture = {
                            val defaultPatientId = patients.firstOrNull()?.id ?: 1
                            onNavigateToCamera(defaultPatientId)
                        },
                        onOpenResult = { screeningId -> onNavigateToResult(screeningId) },
                        onOpenNotificationTest = { showNotificationTestDialog = true },
                        onExportAuditLog = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Clinical Quality & Telemetry Audit Report generated successfully.")
                            }
                        },
                        onDeleteScreening = { screeningToDelete = it }
                    )
                }
                DoctorTab.PATIENTS -> {
                    PatientsTabContent(
                        patients = patients,
                        searchQuery = uiState.searchQuery,
                        onSearchChange = { viewModel.updateSearchQuery(it) },
                        onScreenPatient = { patientId -> onNavigateToCamera(patientId) },
                        onDeletePatient = { patientToDelete = it }
                    )
                }
            }
        }
    }

    // Confirmation Dialog for deleting patient records
    if (patientToDelete != null) {
        val target = patientToDelete!!
        AlertDialog(
            onDismissRequest = { patientToDelete = null },
            title = {
                Text(
                    text = "Delete Patient Records",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DarkNavy
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete all records for ${target.fullName} (ID: #${target.id})? This will delete the patient profile and all associated retinal fundus screenings, Grad-CAM analyses, and diagnostic reports.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkNavy
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePatient(target.id) { success ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    if (success) "Records for ${target.fullName} deleted successfully."
                                    else "Failed to delete patient records."
                                )
                            }
                        }
                        patientToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCritical),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_confirm_delete_patient")
                ) {
                    Text("Delete Records", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { patientToDelete = null }) {
                    Text("Cancel", color = SlateSecondary)
                }
            },
            containerColor = SurfaceWhite
        )
    }

    // Confirmation Dialog for deleting a screening record
    if (screeningToDelete != null) {
        val target = screeningToDelete!!
        AlertDialog(
            onDismissRequest = { screeningToDelete = null },
            title = {
                Text(
                    text = "Delete Screening Record",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DarkNavy
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete screening record ${target.screeningId} for ${target.patientName}?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkNavy
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteScreening(target.screeningId) { success ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    if (success) "Screening record ${target.screeningId} deleted."
                                    else "Failed to delete screening record."
                                )
                            }
                        }
                        screeningToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCritical),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_confirm_delete_screening")
                ) {
                    Text("Delete Record", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { screeningToDelete = null }) {
                    Text("Cancel", color = SlateSecondary)
                }
            },
            containerColor = SurfaceWhite
        )
    }

    // Add Patient Dialog
    if (uiState.showAddPatientDialog) {
        AddPatientModalDialog(
            uiState = uiState,
            onFieldChange = { first, last, email, phone ->
                viewModel.updateNewPatientField(first, last, email, phone)
            },
            onDismiss = { viewModel.closeAddPatientDialog() },
            onConfirm = { viewModel.submitNewPatient() }
        )
    }

    if (showNotificationTestDialog) {
        NotificationTestDialog(
            initialEmail = "1818phani@gmail.com",
            onDismiss = { showNotificationTestDialog = false }
        )
    }
}

@Composable
private fun DoctorTopBar(
    doctorName: String,
    onRefresh: () -> Unit,
    onOpenNotificationTest: () -> Unit,
    onLogout: () -> Unit
) {
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
                    imageVector = Icons.Default.LocalHospital,
                    contentDescription = null,
                    tint = MedicalPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = doctorName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DarkNavy
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(StatusSuccess)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Vitreoretinal Clinic • Online",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateSecondary
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onOpenNotificationTest,
                modifier = Modifier.testTag("btn_top_bar_test_notifications")
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "Test Mobile & Email Notifications",
                    tint = MedicalPrimary
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Stats",
                    tint = SlateSecondary
                )
            }
            IconButton(onClick = onLogout) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Logout",
                    tint = SlateSecondary
                )
            }
        }
    }
}

@Composable
private fun ScreeningTabContent(
    uiState: DoctorDashboardUiState,
    screenings: List<CachedScreening>,
    patients: List<CachedPatient>,
    onStartCapture: () -> Unit,
    onOpenResult: (String) -> Unit,
    onOpenNotificationTest: () -> Unit,
    onExportAuditLog: () -> Unit,
    onDeleteScreening: (CachedScreening) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Clinical Stats Row
        item {
            ClinicalStatsRow(
                screeningsToday = uiState.stats.screeningsToday,
                totalPatients = patients.size.coerceAtLeast(uiState.stats.totalScreenings),
                normalRate = uiState.stats.normalRate,
                drRate = uiState.stats.drPositiveRate
            )
        }

        // Primary Action: New Fundus Screening Button (Clean Medical Button)
        item {
            Button(
                onClick = onStartCapture,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_start_fundus_capture"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start Retinal Fundus Screening",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                }
            }
        }

        // Test Mobile & Email Notifications Action
        item {
            OutlinedButton(
                onClick = onOpenNotificationTest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("btn_dashboard_test_notifications"),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MedicalPrimary.copy(alpha = 0.5f))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MedicalPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Test Mobile & Email Notifications",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MedicalPrimary
                    )
                }
            }
        }

        // Comprehensive Clinical & Model Analytics Section (Integrated on the existing page)
        item {
            DoctorClinicalAnalyticsSection(
                stats = uiState.stats,
                screenings = screenings,
                patients = patients,
                onExportAuditLog = onExportAuditLog
            )
        }

        // Interactive DR vs Non-DR Probability Distribution Card (only when a screening exists)
        item {
            val latest = screenings.firstOrNull()
            if (latest != null) {
                ProbabilityBarChart(
                    probabilityDr = latest.probabilityDr,
                    probabilityNoDr = latest.probabilityNoDr,
                    patientLabel = "Latest Scan: ${latest.patientName} (${latest.screeningId})"
                )
            }
        }

        // Recent Screenings Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Retinopathy Screenings",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DarkNavy
                )
                Text(
                    text = "${screenings.size} records",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateSecondary
                )
            }
        }

        // Screenings List or Empty State
        if (screenings.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceWhite)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No Screenings Recorded Yet",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = DarkNavy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Completed retinal fundus examinations will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateSecondary
                        )
                    }
                }
            }
        } else {
            items(screenings) { scan ->
                ScreeningRecordCard(
                    scan = scan,
                    onClick = { onOpenResult(scan.screeningId) },
                    onDelete = { onDeleteScreening(scan) }
                )
            }
        }
    }
}

@Composable
private fun ClinicalStatsRow(
    screeningsToday: Int,
    totalPatients: Int,
    normalRate: Float,
    drRate: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceWhite)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(16.dp)
            .testTag("clinical_stats_row")
    ) {
        Text(
            text = "CLINICAL ACTIVITY OVERVIEW",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp, fontWeight = FontWeight.SemiBold),
            color = SlateSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatMetricItem(label = "Today", value = "$screeningsToday", color = DarkNavy)
            StatMetricItem(label = "Patients", value = "$totalPatients", color = DarkNavy)
            StatMetricItem(label = "Normal", value = "%.0f%%".format(normalRate), color = StatusSuccess)
            StatMetricItem(label = "DR Present", value = "%.0f%%".format(drRate), color = StatusCritical)
        }
    }
}

@Composable
private fun StatMetricItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SlateSecondary
        )
    }
}

@Composable
private fun ScreeningRecordCard(
    scan: CachedScreening,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isDr = scan.prediction == "DR PRESENT"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceWhite)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag("screening_item_${scan.screeningId}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = scan.screeningId,
                    style = DiagnosticIdStyle.copy(fontSize = 13.sp),
                    color = DarkNavy
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isDr) StatusCriticalBg else StatusSuccessBg)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = scan.prediction,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                        color = if (isDr) StatusCritical else StatusSuccess
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = scan.patientName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = DarkNavy
            )

            Text(
                text = "Confidence: %.1f%% • Risk: %s".format(scan.confidence * 100f, scan.riskLevel),
                style = MaterialTheme.typography.labelSmall,
                color = SlateSecondary
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("btn_delete_screening_${scan.screeningId}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Record",
                    tint = StatusCritical,
                    modifier = Modifier.size(18.dp)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "View Details",
                tint = SlateSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun PatientsTabContent(
    patients: List<CachedPatient>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onScreenPatient: (Int) -> Unit,
    onDeletePatient: (CachedPatient) -> Unit
) {
    val filtered = patients.filter {
        it.fullName.contains(searchQuery, ignoreCase = true) ||
                (it.email?.contains(searchQuery, ignoreCase = true) == true) ||
                (it.phone?.contains(searchQuery, ignoreCase = true) == true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("patient_search_input"),
            placeholder = { Text("Search patient by name, phone, or email...", color = SlateSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SlateSecondary) },
            singleLine = true,
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

        Spacer(modifier = Modifier.height(14.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceWhite)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (searchQuery.isBlank()) "No Patients Registered" else "No Matching Patients",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = DarkNavy
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "Tap the '+' button below to register a new patient." else "Try searching with a different name or contact number.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered) { patient ->
                    PatientCard(
                        patient = patient,
                        onScreen = { onScreenPatient(patient.id) },
                        onDelete = { onDeletePatient(patient) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PatientCard(
    patient: CachedPatient,
    onScreen: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceWhite)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .padding(14.dp)
            .testTag("patient_card_${patient.id}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = patient.fullName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = DarkNavy
            )
            patient.phone?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium, color = SlateSecondary)
            }
            patient.email?.let {
                Text(text = it, style = MaterialTheme.typography.labelSmall, color = SlateTertiary)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = onScreen,
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = MedicalPrimary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Screen", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = MedicalPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("btn_delete_patient_${patient.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Patient Records",
                    tint = StatusCritical,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AddPatientModalDialog(
    uiState: DoctorDashboardUiState,
    onFieldChange: (String, String, String, String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        title = {
            Text(
                text = "Register New Patient",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = DarkNavy
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = uiState.newPatientFirstName,
                    onValueChange = { onFieldChange(it, uiState.newPatientLastName, uiState.newPatientEmail, uiState.newPatientPhone) },
                    label = { Text("First Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DarkNavy,
                        unfocusedTextColor = DarkNavy,
                        focusedBorderColor = MedicalPrimary,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = uiState.newPatientLastName,
                    onValueChange = { onFieldChange(uiState.newPatientFirstName, it, uiState.newPatientEmail, uiState.newPatientPhone) },
                    label = { Text("Last Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DarkNavy,
                        unfocusedTextColor = DarkNavy,
                        focusedBorderColor = MedicalPrimary,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = uiState.newPatientEmail,
                    onValueChange = { onFieldChange(uiState.newPatientFirstName, uiState.newPatientLastName, it, uiState.newPatientPhone) },
                    label = { Text("Email Address") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DarkNavy,
                        unfocusedTextColor = DarkNavy,
                        focusedBorderColor = MedicalPrimary,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = uiState.newPatientPhone,
                    onValueChange = { onFieldChange(uiState.newPatientFirstName, uiState.newPatientLastName, uiState.newPatientEmail, it) },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DarkNavy,
                        unfocusedTextColor = DarkNavy,
                        focusedBorderColor = MedicalPrimary,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimary)
            ) {
                Text("Register Patient", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SlateSecondary)
            }
        }
    )
}
