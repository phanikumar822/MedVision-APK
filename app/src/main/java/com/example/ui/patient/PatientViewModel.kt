package com.example.ui.patient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocalChatMessage
import com.example.data.model.ReportItemDto
import com.example.data.repository.MedVisionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PatientPortalUiState(
    val patientName: String = "",
    val patientIdTag: String = "",
    val reports: List<ReportItemDto> = emptyList(),
    val isLoadingReports: Boolean = false,
    val currentChatInput: String = "",
    val isSendingChatMessage: Boolean = false,
    val selectedReportForViewer: ReportItemDto? = null
)

class PatientViewModel(private val repository: MedVisionRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientPortalUiState())
    val uiState: StateFlow<PatientPortalUiState> = _uiState.asStateFlow()

    val chatMessages: StateFlow<List<LocalChatMessage>> = repository.chatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadPatientData()
    }

    fun loadPatientData() {
        val username = repository.sessionManager.getUsername().ifBlank { "Patient" }
        val id = repository.sessionManager.getUserId()
        _uiState.update {
            it.copy(
                patientName = username,
                patientIdTag = "#PT-$id",
                isLoadingReports = true
            )
        }

        viewModelScope.launch {
            val reportsResult = repository.getPatientReports()

            reportsResult.onSuccess { list ->
                _uiState.update {
                    it.copy(
                        reports = list,
                        isLoadingReports = false,
                        selectedReportForViewer = list.firstOrNull()
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isLoadingReports = false) }
            }
        }
    }

    fun updateChatInput(text: String) {
        _uiState.update { it.copy(currentChatInput = text) }
    }

    fun sendChatMessage(presetQuery: String? = null) {
        val messageToSend = presetQuery ?: _uiState.value.currentChatInput
        if (messageToSend.isBlank()) return

        val activeScreeningId = _uiState.value.selectedReportForViewer?.screeningId
        _uiState.update { it.copy(currentChatInput = "", isSendingChatMessage = true) }

        viewModelScope.launch {
            repository.sendChatMessage(messageToSend.trim(), activeScreeningId)
            _uiState.update { it.copy(isSendingChatMessage = false) }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChatHistory()
        }
    }

    fun selectReport(report: ReportItemDto) {
        _uiState.update { it.copy(selectedReportForViewer = report) }
    }

    fun deleteReport(screeningId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteScreening(screeningId)
            loadPatientData()
            onComplete(true)
        }
    }

    fun deleteAllPatientRecords(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val patientId = repository.sessionManager.getUserId()
            repository.deletePatient(patientId)
            loadPatientData()
            onComplete(true)
        }
    }

    fun isGeminiConfigured(): Boolean = repository.isGeminiApiConfigured()

    fun getCustomApiKey(): String? = repository.getCustomApiKey()

    fun setCustomApiKey(key: String?) {
        repository.setCustomApiKey(key)
    }

    fun logout() {
        repository.sessionManager.clearSession()
    }
}
