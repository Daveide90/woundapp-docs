package com.example.woundmanagement.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Foto della medicazione. La copertina resta DressingEventEntity.photoUri
 * (= position 0 qui); questa tabella contiene TUTTE le foto, copertina inclusa.
 */
@Entity(
    tableName = "dressing_photos",
    foreignKeys = [ForeignKey(
        entity = DressingEventEntity::class,
        parentColumns = ["id"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("eventId")]
)
data class DressingPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    val photoUri: String,
    // 0 = foto principale (copertina), 1..n = foto aggiuntive
    val position: Int = 0
)
