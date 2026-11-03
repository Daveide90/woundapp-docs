package com.example.woundmanagement.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.woundmanagement.WoundManagementApp
import com.example.woundmanagement.data.repository.AuditRepository
import com.example.woundmanagement.security.BackupManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class BackupViewModel : ViewModel() {

    /** Codici errore neutri: la UI li mappa a stringhe localizzate (4 lingue). */
    enum class BackupError { CREATE_FAILED, WRONG_PASSWORD, READ_FAILED, RESTORE_FAILED }

    sealed class BackupState {
        object Idle : BackupState()
        object Working : BackupState()
        data class BackupReady(val file: File) : BackupState()
        data class PreviewReady(val summary: BackupManager.BackupSummary, val uri: Uri) : BackupState()
        /** [result]: esito del merge (pazienti nuovi/aggiornati), mostrato dalla UI. */
        data class RestoreComplete(
            val summary: BackupManager.BackupSummary,
            val result: BackupManager.RestoreResult
        ) : BackupState()
        data class Error(val error: BackupError) : BackupState()
    }

    private val _state = MutableStateFlow<BackupState>(BackupState.Idle)
    val state: StateFlow<BackupState> = _state

    private val db = WoundManagementApp.instance.database
    private val auditRepo = AuditRepository(db.auditDao())

    /**
     * Tiene viva la sessione mentre un'operazione lunga è in corso. Backup, anteprima e
     * ripristino non generano tocchi sullo schermo: su archivi grandi (PBKDF2 a 600k
     * iterazioni, centinaia di foto) l'operazione può superare i 5 minuti di timeout, che
     * scatterebbe A METÀ chiudendo il database mentre lo si sta usando.
     */
    private fun CoroutineScope.startSessionKeepAlive(context: Context): Job {
        val appContext = context.applicationContext
        return launch {
            while (true) {
                com.example.woundmanagement.security.AppPreferences.updateLastActivityTime(appContext)
                kotlinx.coroutines.delay(30_000)
            }
        }
    }

    fun createBackup(context: Context, password: String) {
        _state.value = BackupState.Working
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val keepAlive = startSessionKeepAlive(context)
            try {
                val file = BackupManager.createBackup(context, db, password)
                auditRepo.log("BACKUP_CREATED", "BACKUP", details = "Backup creato")
                _state.value = BackupState.BackupReady(file)
            } catch (e: Throwable) {
                _state.value = BackupState.Error(BackupError.CREATE_FAILED)
            } finally {
                keepAlive.cancel()
            }
        }
    }

    fun previewRestore(context: Context, uri: Uri, password: String) {
        _state.value = BackupState.Working
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val keepAlive = startSessionKeepAlive(context)
            try {
                val summary = BackupManager.peekBackup(context, uri, password)
                _state.value = BackupState.PreviewReady(summary, uri)
            } catch (e: javax.crypto.AEADBadTagException) {
                // MAGIC header già verificato in decrypt: se la decifratura GCM fallisce qui,
                // l'header era valido ma il tag no -> password errata (distinta dal file
                // corrotto/non valido, che lancia IllegalArgumentException prima del GCM).
                _state.value = BackupState.Error(BackupError.WRONG_PASSWORD)
            } catch (e: Throwable) {
                _state.value = BackupState.Error(BackupError.READ_FAILED)
            } finally {
                keepAlive.cancel()
            }
        }
    }

    fun confirmRestore(context: Context, uri: Uri, password: String) {
        val preview = (_state.value as? BackupState.PreviewReady)?.summary
        _state.value = BackupState.Working
        // Scope applicativo: il restore (transazione DB + scrittura foto) NON deve essere
        // cancellato se l'utente lascia la schermata a metà operazione.
        com.example.woundmanagement.WoundManagementApp.instance.applicationScope
            .launch(kotlinx.coroutines.Dispatchers.IO) {
            val keepAlive = startSessionKeepAlive(context)
            try {
                val result = BackupManager.restoreBackup(context, db, uri, password)
                val summary = preview ?: BackupManager.peekBackup(context, uri, password)
                auditRepo.log("BACKUP_RESTORED", "BACKUP",
                    details = "Ripristino: ${summary.patients} pazienti, ${summary.wounds} lesioni " +
                        "(merge: ${result.newPatients} nuovi, ${result.updatedPatients} aggiornati)")
                _state.value = BackupState.RestoreComplete(summary, result)
            } catch (e: javax.crypto.AEADBadTagException) {
                _state.value = BackupState.Error(BackupError.WRONG_PASSWORD)
            } catch (e: Throwable) {
                _state.value = BackupState.Error(BackupError.RESTORE_FAILED)
            } finally {
                keepAlive.cancel()
            }
        }
    }

    fun resetState() {
        _state.value = BackupState.Idle
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = BackupViewModel() as T
        }
    }
}
