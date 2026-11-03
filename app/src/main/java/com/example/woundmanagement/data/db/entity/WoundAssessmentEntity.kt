package com.example.woundmanagement.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "wound_assessments",
    foreignKeys = [ForeignKey(
        entity = PatientEntity::class,
        parentColumns = ["id"],
        childColumns = ["patientId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("patientId")]
)
data class WoundAssessmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val location: String,
    val name: String = "",          // Nome ferita (es. "Sacro 1") — auto-generato se vuoto
    val type: String,
    val origin: String = "",
    val lengthCm: Float = 0f,
    val widthCm: Float = 0f,
    val depthCm: Float = 0f,
    val areaCm2: Float = 0f,
    // Anamnesi / presa in carico della lesione (testo libero dell'operatore)
    val notes: String = "",
    // Descrizione libera del tipo quando type == "ALTRO"
    val typeOther: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
