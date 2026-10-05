package com.example.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.MedVisionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val isDoctorPortal: Boolean = true,
    val username: String = "dr.jenkins",
    val password: String = "clinicianPass123",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // Email verification flow state
    val showEmailVerificationModal: Boolean = false,
    val verificationEmailInput: String = "",
    val isSendingVerificationEmail: Boolean = false,
    val verificationEmailSent: Boolean = false,
    val generatedSetupLink: String? = null,
    val activeVerificationToken: String? = null,

    // Credential setup from link state
    val showCredentialSetupModal: Boolean = false,
    val setupEmail: String = "",
    val setupUsernameInput: String = "",
    val setupPasswordInput: String = "",
    val setupConfirmPasswordInput: String = "",
    val isSavingCredentials: Boolean = false,
    val credentialSetupError: String? = null
)

sealed class LoginEvent {
    data class LoginSuccess(val role: String) : LoginEvent()
    data class ShowToast(val message: String) : LoginEvent()
}

class LoginViewModel(private val repository: MedVisionRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>()
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    fun onPortalToggled(isDoctor: Boolean) {
        _uiState.update {
            it.copy(
                isDoctorPortal = isDoctor,
                username = if (isDoctor) "dr.jenkins" else "eleanor.vance",
                password = if (isDoctor) "clinicianPass123" else "patientPass123",
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun onUsernameChanged(name: String) {
        _uiState.update { it.copy(username = name, errorMessage = null) }
    }

    fun onPasswordChanged(pass: String) {
        _uiState.update { it.copy(password = pass, errorMessage = null) }
    }

    fun onOpenVerificationDialog() {
        val defaultEmail = if (_uiState.value.isDoctorPortal) "clinician@hospital.org" else "patient@gmail.com"
        _uiState.update {
            it.copy(
                showEmailVerificationModal = true,
                verificationEmailInput = defaultEmail,
                verificationEmailSent = false,
                generatedSetupLink = null,
                activeVerificationToken = null,
                errorMessage = null
            )
        }
    }

    fun onCloseVerificationDialog() {
        _uiState.update { it.copy(showEmailVerificationModal = false) }
    }

    fun onVerificationEmailInputChanged(email: String) {
        _uiState.update { it.copy(verificationEmailInput = email, errorMessage = null) }
    }

    fun sendInstantVerificationEmail() {
        val email = _uiState.value.verificationEmailInput.trim()
        val role = if (_uiState.value.isDoctorPortal) "HEALTHCARE_WORKER" else "PATIENT"

        if (email.isBlank() || !email.contains("@")) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingVerificationEmail = true, errorMessage = null) }
            val result = repository.sendVerificationEmail(email, role)
            _uiState.update { it.copy(isSendingVerificationEmail = false) }

            result.onSuccess { details ->
                _uiState.update {
                    it.copy(
                        verificationEmailSent = true,
                        generatedSetupLink = details.setupLink,
                        activeVerificationToken = details.token,
                        setupEmail = details.email
                    )
                }
                _events.emit(LoginEvent.ShowToast("Instant verification email sent to $email"))
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Failed to send email") }
            }
        }
    }

    fun onOpenCredentialSetupFromLink() {
        val current = _uiState.value
        val derivedUsername = current.setupEmail.substringBefore("@").replace(".", "_")
        _uiState.update {
            it.copy(
                showEmailVerificationModal = false,
                showCredentialSetupModal = true,
                setupUsernameInput = derivedUsername,
                setupPasswordInput = "",
                setupConfirmPasswordInput = "",
                credentialSetupError = null
            )
        }
    }

    fun onCloseCredentialSetupModal() {
        _uiState.update { it.copy(showCredentialSetupModal = false) }
    }

    fun onSetupUsernameChanged(username: String) {
        _uiState.update { it.copy(setupUsernameInput = username, credentialSetupError = null) }
    }

    fun onSetupPasswordChanged(password: String) {
        _uiState.update { it.copy(setupPasswordInput = password, credentialSetupError = null) }
    }

    fun onSetupConfirmPasswordChanged(password: String) {
        _uiState.update { it.copy(setupConfirmPasswordInput = password, credentialSetupError = null) }
    }

    fun saveCredentialsAndCompleteSetup() {
        val state = _uiState.value
        val user = state.setupUsernameInput.trim()
        val pass = state.setupPasswordInput.trim()
        val confirmPass = state.setupConfirmPasswordInput.trim()
        val token = state.activeVerificationToken ?: "MV-VERIFY-DIRECT"
        val email = state.setupEmail.ifBlank { "user@hospital.org" }

        if (user.length < 3) {
            _uiState.update { it.copy(credentialSetupError = "Username must be at least 3 characters") }
            return
        }
        if (pass.length < 4) {
            _uiState.update { it.copy(credentialSetupError = "Password must be at least 4 characters") }
            return
        }
        if (pass != confirmPass) {
            _uiState.update { it.copy(credentialSetupError = "Passwords do not match") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingCredentials = true, credentialSetupError = null) }
            val result = repository.completeCredentialSetup(token, email, user, pass)
            _uiState.update { it.copy(isSavingCredentials = false) }

            result.onSuccess {
                _uiState.update {
                    it.copy(
                        showCredentialSetupModal = false,
                        username = user,
                        password = pass,
                        successMessage = "Credentials set up successfully! Log in with your new username and password.",
                        errorMessage = null
                    )
                }
                _events.emit(LoginEvent.ShowToast("Login credentials configured successfully"))
            }.onFailure { err ->
                _uiState.update { it.copy(credentialSetupError = err.localizedMessage ?: "Failed to save credentials") }
            }
        }
    }

    fun login() {
        val currentState = _uiState.value
        if (currentState.username.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter username") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.login(currentState.username, currentState.password)
            _uiState.update { it.copy(isLoading = false) }

            result.onSuccess {
                val role = if (currentState.isDoctorPortal) "HEALTHCARE_WORKER" else "PATIENT"
                _events.emit(LoginEvent.LoginSuccess(role))
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.localizedMessage ?: "Authentication failed") }
            }
        }
    }

    fun authenticateWithBiometrics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.authenticateBiometric()
            _uiState.update { it.copy(isLoading = false) }

            result.onSuccess {
                val role = if (_uiState.value.isDoctorPortal) "HEALTHCARE_WORKER" else "PATIENT"
                _events.emit(LoginEvent.LoginSuccess(role))
            }.onFailure {
                _uiState.update { it.copy(errorMessage = "Biometric authentication failed") }
            }
        }
    }
}
