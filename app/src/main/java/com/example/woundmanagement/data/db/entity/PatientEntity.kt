package com.example.woundmanagement.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val surname: String,
    val fiscalCode: String = "",
    val birthDate: String = "",
    val ward: String = "",
    val bed: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
