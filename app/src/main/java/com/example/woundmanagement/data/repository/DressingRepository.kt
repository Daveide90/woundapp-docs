package com.example.woundmanagement.data.repository

import com.example.woundmanagement.data.db.dao.DressingEventDao
import com.example.woundmanagement.data.db.dao.DressingEventWithWound
import com.example.woundmanagement.data.db.entity.*
import kotlinx.coroutines.flow.Flow

class DressingRepository(
    private val dao: DressingEventDao
) {
    fun getAll(): Flow<List<DressingEventEntity>> = dao.getAll()
    fun getByAssessment(assessmentId: Long): Flow<List<DressingEventEntity>> = dao.getByAssessment(assessmentId)
    fun getByPatient(patientId: Long): Flow<List<DressingEventWithWound>> = dao.getByPatient(patientId)
    suspend fun getById(id: Long): DressingEventEntity? = dao.getById(id)
    suspend fun insert(event: DressingEventEntity): Long = dao.insert(event)
    suspend fun update(event: DressingEventEntity) = dao.update(event)
}
