package com.example.woundmanagement.data.db.dao

import androidx.room.*
import com.example.woundmanagement.data.db.entity.DressingPhotoEntity

@Dao
interface DressingPhotoDao {
    @Query("SELECT * FROM dressing_photos WHERE eventId = :eventId ORDER BY position")
    suspend fun getByEventOnce(eventId: Long): List<DressingPhotoEntity>

    @Query("SELECT * FROM dressing_photos WHERE eventId IN (:eventIds)")
    suspend fun getByEvents(eventIds: List<Long>): List<DressingPhotoEntity>

    @Insert
    suspend fun insert(photo: DressingPhotoEntity): Long

    @Insert
    suspend fun insertAll(photos: List<DressingPhotoEntity>)

    /** Conta le righe che referenziano un file foto (per la pulizia sicura dei file). */
    @Query("SELECT COUNT(*) FROM dressing_photos WHERE photoUri = :name")
    suspend fun countByPhotoUri(name: String): Int
}
