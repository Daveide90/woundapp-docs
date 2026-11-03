package com.example.woundmanagement.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "nrs2002_assessments",
    foreignKeys = [ForeignKey(
        entity = PatientEntity::class,
        parentColumns = ["id"],
        childColumns = ["patientId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("patientId")]
)
data class NRS2002AssessmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val nutritionalScore: Int,          // 0-3 (stato nutrizionale compromesso)
    val diseaseSeverityScore: Int,      // 0-3 (gravità di malattia)
    val ageAdjustment: Int,             // 0 o 1 (+1 se età ≥70)
    val totalScore: Int,                // 0-7
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
