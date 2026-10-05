package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.LocalChatMessage
import com.example.data.local.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiClinicalService(private val sessionManager: SessionManager? = null) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun resolveApiKey(): String {
        val customKey = sessionManager?.getCustomApiKey()?.trim()
        if (!customKey.isNullOrBlank()) return customKey

        val buildConfigKey = try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (e: Exception) {
            ""
        }
        if (buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY") {
            return buildConfigKey
        }

        val envFieldKey = try {
            BuildConfig.ENV_GEMINI_API_KEY.trim()
        } catch (e: Exception) {
            ""
        }
        if (envFieldKey.isNotBlank() && envFieldKey != "MY_GEMINI_API_KEY") {
            return envFieldKey
        }

        val envVarKey = try {
            System.getenv("GEMINI_API_KEY")?.trim() ?: ""
        } catch (e: Exception) {
            ""
        }
        if (envVarKey.isNotBlank() && envVarKey != "MY_GEMINI_API_KEY") {
            return envVarKey
        }

        return ""
    }

    fun isApiConfigured(): Boolean = resolveApiKey().isNotBlank()

    suspend fun askAssistant(
        userQuery: String,
        conversationHistory: List<LocalChatMessage>,
        clinicalContext: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey()

        if (apiKey.isBlank()) {
            Log.w("GeminiClinicalService", "No active Gemini API key found")
            return@withContext Result.failure(IllegalStateException("Gemini API key is not configured. Please insert an API key in Chat settings."))
        }

        try {
            val systemPrompt = """
                You are MedVisionAI Clinical Diagnostic Assistant, specialized in ophthalmic diabetic retinopathy (DR) 
                screening interpretation, retinal fundus photography analysis, and Grad-CAM interpretability.
                You communicate with clinical empathy, accuracy, and clear, structured patient-friendly language.
                
                Patient Clinical Context:
                $clinicalContext
                
                Guidelines:
                - Ground your responses firmly in the patient's retrieved clinical records and diagnostic findings.
                - Explain technical concepts clearly:
                  * Grad-CAM heatmaps: crimson/orange spots highlight high-weight neural activation zones (microaneurysms, hemorrhages), while blue/green represents healthy retinal parenchyma.
                  * Diabetic retinopathy: microvascular damage caused by chronic hyperglycemia.
                - Provide clear, actionable recommendations: dilated fundus exam referral timeframe, HbA1c targets (< 7.0%), blood pressure control (< 130/80 mmHg).
                - Remind patients that MedVisionAI is an AI-assisted screening decision tool and does not replace in-person dilated ophthalmic examination by an eye specialist.
                - Direct patients to download their official signed PDF report if they need clinical documentation for their provider.
                - Format responses cleanly with markdown bullet points and bold headers for clarity.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemPrompt) })
                    })
                })

                val contentsArray = JSONArray()

                // Gemini API STRICT REQUIREMENTS:
                // 1. Must start with role="user"
                // 2. Turns must alternate ("user", "model", "user", "model"...)
                // 3. Last turn must be "user"
                val validTurns = mutableListOf<Pair<String, String>>()
                for (msg in conversationHistory.takeLast(6)) {
                    val cleanText = msg.message.trim()
                    if (cleanText.isBlank()) continue
                    val role = if (msg.role == "user") "user" else "model"

                    // Drop any initial assistant greetings so first message is always user
                    if (validTurns.isEmpty() && role != "user") continue

                    // Merge consecutive turns with the same role
                    if (validTurns.isNotEmpty() && validTurns.last().first == role) {
                        val prev = validTurns.removeAt(validTurns.lastIndex)
                        validTurns.add(role to "${prev.second}\n${cleanText}")
                    } else {
                        validTurns.add(role to cleanText)
                    }
                }

                // If the last historical turn was also user, pop it to avoid consecutive user turns before userQuery
                if (validTurns.isNotEmpty() && validTurns.last().first == "user") {
                    validTurns.removeAt(validTurns.lastIndex)
                }

                for ((role, text) in validTurns) {
                    contentsArray.put(JSONObject().apply {
                        put("role", role)
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", text) })
                        })
                    })
                }

                // Append current user message as the final turn
                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", userQuery) })
                    })
                })

                put("contents", contentsArray)

                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                    put("topP", 0.95)
                })
            }

            val requestBody = requestJson.toString().toRequestBody(jsonMediaType)
            val candidateModels = listOf(
                "gemini-3.5-flash",
                "gemini-flash-latest",
                "gemini-3.1-flash-lite-preview",
                "gemini-2.5-flash"
            )

            var lastErrorMessage = ""
            for (model in candidateModels) {
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                    val request = Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()

                    if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                        val rootObj = JSONObject(responseBody)
                        val candidates = rootObj.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)
                            val content = candidate.optJSONObject("content")
                            val parts = content?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                val text = parts.getJSONObject(0).optString("text")
                                if (text.isNotBlank()) {
                                    Log.d("GeminiClinicalService", "Generated response with model $model")
                                    return@withContext Result.success(text.trim())
                                }
                            }
                        }
                    } else {
                        lastErrorMessage = "Model $model returned code ${response.code}: $responseBody"
                        Log.w("GeminiClinicalService", lastErrorMessage)
                    }
                } catch (subException: Exception) {
                    lastErrorMessage = "Model $model failed: ${subException.message}"
                    Log.w("GeminiClinicalService", lastErrorMessage)
                }
            }

            Result.failure(RuntimeException("Gemini API could not generate response: $lastErrorMessage"))
        } catch (e: Exception) {
            Log.e("GeminiClinicalService", "Exception during Gemini request", e)
            Result.failure(e)
        }
    }
}
