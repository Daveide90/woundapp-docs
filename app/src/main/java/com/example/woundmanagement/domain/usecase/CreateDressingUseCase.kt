package com.example.woundmanagement.domain.usecase

import com.example.woundmanagement.data.db.dao.DressingPhotoDao
import com.example.woundmanagement.data.db.entity.DressingEventEntity
import com.example.woundmanagement.data.db.entity.DressingPhotoEntity
import com.example.woundmanagement.data.repository.AuditRepository
import com.example.woundmanagement.data.repository.DressingRepository
import androidx.room.withTransaction
import com.example.woundmanagement.data.db.AppDatabase

class CreateDressingUseCase(
    private val dressingRepository: DressingRepository,
    private val auditRepository: AuditRepository,
    private val dressingPhotoDao: DressingPhotoDao,
    private val db: AppDatabase
) {
    /**
     * @param extraPhotoUris foto aggiuntive (angolazioni 2..4) già normalizzate
     * (solo filename, come [DressingEventEntity.photoUri] per la principale).
     */
    suspend operator fun invoke(
        event: DressingEventEntity,
        extraPhotoUris: List<String> = emptyList()
    ): Long {
        // La foto NON è obbligatoria: si può documentare una medicazione senza immagine
        // (l'utente è già avvisato che è raccomandata). Se manca, non si crea alcuna riga foto.
        // Transazione unica: evento + foto + audit committano insieme. Senza, un errore
        // dopo il primo insert lascerebbe l'evento nel DB con la UI in Errore, e il
        // retry dell'utente creerebbe un duplicato.
        return db.withTransaction {
        val id = dressingRepository.insert(event)
        // dressing_photos contiene TUTTE le foto: position 0 = principale (copertina),
        // position 1..n = angolazioni aggiuntive. Vuoto se la medicazione è senza foto.
        val photos = buildList {
            if (event.photoUri.isNotBlank()) {
                add(DressingPhotoEntity(eventId = id, photoUri = event.photoUri, position = 0))
                extraPhotoUris.filter { it.isNotBlank() }.forEachIndexed { i, uri ->
                    add(DressingPhotoEntity(eventId = id, photoUri = uri, position = i + 1))
                }
            }
        }
        if (photos.isNotEmpty()) dressingPhotoDao.insertAll(photos)
        auditRepository.log(
            action = "CREATE_DRESSING",
            entityType = "DressingEvent",
            entityId = id,
            details = "Assessment=${event.assessmentId}, photo=${event.photoUri}, extraPhotos=${(photos.size - 1).coerceAtLeast(0)}"
        )
        id
        }
    }
}
