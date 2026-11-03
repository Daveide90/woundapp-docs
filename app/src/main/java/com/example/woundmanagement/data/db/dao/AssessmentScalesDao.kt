package com.example.woundmanagement.data.db.dao

import androidx.room.*
import com.example.woundmanagement.data.db.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NortonDao {
    @Query("SELECT * FROM norton_assessments WHERE patientId = :patientId ORDER BY createdAt DESC")
    fun getByPatient(patientId: Long): Flow<List<NortonAssessmentEntity>>

    @Query("SELECT * FROM norton_assessments WHERE patientId = :patientId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestByPatient(patientId: Long): NortonAssessmentEntity?

    @Insert
    suspend fun insert(assessment: NortonAssessmentEntity): Long

    @Update
    suspend fun update(assessment: NortonAssessmentEntity)

    @Delete
    suspend fun delete(assessment: NortonAssessmentEntity)
}

@Dao
interface NRS2002Dao {
    @Query("SELECT * FROM nrs2002_assessments WHERE patientId = :patientId ORDER BY createdAt DESC")
    fun getByPatient(patientId: Long): Flow<List<NRS2002AssessmentEntity>>

    @Query("SELECT * FROM nrs2002_assessments WHERE patientId = :patientId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestByPatient(patientId: Long): NRS2002AssessmentEntity?

    @Insert
    suspend fun insert(assessment: NRS2002AssessmentEntity): Long

    @Update
    suspend fun update(assessment: NRS2002AssessmentEntity)

    @Delete
    suspend fun delete(assessment: NRS2002AssessmentEntity)
}

@Dao
interface PainNRSDao {
    @Query("SELECT * FROM pain_nrs_assessments WHERE patientId = :patientId ORDER BY createdAt DESC")
    fun getByPatient(patientId: Long): Flow<List<PainNRSAssessmentEntity>>

    @Query("SELECT * FROM pain_nrs_assessments WHERE patientId = :patientId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestByPatient(patientId: Long): PainNRSAssessmentEntity?

    @Insert
    suspend fun insert(assessment: PainNRSAssessmentEntity): Long

    @Update
    suspend fun update(assessment: PainNRSAssessmentEntity)

    @Delete
    suspend fun delete(assessment: PainNRSAssessmentEntity)
}
