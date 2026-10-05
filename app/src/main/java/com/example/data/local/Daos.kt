package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Query("SELECT * FROM cached_patients ORDER BY id DESC")
    fun getAllPatients(): Flow<List<CachedPatient>>

    @Query("SELECT * FROM cached_patients WHERE id = :id LIMIT 1")
    suspend fun getPatientById(id: Int): CachedPatient?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatients(patients: List<CachedPatient>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: CachedPatient)

    @Query("DELETE FROM cached_patients WHERE id = :id")
    suspend fun deletePatient(id: Int)

    @Query("DELETE FROM cached_patients")
    suspend fun clearPatients()
}

@Dao
interface ScreeningDao {
    @Query("SELECT * FROM cached_screenings ORDER BY timestamp DESC")
    fun getAllScreenings(): Flow<List<CachedScreening>>

    @Query("SELECT * FROM cached_screenings WHERE patientId = :patientId ORDER BY timestamp DESC")
    fun getScreeningsForPatient(patientId: Int): Flow<List<CachedScreening>>

    @Query("SELECT * FROM cached_screenings WHERE screeningId = :screeningId LIMIT 1")
    suspend fun getScreeningById(screeningId: String): CachedScreening?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScreening(screening: CachedScreening)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScreenings(screenings: List<CachedScreening>)

    @Query("DELETE FROM cached_screenings WHERE screeningId = :screeningId")
    suspend fun deleteScreening(screeningId: String)

    @Query("DELETE FROM cached_screenings WHERE patientId = :patientId")
    suspend fun deleteScreeningsForPatient(patientId: Int)

    @Query("DELETE FROM cached_screenings")
    suspend fun clearScreenings()
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY id ASC")
    fun getAllMessages(): Flow<List<LocalChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: LocalChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun clearMessages()
}
