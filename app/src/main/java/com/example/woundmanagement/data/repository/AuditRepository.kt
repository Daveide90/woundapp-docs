package com.example.woundmanagement.data.repository

import com.example.woundmanagement.data.db.dao.AuditDao
import com.example.woundmanagement.data.db.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

class AuditRepository(private val dao: AuditDao) {
    fun getAll(): Flow<List<AuditLogEntity>> = dao.getAll()
    fun getByEntityType(entityType: String): Flow<List<AuditLogEntity>> = dao.getByEntityType(entityType)
    fun getByDateRange(from: Long, to: Long): Flow<List<AuditLogEntity>> = dao.getByDateRange(from, to)

    suspend fun log(action: String, entityType: String, entityId: Long = 0, details: String = "") {
        dao.insert(
            AuditLogEntity(
                action = action,
                entityType = entityType,
                entityId = entityId,
                details = details
            )
        )
    }
}
