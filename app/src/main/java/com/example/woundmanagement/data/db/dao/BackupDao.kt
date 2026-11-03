package com.example.woundmanagement.data.db.dao

import androidx.room.*
import com.example.woundmanagement.data.db.entity.*

@Dao
interface BackupDao {
    // ── Export queries ──
    @Query("SELECT * FROM patients")
    suspend fun getAllPatients(): List<PatientEntity>

    @Query("SELECT * FROM wound_assessments")
    suspend fun getAllWounds(): List<WoundAssessmentEntity>

    @Query("SELECT * FROM dressing_events")
    suspend fun getAllDressings(): List<DressingEventEntity>

    @Query("SELECT * FROM dressing_photos")
    suspend fun getAllDressingPhotos(): List<DressingPhotoEntity>

    @Query("SELECT * FROM norton_assessments")
    suspend fun getAllNorton(): List<NortonAssessmentEntity>

    @Query("SELECT * FROM nrs2002_assessments")
    suspend fun getAllNRS2002(): List<NRS2002AssessmentEntity>

    @Query("SELECT * FROM pain_nrs_assessments")
    suspend fun getAllPainNrs(): List<PainNRSAssessmentEntity>

    @Query("SELECT * FROM audit_log")
    suspend fun getAllAudit(): List<AuditLogEntity>

    // ── Delete all (child tables first) ──
    @Query("DELETE FROM dressing_photos")
    suspend fun deleteAllDressingPhotos()

    @Query("DELETE FROM dressing_events")
    suspend fun deleteAllDressings()

    @Query("DELETE FROM wound_assessments")
    suspend fun deleteAllWounds()

    @Query("DELETE FROM norton_assessments")
    suspend fun deleteAllNorton()

    @Query("DELETE FROM nrs2002_assessments")
    suspend fun deleteAllNRS2002()

    @Query("DELETE FROM pain_nrs_assessments")
    suspend fun deleteAllPainNrs()

    @Query("DELETE FROM audit_log")
    suspend fun deleteAllAudit()

    @Query("DELETE FROM patients")
    suspend fun deleteAllPatients()

    // ── Insert with explicit IDs ──
    @Insert
    suspend fun insertPatients(patients: List<PatientEntity>)

    @Insert
    suspend fun insertWounds(wounds: List<WoundAssessmentEntity>)

    @Insert
    suspend fun insertDressings(dressings: List<DressingEventEntity>)

    @Insert
    suspend fun insertNorton(assessments: List<NortonAssessmentEntity>)

    @Insert
    suspend fun insertNRS2002(assessments: List<NRS2002AssessmentEntity>)

    @Insert
    suspend fun insertPainNrs(assessments: List<PainNRSAssessmentEntity>)

    @Insert
    suspend fun insertAudit(logs: List<AuditLogEntity>)

    // ── Insert singoli che ritornano il nuovo ID (per l'import-merge con rimappamento) ──
    @Insert suspend fun insertPatient(patient: PatientEntity): Long
    @Insert suspend fun insertWound(wound: WoundAssessmentEntity): Long
    @Insert suspend fun insertDressing(dressing: DressingEventEntity): Long
    @Insert suspend fun insertDressingPhoto(photo: DressingPhotoEntity): Long
    @Insert suspend fun insertNorton(assessment: NortonAssessmentEntity): Long
    @Insert suspend fun insertNRS2002(assessment: NRS2002AssessmentEntity): Long
    @Insert suspend fun insertPainNrs(assessment: PainNRSAssessmentEntity): Long
    @Insert suspend fun insertAudit(log: AuditLogEntity): Long
}
