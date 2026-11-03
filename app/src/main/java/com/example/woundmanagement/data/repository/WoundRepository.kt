package com.example.woundmanagement.data.repository

import com.example.woundmanagement.data.db.dao.WoundAssessmentDao
import com.example.woundmanagement.data.db.entity.WoundAssessmentEntity
import kotlinx.coroutines.flow.Flow

class WoundRepository(private val dao: WoundAssessmentDao) {
    fun getByPatient(patientId: Long): Flow<List<WoundAssessmentEntity>> = dao.getByPatient(patientId)
    suspend fun getById(id: Long): WoundAssessmentEntity? = dao.getById(id)
    suspend fun insert(assessment: WoundAssessmentEntity): Long = dao.insert(assessment)
    suspend fun update(assessment: WoundAssessmentEntity) = dao.update(assessment)
    suspend fun delete(assessment: WoundAssessmentEntity) = dao.delete(assessment)
    suspend fun getCountByLocation(patientId: Long, location: String): Int =
        dao.getCountByLocation(patientId, location)
}
