package com.example.woundmanagement.data.db.dao

import androidx.room.*
import com.example.woundmanagement.data.db.entity.PatientEntity
import kotlinx.coroutines.flow.Flow

data class PatientWithRisk(
    val id: Long,
    val name: String,
    val surname: String,
    val fiscalCode: String,
    val birthDate: String,
    val ward: String,
    val bed: String,
    val notes: String,
    val createdAt: Long
)

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients ORDER BY surname ASC, name ASC")
    fun getAll(): Flow<List<PatientEntity>>

    @Query("""
        SELECT p.id, p.name, p.surname, p.fiscalCode, p.birthDate, p.ward, p.bed, p.notes, p.createdAt
        FROM patients p
        ORDER BY p.surname ASC, p.name ASC
    """)
    fun getAllWithRisk(): Flow<List<PatientWithRisk>>

    @Query("""
        SELECT p.id, p.name, p.surname, p.fiscalCode, p.birthDate, p.ward, p.bed, p.notes, p.createdAt
        FROM patients p
        WHERE p.name LIKE '%' || :query || '%' OR p.surname LIKE '%' || :query || '%' OR p.fiscalCode LIKE '%' || :query || '%'
        ORDER BY p.surname ASC, p.name ASC
    """)
    fun searchWithRisk(query: String): Flow<List<PatientWithRisk>>

    @Query("SELECT * FROM patients WHERE id = :id")
    suspend fun getById(id: Long): PatientEntity?

    @Query("SELECT * FROM patients WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<PatientEntity?>

    @Insert
    suspend fun insert(patient: PatientEntity): Long

    @Update
    suspend fun update(patient: PatientEntity)

    @Delete
    suspend fun delete(patient: PatientEntity)

    @Query("SELECT * FROM patients WHERE name LIKE '%' || :query || '%' OR surname LIKE '%' || :query || '%' OR fiscalCode LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<PatientEntity>>
}
