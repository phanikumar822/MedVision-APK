package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.repository.MedVisionRepository
import com.example.ui.auth.LoginScreen
import com.example.ui.auth.LoginViewModel
import com.example.ui.doctor.CameraFundusCaptureScreen
import com.example.ui.doctor.DoctorDashboardScreen
import com.example.ui.doctor.DoctorViewModel
import com.example.ui.doctor.ScreeningResultScreen
import com.example.ui.patient.PatientPortalScreen
import com.example.ui.patient.PatientViewModel

object AppDestinations {
    const val LOGIN = "login"
    const val DOCTOR_DASHBOARD = "doctor_dashboard"
    const val CAMERA_CAPTURE = "camera_capture/{patientId}"
    const val SCREENING_RESULT = "screening_result/{screeningId}"
    const val PATIENT_PORTAL = "patient_portal"

    fun cameraRoute(patientId: Int) = "camera_capture/$patientId"
    fun resultRoute(screeningId: String) = "screening_result/$screeningId"
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val context = LocalContext.current.applicationContext

    // Central Repository singleton
    val repository = remember { MedVisionRepository(context) }

    // Start destination based on session
    val startDestination = remember {
        if (repository.sessionManager.hasPreviousSession()) {
            if (repository.sessionManager.getUserRole() == "PATIENT") {
                AppDestinations.PATIENT_PORTAL
            } else {
                AppDestinations.DOCTOR_DASHBOARD
            }
        } else {
            AppDestinations.LOGIN
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // 1. Auth Login Screen
        composable(AppDestinations.LOGIN) {
            val loginViewModel = remember { LoginViewModel(repository) }
            LoginScreen(
                viewModel = loginViewModel,
                onNavigateToDoctor = {
                    navController.navigate(AppDestinations.DOCTOR_DASHBOARD) {
                        popUpTo(AppDestinations.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToPatient = {
                    navController.navigate(AppDestinations.PATIENT_PORTAL) {
                        popUpTo(AppDestinations.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // 2. Doctor Dashboard Screen
        composable(AppDestinations.DOCTOR_DASHBOARD) {
            val doctorViewModel = remember { DoctorViewModel(repository) }
            DoctorDashboardScreen(
                viewModel = doctorViewModel,
                onNavigateToCamera = { patientId ->
                    navController.navigate(AppDestinations.cameraRoute(patientId))
                },
                onNavigateToResult = { screeningId ->
                    navController.navigate(AppDestinations.resultRoute(screeningId))
                },
                onLogout = {
                    navController.navigate(AppDestinations.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // 3. Camera Fundus Capture Screen
        composable(
            route = AppDestinations.CAMERA_CAPTURE,
            arguments = listOf(navArgument("patientId") { type = NavType.IntType; defaultValue = 1 })
        ) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getInt("patientId") ?: 1
            CameraFundusCaptureScreen(
                patientId = patientId,
                repository = repository,
                onNavigateBack = { navController.popBackStack() },
                onScreeningComplete = { screeningId ->
                    navController.navigate(AppDestinations.resultRoute(screeningId)) {
                        popUpTo(AppDestinations.DOCTOR_DASHBOARD)
                    }
                }
            )
        }

        // 4. Screening Result & Grad-CAM Heatmap Viewer Screen
        composable(
            route = AppDestinations.SCREENING_RESULT,
            arguments = listOf(navArgument("screeningId") { type = NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            val screeningId = backStackEntry.arguments?.getString("screeningId") ?: ""
            ScreeningResultScreen(
                screeningId = screeningId,
                repository = repository,
                onNavigateBack = { navController.popBackStack() },
                onStartNewScreening = {
                    navController.navigate(AppDestinations.cameraRoute(1)) {
                        popUpTo(AppDestinations.DOCTOR_DASHBOARD)
                    }
                }
            )
        }

        // 5. Patient Health Portal Screen
        composable(AppDestinations.PATIENT_PORTAL) {
            val patientViewModel = remember { PatientViewModel(repository) }
            PatientPortalScreen(
                viewModel = patientViewModel,
                onLogout = {
                    navController.navigate(AppDestinations.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
