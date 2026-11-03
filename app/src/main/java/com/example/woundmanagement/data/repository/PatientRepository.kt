package com.example.woundmanagement.data.repository

import com.example.woundmanagement.data.db.dao.PatientDao
import com.example.woundmanagement.data.db.dao.PatientWithRisk
import com.example.woundmanagement.data.db.entity.PatientEntity
import kotlinx.coroutines.flow.Flow

class PatientRepository(private val dao: PatientDao) {
    fun getAll(): Flow<List<PatientEntity>> = dao.getAll()
    fun getAllWithRisk(): Flow<List<PatientWithRisk>> = dao.getAllWithRisk()
    fun search(query: String): Flow<List<PatientEntity>> = dao.search(query)
    fun searchWithRisk(query: String): Flow<List<PatientWithRisk>> = dao.searchWithRisk(query)
    suspend fun getById(id: Long): PatientEntity? = dao.getById(id)
    fun getByIdFlow(id: Long): Flow<PatientEntity?> = dao.getByIdFlow(id)
    suspend fun insert(patient: PatientEntity): Long = dao.insert(patient)
    suspend fun update(patient: PatientEntity) = dao.update(patient)
    suspend fun delete(patient: PatientEntity) = dao.delete(patient)
}
