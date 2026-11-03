package com.example.woundmanagement.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.woundmanagement.WoundManagementApp
import com.example.woundmanagement.data.db.entity.WoundAssessmentEntity
import com.example.woundmanagement.data.repository.AuditRepository
import com.example.woundmanagement.data.repository.WoundRepository
import com.example.woundmanagement.ui.components.BodyZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NewAssessmentViewModel(
    private val woundRepository: WoundRepository,
    private val auditRepository: AuditRepository
) : ViewModel() {

    private val _suggestedName = MutableStateFlow("")
    val suggestedName: StateFlow<String> = _suggestedName

    /**
     * Carica il nome suggerito per una nuova ferita in base al distretto corporeo.
     * [lang]: il nome finisce nel database, quindi deve seguire la lingua dell'app
     * (prima usava sempre l'etichetta italiana, anche con l'app in inglese).
     */
    fun loadSuggestedName(patientId: Long, location: String, lang: com.example.woundmanagement.ui.strings.AppLanguage) {
        viewModelScope.launch {
            val count = woundRepository.getCountByLocation(patientId, location)
            val zoneLabel = BodyZone.entries
                .firstOrNull { it.dbLocations.contains(location) || it.name == location }
                ?.label(lang)
                ?: location.replace("_", " ").lowercase().replaceFirstChar { it.uppercaseChar() }
            _suggestedName.value = "$zoneLabel ${count + 1}"
        }
    }

    fun save(assessment: WoundAssessmentEntity, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            // Se il nome è vuoto, usa il nome suggerito
            val finalAssessment = if (assessment.name.isBlank()) {
                assessment.copy(name = _suggestedName.value.ifBlank { assessment.location })
            } else assessment
            val id = woundRepository.insert(finalAssessment)
            auditRepository.log(
                "CREATE_ASSESSMENT", "WoundAssessment", id,
                "Patient=${finalAssessment.patientId}, location=${finalAssessment.location}, name=${finalAssessment.name}"
            )
            onSaved(id)
        }
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = WoundManagementApp.instance.database
                return NewAssessmentViewModel(
                    WoundRepository(db.woundAssessmentDao()),
                    AuditRepository(db.auditDao())
                ) as T
            }
        }
    }
}
