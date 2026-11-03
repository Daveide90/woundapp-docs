package com.example.woundmanagement.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.woundmanagement.WoundManagementApp
import com.example.woundmanagement.data.db.dao.PatientWithRisk
import com.example.woundmanagement.data.db.entity.PatientEntity
import com.example.woundmanagement.data.repository.AuditRepository
import com.example.woundmanagement.data.repository.PatientRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PatientListViewModel(
    private val patientRepository: PatientRepository,
    private val auditRepository: AuditRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    // catch come negli altri ViewModel: al timeout di sessione il database viene chiuso
    // mentre questa lista è ancora a schermo e la query di Room fallisce; senza catch
    // l'eccezione arriva alla UI e fa chiudere l'app invece di portare al login.
    val patientsWithRisk: Flow<List<PatientWithRisk>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) patientRepository.getAllWithRisk()
        else patientRepository.searchWithRisk(query)
    }.catch { emit(emptyList()) }

    fun search(query: String) { _searchQuery.value = query }

    fun addPatient(patient: PatientEntity) {
        viewModelScope.launch {
            val id = patientRepository.insert(patient)
            auditRepository.log("CREATE_PATIENT", "Patient", id)
        }
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = WoundManagementApp.instance.database
                return PatientListViewModel(
                    PatientRepository(db.patientDao()),
                    AuditRepository(db.auditDao())
                ) as T
            }
        }
    }
}
