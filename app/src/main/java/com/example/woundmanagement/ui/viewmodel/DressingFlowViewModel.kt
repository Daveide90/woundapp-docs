package com.example.woundmanagement.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.woundmanagement.WoundManagementApp
import com.example.woundmanagement.data.db.entity.DressingEventEntity
import com.example.woundmanagement.data.repository.AuditRepository
import com.example.woundmanagement.data.repository.DressingRepository
import com.example.woundmanagement.domain.usecase.CreateDressingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DressingFlowViewModel(
    private val createDressingUseCase: CreateDressingUseCase,
    private val dressingRepository: DressingRepository,
    private val auditRepository: AuditRepository
) : ViewModel() {

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState = _saveState.asStateFlow()

    sealed class SaveState {
        data object Idle : SaveState()
        data object Saving : SaveState()
        data object Success : SaveState()
        data class Error(val message: String) : SaveState()
    }

    fun saveDressing(
        event: DressingEventEntity,
        extraPhotoUris: List<String> = emptyList(),
        // Bozza da cancellare ad insert riuscito: farlo QUI (stesso scope non cancellabile
        // dell'insert) chiude la finestra "evento salvato ma bozza fantasma ancora viva",
        // che al successivo "Scarta bozza" cancellerebbe file foto ormai referenziati.
        clearDraft: (suspend () -> Unit)? = null
    ) {
        // applicationScope, NON viewModelScope: il back/navigazione durante il "Saving"
        // non deve cancellare l'insert a metà (medicazione persa in silenzio).
        WoundManagementApp.instance.applicationScope.launch {
            _saveState.value = SaveState.Saving
            try {
                createDressingUseCase(event, extraPhotoUris)
                runCatching { clearDraft?.invoke() }
                _saveState.value = SaveState.Success
            } catch (e: Exception) {
                if (com.example.woundmanagement.BuildConfig.DEBUG) android.util.Log.e("DressingFlowVM", "Error saving dressing event", e)
                _saveState.value = SaveState.Error("Errore nel salvataggio")
            }
        }
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = WoundManagementApp.instance.database
                val dressingRepo = DressingRepository(db.dressingEventDao())
                val auditRepo = AuditRepository(db.auditDao())
                return DressingFlowViewModel(
                    CreateDressingUseCase(dressingRepo, auditRepo, db.dressingPhotoDao(), db),
                    dressingRepo,
                    auditRepo
                ) as T
            }
        }
    }
}
