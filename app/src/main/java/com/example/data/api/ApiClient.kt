package com.example.data.api

import com.example.BuildConfig
import com.example.data.local.SessionManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = sessionManager.getAuthToken()

        val requestBuilder = original.newBuilder()
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }
        requestBuilder.header("Accept", "application/json")

        return chain.proceed(requestBuilder.build())
    }
}

object ApiClient {
    private var apiService: MedVisionApiService? = null

    fun getService(sessionManager: SessionManager): MedVisionApiService {
        return apiService ?: synchronized(this) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(sessionManager))
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .writeTimeout(45, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val baseUrl = try {
                val url = BuildConfig.BASE_URL
                if (url.endsWith("/")) url else "$url/"
            } catch (e: Exception) {
                "http://10.0.2.2:8000/"
            }

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            val service = retrofit.create(MedVisionApiService::class.java)
            apiService = service
            service
        }
    }
}
