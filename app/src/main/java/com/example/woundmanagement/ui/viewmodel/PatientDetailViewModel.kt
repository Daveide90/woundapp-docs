package com.example.woundmanagement.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.woundmanagement.WoundManagementApp
import com.example.woundmanagement.data.db.dao.*
import com.example.woundmanagement.data.db.entity.DressingEventEntity
import com.example.woundmanagement.data.db.entity.PatientEntity
import com.example.woundmanagement.data.db.entity.WoundAssessmentEntity
import com.example.woundmanagement.data.repository.DressingRepository
import com.example.woundmanagement.data.repository.PatientRepository
import com.example.woundmanagement.data.repository.WoundRepository
import com.example.woundmanagement.ui.screens.ScaleHistoryData
import com.example.woundmanagement.data.repository.AuditRepository
import com.example.woundmanagement.security.PhotoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PatientDetailViewModel(
    private val patientId: Long,
    private val patientRepository: PatientRepository,
    private val woundRepository: WoundRepository,
    private val dressingRepository: DressingRepository,
    private val nortonDao: NortonDao,
    private val nrs2002Dao: NRS2002Dao,
    private val painNrsDao: PainNRSDao,
    private val dressingEventDao: DressingEventDao,
    private val woundAssessmentDao: WoundAssessmentDao,
    private val dressingPhotoDao: DressingPhotoDao
) : ViewModel() {

    // Come per le scale: le operazioni di scrittura girano sullo scope applicativo,
    // così non vengono cancellate se la schermata viene chiusa a metà operazione.
    private val writeScope = WoundManagementApp.instance.applicationScope

    val patient: Flow<PatientEntity?> = patientRepository.getByIdFlow(patientId)
        .catch { emit(null) }

    val assessments: Flow<List<WoundAssessmentEntity>> = woundRepository.getByPatient(patientId)
        .catch { emit(emptyList()) }

    fun updatePatient(patient: PatientEntity, auditRepository: AuditRepository? = null) {
        // writeScope come le altre scritture: su viewModelScope una modifica salvata e
        // seguita subito da un "indietro" veniva cancellata a metà (dato perso in silenzio).
        writeScope.launch(Dispatchers.IO) {
            patientRepository.update(patient)
            auditRepository?.log("UPDATE_PATIENT", "Patient", patientId)
        }
    }

    /**
     * Elimina il file fisico della foto cifrata di un evento. Supporta nomi file
     * relativi (UUID.jpg.enc), path assoluti legacy ("/data/...") e URI legacy
     * "file:///data/..." (C10a: senza la normalizzazione il prefisso "file://"
     * finiva nel ramo relativo e il file non veniva mai cancellato).
     */
    private fun deletePhotoFile(context: android.content.Context, photoRef: String) {
        if (photoRef.isBlank()) return
        try {
            val encDir = PhotoManager.getEncryptedDir(context)
            val normalized = if (photoRef.startsWith("file://")) {
                android.net.Uri.parse(photoRef).path ?: photoRef.removePrefix("file://")
            } else {
                photoRef
            }
            val file = if (normalized.startsWith("/")) File(normalized) else File(encDir, normalized)
            file.delete()
        } catch (_: Exception) {}
    }

    /**
     * Cancella i file di TUTTE le foto (tabella dressing_photos) degli eventi indicati.
     * Da chiamare PRIMA del delete sul DB: il CASCADE elimina solo le righe, non i file.
     * Best-effort come deletePhotoFile (idempotente: la copertina è anche position 0).
     */
    private suspend fun deleteAllDressingPhotoFiles(context: android.content.Context, eventIds: List<Long>) {
        if (eventIds.isEmpty()) return
        try {
            dressingPhotoDao.getByEvents(eventIds).forEach { photo ->
                deletePhotoFile(context, photo.photoUri)
            }
        } catch (_: Exception) {}
    }

    fun deletePatient(auditRepository: AuditRepository, context: android.content.Context, onComplete: () -> Unit) {
        // writeScope (non viewModelScope): la cancellazione non deve interrompersi a metà
        // se l'utente naviga via — file foto già eliminati ma record ancora nel DB.
        writeScope.launch(Dispatchers.IO) {
            // Bozze delle lesioni del paziente (con i loro file foto non referenziati)
            runCatching {
                woundRepository.getByPatient(patientId).first().forEach { w -> clearWoundDraft(context, w.id) }
            }
            // Elimina i file fisici delle foto cifrate prima di cancellare il DB
            val dressingEvents = dressingRepository.getByPatient(patientId).first()
            // Tutte le foto extra (dressing_photos) + copertina legacy (photoUri)
            deleteAllDressingPhotoFiles(context, dressingEvents.map { it.eventId })
            dressingEvents.forEach { event -> deletePhotoFile(context, event.photoUri) }
            // Cancella il paziente: CASCADE elimina WoundAssessment, DressingEvent, scale, ecc.
            val patient = patientRepository.getById(patientId)
            if (patient != null) {
                patientRepository.delete(patient)
                auditRepository.log("DELETE_PATIENT", "Patient", patientId)
            }
            withContext(Dispatchers.Main) { onComplete() }
        }
    }

    /** Bozza medicazione della lesione: dati + file foto non ancora referenziati dal DB. */
    private fun clearWoundDraft(context: android.content.Context, woundId: Long) {
        runCatching {
            com.example.woundmanagement.security.DressingDraftManager.load(context, woundId)?.let { d ->
                (listOf(d.photoUri) + d.extraPhotoUris).forEach { s ->
                    if (!s.isNullOrBlank()) runCatching {
                        android.net.Uri.parse(s).path?.let { p -> File(p).delete() }
                    }
                }
            }
            com.example.woundmanagement.security.DressingDraftManager.clear(context, woundId)
        }
    }

    /** Elimina una lesione con tutte le sue medicazioni e le relative foto cifrate. */
    fun deleteWound(auditRepository: AuditRepository, context: android.content.Context, woundId: Long, onComplete: () -> Unit) {
        writeScope.launch(Dispatchers.IO) {
            clearWoundDraft(context, woundId)
            val events = dressingEventDao.getByAssessmentOnce(woundId)
            deleteAllDressingPhotoFiles(context, events.map { it.id })
            events.forEach { deletePhotoFile(context, it.photoUri) }
            woundAssessmentDao.getById(woundId)?.let { wound ->
                woundAssessmentDao.delete(wound) // CASCADE elimina i dressing_events
                auditRepository.log("DELETE_WOUND", "WoundAssessment", woundId, "events=${events.size}")
            }
            withContext(Dispatchers.Main) { onComplete() }
        }
    }

    /** Elimina un singolo record di medicazione e la sua foto cifrata. */
    fun deleteDressing(auditRepository: AuditRepository, context: android.content.Context, eventId: Long, onComplete: () -> Unit) {
        writeScope.launch(Dispatchers.IO) {
            dressingEventDao.getById(eventId)?.let { event ->
                deleteAllDressingPhotoFiles(context, listOf(event.id))
                deletePhotoFile(context, event.photoUri)
                dressingEventDao.delete(event)
                auditRepository.log("DELETE_DRESSING", "DressingEvent", event.id, "assessment=${event.assessmentId}")
            }
            withContext(Dispatchers.Main) { onComplete() }
        }
    }

    /** Correzione documentale: data e note di una medicazione esistente. */
    fun updateDressingDateNotes(auditRepository: AuditRepository, eventId: Long, newDate: Long, newNotes: String, onComplete: () -> Unit = {}) {
        writeScope.launch(Dispatchers.IO) {
            dressingEventDao.getById(eventId)?.let { event ->
                dressingEventDao.update(event.copy(createdAt = newDate, notes = newNotes))
                auditRepository.log("UPDATE_DRESSING", "DressingEvent", eventId, "date/notes edited")
            }
            withContext(Dispatchers.Main) { onComplete() }
        }
    }

    /** Aggiorna le note/anamnesi di una lesione. */
    fun updateWoundNotes(auditRepository: AuditRepository, woundId: Long, newNotes: String) {
        writeScope.launch(Dispatchers.IO) {
            woundAssessmentDao.getById(woundId)?.let { wound ->
                woundAssessmentDao.update(wound.copy(notes = newNotes))
                auditRepository.log("UPDATE_WOUND", "WoundAssessment", woundId, "notes edited")
            }
        }
    }

    val dressingHistory: Flow<List<DressingEventWithWound>> = dressingRepository.getByPatient(patientId)
        .catch { emit(emptyList()) }

    val scaleHistoryData: Flow<ScaleHistoryData> = combine(
        nortonDao.getByPatient(patientId),
        nrs2002Dao.getByPatient(patientId)
    ) { norton, nrs2002 ->
        ScaleHistoryData(
            nortonScores = norton.map { it.createdAt to it.totalScore },
            nrs2002Scores = nrs2002.map { it.createdAt to it.totalScore }
        )
    }.combine(painNrsDao.getByPatient(patientId)) { data, painNrs ->
        data.copy(painNrsScores = painNrs.map { it.createdAt to it.painScore })
    }.catch { emit(ScaleHistoryData()) }

    companion object {
        fun factory(patientId: Long) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = WoundManagementApp.instance.database
                return PatientDetailViewModel(
                    patientId,
                    PatientRepository(db.patientDao()),
                    WoundRepository(db.woundAssessmentDao()),
                    DressingRepository(db.dressingEventDao()),
                    db.nortonDao(),
                    db.nrs2002Dao(),
                    db.painNrsDao(),
                    db.dressingEventDao(),
                    db.woundAssessmentDao(),
                    db.dressingPhotoDao()
                ) as T
            }
        }
    }
}
