package com.example.woundmanagement.data.db.dao

import androidx.room.*
import com.example.woundmanagement.data.db.entity.DressingEventEntity
import kotlinx.coroutines.flow.Flow

data class DressingEventWithWound(
    val eventId: Long,
    val assessmentId: Long,
    val photoUri: String,
    val tissueBlackPercent: Float,
    val tissueYellowPercent: Float,
    val tissueRedPercent: Float,
    val tissuePinkPercent: Float,
    val exudateLevel: String,
    val exudateType: String,
    val painNrs: Int?,
    val dressingType: String,
    val primaryDressing: String,
    val secondaryDressing: String,
    val notes: String,
    val lengthCm: Float,
    val widthCm: Float,
    val depthCm: Float,
    val eventCreatedAt: Long,
    val woundLocation: String,
    val woundName: String,
    val woundType: String
)

@Dao
interface DressingEventDao {
    @Query("SELECT * FROM dressing_events ORDER BY createdAt DESC")
    fun getAll(): Flow<List<DressingEventEntity>>

    @Query("SELECT * FROM dressing_events WHERE assessmentId = :assessmentId ORDER BY createdAt DESC")
    fun getByAssessment(assessmentId: Long): Flow<List<DressingEventEntity>>

    @Query("""
        SELECT
            de.id as eventId,
            de.assessmentId,
            de.photoUri,
            de.tissueBlackPercent,
            de.tissueYellowPercent,
            de.tissueRedPercent,
            de.tissuePinkPercent,
            de.exudateLevel,
            de.exudateType,
            de.painNrs,
            de.dressingType,
            de.primaryDressing,
            de.secondaryDressing,
            de.notes,
            de.lengthCm,
            de.widthCm,
            de.depthCm,
            de.createdAt as eventCreatedAt,
            wa.location as woundLocation,
            wa.name as woundName,
            wa.type as woundType
        FROM dressing_events de
        INNER JOIN wound_assessments wa ON de.assessmentId = wa.id
        WHERE wa.patientId = :patientId
        ORDER BY de.createdAt DESC
    """)
    fun getByPatient(patientId: Long): Flow<List<DressingEventWithWound>>

    @Query("SELECT * FROM dressing_events WHERE id = :id")
    suspend fun getById(id: Long): DressingEventEntity?

    @Query("SELECT * FROM dressing_events WHERE assessmentId = :assessmentId ORDER BY createdAt DESC")
    suspend fun getByAssessmentOnce(assessmentId: Long): List<DressingEventEntity>

    /** Conta i record che referenziano un file foto (per la pulizia sicura dei file). */
    @Query("SELECT COUNT(*) FROM dressing_events WHERE photoUri = :name")
    suspend fun countByPhotoUri(name: String): Int

    @Insert
    suspend fun insert(event: DressingEventEntity): Long

    @Update
    suspend fun update(event: DressingEventEntity)

    @Delete
    suspend fun delete(event: DressingEventEntity)
}
