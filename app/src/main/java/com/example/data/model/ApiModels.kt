package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "token_type") val tokenType: String = "bearer"
)

@JsonClass(generateAdapter = true)
data class UserDto(
    val id: Int,
    val username: String,
    val role: String, // HEALTHCARE_WORKER | PATIENT | SPECIALIST | ADMIN
    @Json(name = "is_active") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class PatientDto(
    val id: Int,
    @Json(name = "first_name") val firstName: String,
    @Json(name = "last_name") val lastName: String,
    val email: String? = null,
    val phone: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
) {
    val fullName: String get() = "$firstName $lastName"
}

@JsonClass(generateAdapter = true)
data class CreatePatientRequest(
    @Json(name = "first_name") val firstName: String,
    @Json(name = "last_name") val lastName: String,
    val email: String,
    val phone: String
)

@JsonClass(generateAdapter = true)
data class ScreenStatsDto(
    @Json(name = "total_screenings") val totalScreenings: Int = 0,
    @Json(name = "screenings_today") val screeningsToday: Int = 0,
    @Json(name = "dr_present_count") val drPresentCount: Int = 0,
    @Json(name = "no_dr_count") val noDrCount: Int = 0
) {
    val drPositiveRate: Float
        get() = if (totalScreenings > 0) (drPresentCount.toFloat() / totalScreenings) * 100f else 0f

    val normalRate: Float
        get() = if (totalScreenings > 0) (noDrCount.toFloat() / totalScreenings) * 100f else 0f
}

@JsonClass(generateAdapter = true)
data class ScreeningResultDto(
    val id: Int,
    @Json(name = "screening_id") val screeningId: String,
    val prediction: String, // "DR PRESENT" | "NO DR"
    @Json(name = "probability_dr") val probabilityDr: Float,
    @Json(name = "probability_no_dr") val probabilityNoDr: Float,
    val confidence: Float,
    @Json(name = "risk_level") val riskLevel: String, // "LOW" | "MODERATE" | "HIGH"
    val recommendation: String,
    @Json(name = "ai_context") val aiContext: String,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "heatmap_url") val heatmapUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class ReportItemDto(
    val id: Int,
    @Json(name = "screening_id") val screeningId: String,
    val date: String,
    val prediction: String,
    val confidence: Float,
    @Json(name = "risk_level") val riskLevel: String,
    @Json(name = "report_url") val reportUrl: String? = null,
    @Json(name = "is_published") val isPublished: Boolean = true,
    val summary: String? = null
)

@JsonClass(generateAdapter = true)
data class ChatPartDto(
    val text: String
)

@JsonClass(generateAdapter = true)
data class ChatHistoryMessage(
    val role: String, // "user" | "model"
    val parts: List<String>
)

@JsonClass(generateAdapter = true)
data class ChatRequest(
    val message: String,
    val history: List<ChatHistoryMessage> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ChatResponse(
    val answer: String
)

@JsonClass(generateAdapter = true)
data class ApiResponseStatus(
    val status: String,
    val message: String? = null,
    @Json(name = "report_id") val reportId: Int? = null
)
