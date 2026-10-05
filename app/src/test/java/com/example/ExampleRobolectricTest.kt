package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SessionManager
import com.example.data.repository.MedVisionRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MedVisionAI", appName)
    }

    @Test
    fun `session manager initialization and role check`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionManager = SessionManager(context)
        assertNotNull(sessionManager)
        assertEquals("HEALTHCARE_WORKER", sessionManager.getUserRole())
    }

    @Test
    fun `chatbot answers clinical queries with grounded context`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = MedVisionRepository(context)
        val result = repo.sendChatMessage("What does my Grad-CAM heatmap show?", "MV-8A4F12C9")
        assertTrue(result.isSuccess)
        val answer = result.getOrNull()
        assertNotNull(answer)
        assertTrue(answer!!.contains("Grad-CAM") || answer.contains("retinal") || answer.contains("hotspots", ignoreCase = true))
    }
}

