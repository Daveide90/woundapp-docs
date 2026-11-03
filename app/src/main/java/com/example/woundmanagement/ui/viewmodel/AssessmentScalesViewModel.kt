package com.example.woundmanagement.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.woundmanagement.WoundManagementApp
import com.example.woundmanagement.data.db.dao.*
import com.example.woundmanagement.data.db.entity.*
import com.example.woundmanagement.data.repository.AuditRepository
import com.example.woundmanagement.domain.scoring.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AssessmentScalesViewModel(
    private val patientId: Long,
    private val nortonDao: NortonDao,
    private val nrs2002Dao: NRS2002Dao,
    private val painNrsDao: PainNRSDao,
    private val auditRepository: AuditRepository
) : ViewModel() {

    // NB: gli storici delle scale si leggono dalla scheda paziente (PatientDetailViewModel):
    // qui erano esposti tre Flow che nessuna schermata osservava.

    // NB: il salvataggio gira su un CoroutineScope APPLICATIVO (non viewModelScope), così
    // l'insert non viene cancellato quando la schermata fa popBackStack distruggendo il
    // ViewModel. La callback onSaved viene invocata sul thread Main solo a insert riuscito,
    // e solo allora la UI può navigare indietro: niente più punteggi persi in silenzio.
    private val saveScope = WoundManagementApp.instance.applicationScope

    fun saveNorton(score: NortonScale.NortonScore, onSaved: () -> Unit) {
        saveScope.launch {
            val entity = NortonAssessmentEntity(
                patientId = patientId,
                condizioneFisica = score.condizioneFisica,
                statoMentale = score.statoMentale,
                attivita = score.attivita,
                mobilita = score.mobilita,
                incontinenza = score.incontinenza,
                totalScore = score.total
            )
            val id = nortonDao.insert(entity)
            auditRepository.log("CREATE_NORTON", "NortonAssessment", id, "Score: ${score.total}")
            withContext(Dispatchers.Main) { onSaved() }
        }
    }

    fun saveNRS2002(score: NRS2002Scale.NRS2002Score, onSaved: () -> Unit) {
        saveScope.launch {
            val entity = NRS2002AssessmentEntity(
                patientId = patientId,
                nutritionalScore = score.nutritionalScore,
                diseaseSeverityScore = score.diseaseSeverityScore,
                ageAdjustment = if (score.ageOver70) 1 else 0,
                totalScore = score.total
            )
            val id = nrs2002Dao.insert(entity)
            auditRepository.log("CREATE_NRS2002", "NRS2002Assessment", id, "Score: ${score.total}")
            withContext(Dispatchers.Main) { onSaved() }
        }
    }

    fun savePainNRS(
        painScore: Int,
        painLocation: String,
        painType: PainNRSScale.PainType,
        painCharacter: PainNRSScale.PainCharacter,
        aggravatingFactors: String,
        relievingFactors: String,
        onSaved: () -> Unit
    ) {
        saveScope.launch {
            val entity = PainNRSAssessmentEntity(
                patientId = patientId,
                painScore = painScore,
                painLocation = painLocation,
                painType = painType.name,
                painCharacter = painCharacter.name,
                aggravatingFactors = aggravatingFactors,
                relievingFactors = relievingFactors
            )
            val id = painNrsDao.insert(entity)
            auditRepository.log("CREATE_PAIN_NRS", "PainNRSAssessment", id, "Score: $painScore")
            withContext(Dispatchers.Main) { onSaved() }
        }
    }

    companion object {
        fun factory(patientId: Long) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = WoundManagementApp.instance.database
                return AssessmentScalesViewModel(
                    patientId,
                    db.nortonDao(),
                    db.nrs2002Dao(),
                    db.painNrsDao(),
                    AuditRepository(db.auditDao())
                ) as T
            }
        }
    }
}
