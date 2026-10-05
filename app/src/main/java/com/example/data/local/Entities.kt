package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_patients")
data class CachedPatient(
    @PrimaryKey val id: Int,
    val firstName: String,
    val lastName: String,
    val email: String?,
    val phone: String?,
    val createdAt: String? = null
) {
    val fullName: String get() = "$firstName $lastName"
}

@Entity(tableName = "cached_screenings")
data class CachedScreening(
    @PrimaryKey val id: Int,
    val screeningId: String,
    val patientId: Int,
    val patientName: String,
    val prediction: String, // "DR PRESENT" | "NO DR"
    val probabilityDr: Float,
    val probabilityNoDr: Float,
    val confidence: Float,
    val riskLevel: String, // "LOW" | "MODERATE" | "HIGH"
    val recommendation: String,
    val aiContext: String,
    val imageUrl: String?,
    val heatmapUrl: String?,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class LocalChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String, // "user" | "assistant"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
