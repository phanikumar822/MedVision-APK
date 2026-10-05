package com.example.data.api

import com.example.data.model.ApiResponseStatus
import com.example.data.model.ChatRequest
import com.example.data.model.ChatResponse
import com.example.data.model.CreatePatientRequest
import com.example.data.model.LoginResponse
import com.example.data.model.PatientDto
import com.example.data.model.ReportItemDto
import com.example.data.model.ScreenStatsDto
import com.example.data.model.ScreeningResultDto
import com.example.data.model.UserDto
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface MedVisionApiService {

    @FormUrlEncoded
    @POST("auth/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<LoginResponse>

    @GET("auth/me")
    suspend fun getCurrentUser(): Response<UserDto>

    @POST("auth/logout")
    suspend fun logout(): Response<ResponseBody>

    @GET("patients/")
    suspend fun getPatients(): Response<List<PatientDto>>

    @POST("patients/")
    suspend fun createPatient(
        @Body request: CreatePatientRequest
    ): Response<PatientDto>

    @DELETE("patients/{id}")
    suspend fun deletePatient(
        @Path("id") id: Int
    ): Response<ResponseBody>

    @GET("screen/stats")
    suspend fun getScreenStats(): Response<ScreenStatsDto>

    @Multipart
    @POST("screen/")
    suspend fun runScreening(
        @Query("patient_id") patientId: Int,
        @Part file: MultipartBody.Part
    ): Response<ScreeningResultDto>

    @POST("reports/{screening_id}/generate")
    suspend fun generateReport(
        @Path("screening_id") screeningId: String
    ): Response<ApiResponseStatus>

    @POST("reports/{report_id}/publish")
    suspend fun publishReport(
        @Path("report_id") reportId: Int
    ): Response<ApiResponseStatus>

    @GET("reports/my-reports")
    suspend fun getMyReports(): Response<List<ReportItemDto>>

    @Streaming
    @GET("reports/{report_id}/download")
    suspend fun downloadReport(
        @Path("report_id") reportId: Int
    ): Response<ResponseBody>

    @POST("chat/")
    suspend fun chatWithAssistant(
        @Body request: ChatRequest
    ): Response<ChatResponse>
}
