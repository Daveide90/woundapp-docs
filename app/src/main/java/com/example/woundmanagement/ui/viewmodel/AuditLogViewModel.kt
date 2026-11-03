package com.example.woundmanagement.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.woundmanagement.WoundManagementApp
import com.example.woundmanagement.data.db.entity.AuditLogEntity
import com.example.woundmanagement.data.repository.AuditRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

class AuditLogViewModel(private val auditRepository: AuditRepository) : ViewModel() {

    // catch: al timeout il database si chiude con il registro ancora a schermo; senza,
    // l'errore della query risalirebbe alla UI facendo chiudere l'app.
    val logs: Flow<List<AuditLogEntity>> = auditRepository.getAll()
        .catch { emit(emptyList()) }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = WoundManagementApp.instance.database
                return AuditLogViewModel(AuditRepository(db.auditDao())) as T
            }
        }
    }
}
