package com.example.woundmanagement.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pain_nrs_assessments",
    foreignKeys = [ForeignKey(
        entity = PatientEntity::class,
        parentColumns = ["id"],
        childColumns = ["patientId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("patientId")]
)
data class PainNRSAssessmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val painScore: Int,             // 0-10
    val painLocation: String,       // Localizzazione del dolore
    val painType: String,           // CONTINUO, INTERMITTENTE, PROCEDURALE
    val painCharacter: String,      // PUNGENTE, BRUCIANTE, PULSANTE, SORDO
    val aggravatingFactors: String, // Fattori aggravanti
    val relievingFactors: String,   // Fattori allevianti
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
