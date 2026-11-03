package com.example.woundmanagement.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_log")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String,
    val entityType: String,
    val entityId: Long = 0,
    val details: String = ""
)
