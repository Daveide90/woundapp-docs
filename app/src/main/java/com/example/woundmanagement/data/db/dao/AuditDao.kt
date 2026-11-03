package com.example.woundmanagement.data.db.dao

import androidx.room.*
import com.example.woundmanagement.data.db.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditDao {
    @Query("SELECT * FROM audit_log ORDER BY timestamp DESC")
    fun getAll(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_log WHERE entityType = :entityType ORDER BY timestamp DESC")
    fun getByEntityType(entityType: String): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_log WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp DESC")
    fun getByDateRange(from: Long, to: Long): Flow<List<AuditLogEntity>>

    @Insert
    suspend fun insert(log: AuditLogEntity): Long
}
