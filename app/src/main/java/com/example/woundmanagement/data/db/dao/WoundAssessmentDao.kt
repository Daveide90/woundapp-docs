package com.example.woundmanagement.data.db.dao

import androidx.room.*
import com.example.woundmanagement.data.db.entity.WoundAssessmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WoundAssessmentDao {
    @Query("SELECT * FROM wound_assessments WHERE patientId = :patientId ORDER BY createdAt DESC")
    fun getByPatient(patientId: Long): Flow<List<WoundAssessmentEntity>>

    @Query("SELECT * FROM wound_assessments WHERE id = :id")
    suspend fun getById(id: Long): WoundAssessmentEntity?

    @Query("SELECT COUNT(*) FROM wound_assessments WHERE patientId = :patientId AND location = :location")
    suspend fun getCountByLocation(patientId: Long, location: String): Int

    @Insert
    suspend fun insert(assessment: WoundAssessmentEntity): Long

    @Update
    suspend fun update(assessment: WoundAssessmentEntity)

    @Delete
    suspend fun delete(assessment: WoundAssessmentEntity)
}
