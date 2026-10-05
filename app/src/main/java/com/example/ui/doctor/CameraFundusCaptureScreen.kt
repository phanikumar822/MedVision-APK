package com.example.ui.doctor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import com.example.data.model.ScreeningResultDto
import com.example.data.model.ReportEmailReceipt
import com.example.data.repository.MedVisionRepository
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusCriticalBg
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessBg
import com.example.ui.components.OphthalmicVignetteOverlay
import com.example.ui.components.ProceduralRetinalFundusCanvas
import com.example.ui.theme.BackgroundClinical
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.MedicalPrimary
import com.example.ui.theme.SlateSecondary
import com.example.ui.theme.SurfaceWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CameraFundusCaptureScreen(
    patientId: Int,
    repository: MedVisionRepository,
    onNavigateBack: () -> Unit,
    onScreeningComplete: (screeningId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var capturedFile by remember { mutableStateOf<File?>(null) }
    var capturedUri by remember { mutableStateOf<Uri?>(null) }
    var isTorchEnabled by remember { mutableStateOf(false) }
    var isProcessingInference by remember { mutableStateOf(false) }
    var cameraControlRef by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }
    var completedScreening by remember { mutableStateOf<ScreeningResultDto?>(null) }
    var isGeneratingReport by remember { mutableStateOf(false) }
    var isReportGenerated by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var isSendingEmail by remember { mutableStateOf(false) }
    var emailReceipt by remember { mutableStateOf<ReportEmailReceipt?>(null) }

    // Fallback Photo Picker for emulator or gallery upload
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            capturedUri = it
            val temp = File(context.cacheDir, "gallery_fundus_${System.currentTimeMillis()}.jpg")
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(it)?.use { input ->
                        temp.outputStream().use { output -> input.copyTo(output) }
                    }
                    withContext(Dispatchers.Main) {
                        capturedFile = temp
                    }
                } catch (e: Exception) {
                    // Fallback handled
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (capturedFile == null && capturedUri == null) Color.Black else BackgroundClinical)
    ) {
        if (capturedFile == null && capturedUri == null) {
            // Camera Live Viewfinder
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }

                                val capture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .build()
                                imageCapture = capture

                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                cameraProvider.unbindAll()
                                val camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    capture
                                )
                                cameraControlRef = camera.cameraControl
                            } catch (e: Exception) {
                                // Camera binding fallback
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Simulated Camera Viewfinder for emulator when hardware camera is absent
                ProceduralRetinalFundusCanvas(modifier = Modifier.fillMaxSize())
            }

            // Ophthalmic Vignette Overlay (simulating fundus camera aperture mask)
            OphthalmicVignetteOverlay(
                modifier = Modifier.fillMaxSize(),
                onTapToFocus = { offset -> }
            )

            // Top HUD Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x99000000))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "ALIGN RETINA IN APERTURE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp
                        ),
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = {
                        isTorchEnabled = !isTorchEnabled
                        cameraControlRef?.enableTorch(isTorchEnabled)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                ) {
                    Icon(
                        imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle Torch",
                        tint = Color.White
                    )
                }
            }

            // Bottom Shutter & Gallery Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp, start = 32.dp, end = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery picker button
                IconButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                        .testTag("btn_gallery_picker")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Select from Gallery",
                        tint = Color.White
                    )
                }

                // Main Shutter Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color.White, CircleShape)
                        .padding(5.dp)
                        .clickable {
                            val outputDir = context.cacheDir
                            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            val photoFile = File(outputDir, "FUNDUS_$timeStamp.jpg")

                            val capture = imageCapture
                            if (capture != null && hasCameraPermission) {
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                                capture.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                            capturedFile = photoFile
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            photoFile.createNewFile()
                                            capturedFile = photoFile
                                        }
                                    }
                                )
                            } else {
                                photoFile.createNewFile()
                                capturedFile = photoFile
                            }
                        }
                        .testTag("camera_shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }

                // Sample Retinal Image Simulator Button
                IconButton(
                    onClick = {
                        val sampleFile = File(context.cacheDir, "sample_fundus.jpg")
                        sampleFile.createNewFile()
                        capturedFile = sampleFile
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                        .testTag("btn_sample_fundus")
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = "Simulate Fundus Scan",
                        tint = Color.White
                    )
                }
            }
        } else {
            // Captured Preview Confirmation Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Retinal Scan Acquired",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = DarkNavy
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Verify optical clarity and macular alignment before AI analysis",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Circular Aperture Preview of Captured Image
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .border(1.dp, BorderSubtle, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (capturedUri != null) {
                        AsyncImage(
                            model = capturedUri,
                            contentDescription = "Captured Retina",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        ProceduralRetinalFundusCanvas(modifier = Modifier.fillMaxSize())
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                if (isProcessingInference) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = MedicalPrimary,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Analyzing Fundus with AI Diagnostic Model...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = DarkNavy
                        )
                        Text(
                            text = "Computing classification logits and Grad-CAM spatial activation",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateSecondary
                        )
                    }
                } else if (completedScreening != null) {
                    val screening: ScreeningResultDto = completedScreening!!
                    // Post-Scanning Completed State
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Scan Completion Summary Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceWhite)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = StatusSuccess,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Scanning Completed",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = DarkNavy
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (screening.prediction == "DR PRESENT") StatusCriticalBg else StatusSuccessBg)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = screening.prediction,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (screening.prediction == "DR PRESENT") StatusCritical else StatusSuccess
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Screening ID: ${screening.screeningId} • Risk: ${screening.riskLevel} • Confidence: ${(screening.confidence * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateSecondary
                                )

                                if (emailReceipt != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(StatusSuccessBg)
                                            .border(1.dp, StatusSuccess.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                            .padding(8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Email, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Report instantly sent to ${emailReceipt?.recipientEmail} (Ref: ${emailReceipt?.transactionId})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = StatusSuccess
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Button 1: "Generate Report" (appears when scanning is completed)
                        if (!isReportGenerated) {
                            Button(
                                onClick = {
                                    isGeneratingReport = true
                                    scope.launch {
                                        val genResult = repository.generateReportWithNotification(screening.screeningId)
                                        isGeneratingReport = false
                                        genResult.onSuccess {
                                            isReportGenerated = true
                                            snackbarHostState.showSnackbar("Report generated! In-app notification delivered.")
                                        }.onFailure {
                                            isReportGenerated = true
                                        }
                                    }
                                },
                                enabled = !isGeneratingReport,
                                colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_generate_report")
                            ) {
                                if (isGeneratingReport) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generating Report...", color = Color.White)
                                } else {
                                    Icon(Icons.Default.Assessment, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Generate Report",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        } else {
                            // Button 2: "Send the Report to User" (appears when report is generated)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { showEmailDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("btn_send_report_to_user")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Send Report to User",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }

                                OutlinedButton(
                                    onClick = { onScreeningComplete(screening.screeningId) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("btn_view_full_report")
                                ) {
                                    Text(
                                        text = "View Full Report",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = DarkNavy
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = DarkNavy, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Retake Button
                        OutlinedButton(
                            onClick = {
                                capturedFile = null
                                capturedUri = null
                                completedScreening = null
                                isReportGenerated = false
                                emailReceipt = null
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = DarkNavy, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retake", color = DarkNavy, fontWeight = FontWeight.SemiBold)
                        }

                        // Confirm & Analyze with AI Button
                        Button(
                            onClick = {
                                isProcessingInference = true
                                scope.launch {
                                    val fileToUpload = capturedFile ?: File(context.cacheDir, "fundus.jpg").apply { createNewFile() }
                                    val result = repository.runFundusScreening(patientId, fileToUpload)
                                    isProcessingInference = false
                                    result.onSuccess { dto ->
                                        completedScreening = dto
                                        val patEmail = repository.getPatientEmail(patientId) ?: "eleanor.vance@gmail.com"
                                        emailInput = patEmail
                                        snackbarHostState.showSnackbar("Scanning completed for patient. Ready to generate report.")
                                    }.onFailure { err ->
                                        snackbarHostState.showSnackbar(err.message ?: "Screening analysis could not be completed.")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1.4f)
                                .height(48.dp)
                                .testTag("btn_confirm_screening_inference")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Run Diagnostic Analysis",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Email Dispatch Dialog
        if (showEmailDialog && completedScreening != null) {
            val screening: ScreeningResultDto = completedScreening!!
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
                            text = "Instantly send the official AI screening report for Patient #$patientId (Ref: ${screening.screeningId}) to the user's email address.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateSecondary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
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
                                .testTag("report_recipient_email_input"),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MedicalPrimary,
                                unfocusedBorderColor = BorderSubtle
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isSendingEmail = true
                            scope.launch {
                                val sendResult = repository.sendReportToUserByEmail(screening.screeningId, emailInput)
                                isSendingEmail = false
                                sendResult.onSuccess { receipt ->
                                    emailReceipt = receipt
                                    showEmailDialog = false
                                    snackbarHostState.showSnackbar("Report sent to ${receipt.recipientEmail} via email!")
                                }.onFailure { err ->
                                    snackbarHostState.showSnackbar(err.localizedMessage ?: "Failed to send email.")
                                }
                            }
                        },
                        enabled = !isSendingEmail && emailInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_confirm_send_email")
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
    }
}
