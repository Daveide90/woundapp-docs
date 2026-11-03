package com.example.woundmanagement.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "norton_assessments",
    foreignKeys = [ForeignKey(
        entity = PatientEntity::class,
        parentColumns = ["id"],
        childColumns = ["patientId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("patientId")]
)
data class NortonAssessmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val condizioneFisica: Int,      // 1-4
    val statoMentale: Int,          // 1-4
    val attivita: Int,              // 1-4
    val mobilita: Int,              // 1-4
    val incontinenza: Int,          // 1-4
    val totalScore: Int,            // 5-20 (più basso = più rischio)
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
