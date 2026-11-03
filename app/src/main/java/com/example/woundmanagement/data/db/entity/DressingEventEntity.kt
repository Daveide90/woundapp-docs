package com.example.woundmanagement.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dressing_events",
    foreignKeys = [ForeignKey(
        entity = WoundAssessmentEntity::class,
        parentColumns = ["id"],
        childColumns = ["assessmentId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("assessmentId")]
)
data class DressingEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val assessmentId: Long,
    val photoUri: String,
    val roiData: String = "",
    val tissueBlackPercent: Float = 0f,
    val tissueYellowPercent: Float = 0f,
    val tissueRedPercent: Float = 0f,
    val tissuePinkPercent: Float = 0f,
    val exudateLevel: String = "",
    val exudateType: String = "",
    // null = dolore non rilevato (distinto da 0 = "nessun dolore")
    val painNrs: Int? = null,
    // Legacy: testo composto localizzato dei record pre-v11. I nuovi record usano i due enum name.
    val dressingType: String = "",
    val primaryDressing: String = "",
    val secondaryDressing: String = "",
    val notes: String = "",
    // Misure della lesione a QUESTA medicazione (per seguire l'evoluzione nel tempo).
    // 0 = non misurata. Inserimento manuale (documentazione), depthCm sempre in cm.
    val lengthCm: Float = 0f,
    val widthCm: Float = 0f,
    val depthCm: Float = 0f,
    val createdAt: Long = System.currentTimeMillis()
)
