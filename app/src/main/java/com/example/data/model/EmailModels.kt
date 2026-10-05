package com.example.data.model

data class VerificationEmailDetails(
    val email: String,
    val token: String,
    val setupLink: String,
    val role: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ReportEmailReceipt(
    val transactionId: String,
    val screeningId: String,
    val patientName: String,
    val recipientEmail: String,
    val sentTimestamp: Long = System.currentTimeMillis(),
    val status: String = "DELIVERED",
    val protocol: String = "TLS 1.3 / HIPAA Encrypted SMTP",
    val emailSubject: String = "MedVision AI: Ophthalmic Screening Diagnostic Report"
)
