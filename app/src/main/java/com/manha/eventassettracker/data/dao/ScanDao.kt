package com.manha.eventassettracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.manha.eventassettracker.data.entity.ScanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query("SELECT * FROM scans ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE eventId = :eventId ORDER BY timestamp ASC")
    fun observeByEvent(eventId: String): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE staffId = :staffId")
    suspend fun findByStaff(staffId: String): List<ScanEntity>

    @Query("SELECT COUNT(*) FROM scans WHERE staffId = :staffId")
    suspend fun countByStaff(staffId: String): Int

    @Insert
    suspend fun insert(scan: ScanEntity)

    @Update
    suspend fun update(scan: ScanEntity)

    @Delete
    suspend fun delete(scan: ScanEntity)

    @Query("SELECT * FROM scans WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): ScanEntity?
}
