package com.example.ui.doctor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CachedPatient
import com.example.data.local.CachedScreening
import com.example.data.model.ScreenStatsDto
import com.example.data.repository.MedVisionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DoctorDashboardUiState(
    val selectedTab: DoctorTab = DoctorTab.SCREENING,
    val stats: ScreenStatsDto = ScreenStatsDto(0, 0, 0, 0),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val doctorName: String = "Clinician Workstation",
    val showAddPatientDialog: Boolean = false,
    val newPatientFirstName: String = "",
    val newPatientLastName: String = "",
    val newPatientEmail: String = "",
    val newPatientPhone: String = "",
    val latestScreening: CachedScreening? = null
)

enum class DoctorTab {
    SCREENING,
    PATIENTS
}

class DoctorViewModel(private val repository: MedVisionRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(DoctorDashboardUiState())
    val uiState: StateFlow<DoctorDashboardUiState> = _uiState.asStateFlow()

    val patients: StateFlow<List<CachedPatient>> = repository.cachedPatients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val screenings: StateFlow<List<CachedScreening>> = repository.cachedScreenings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val statsResult = repository.getScreeningStats()
            repository.refreshPatients()

            statsResult.onSuccess { stats ->
                _uiState.update { it.copy(stats = stats, isLoading = false) }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun selectTab(tab: DoctorTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openAddPatientDialog() {
        _uiState.update { it.copy(showAddPatientDialog = true) }
    }

    fun closeAddPatientDialog() {
        _uiState.update {
            it.copy(
                showAddPatientDialog = false,
                newPatientFirstName = "",
                newPatientLastName = "",
                newPatientEmail = "",
                newPatientPhone = ""
            )
        }
    }

    fun updateNewPatientField(firstName: String, lastName: String, email: String, phone: String) {
        _uiState.update {
            it.copy(
                newPatientFirstName = firstName,
                newPatientLastName = lastName,
                newPatientEmail = email,
                newPatientPhone = phone
            )
        }
    }

    fun registerPatient(onSuccess: (CachedPatient) -> Unit, onError: (String) -> Unit) {
        val s = _uiState.value
        if (s.newPatientFirstName.isBlank() || s.newPatientLastName.isBlank()) {
            onError("Please enter both first and last name")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = repository.addPatient(
                firstName = s.newPatientFirstName.trim(),
                lastName = s.newPatientLastName.trim(),
                email = s.newPatientEmail.trim(),
                phone = s.newPatientPhone.trim()
            )
            _uiState.update { it.copy(isLoading = false) }

            res.onSuccess { dto ->
                closeAddPatientDialog()
                onSuccess(
                    CachedPatient(
                        id = dto.id,
                        firstName = dto.firstName,
                        lastName = dto.lastName,
                        email = dto.email,
                        phone = dto.phone,
                        createdAt = dto.createdAt
                    )
                )
            }.onFailure { err ->
                onError(err.message ?: "Failed to add patient")
            }
        }
    }

    fun submitNewPatient(onSuccess: (CachedPatient) -> Unit = {}, onError: (String) -> Unit = {}) {
        registerPatient(onSuccess, onError)
    }

    fun deletePatient(patientId: Int, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.deletePatient(patientId)
            repository.getScreeningStats().onSuccess { stats ->
                _uiState.update { it.copy(stats = stats) }
            }
            _uiState.update { it.copy(isLoading = false) }
            onComplete(result.isSuccess)
        }
    }

    fun deleteScreening(screeningId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.deleteScreening(screeningId)
            repository.getScreeningStats().onSuccess { stats ->
                _uiState.update { it.copy(stats = stats) }
            }
            onComplete(result.isSuccess)
        }
    }

    fun logout() {
        repository.sessionManager.clearSession()
    }
}
