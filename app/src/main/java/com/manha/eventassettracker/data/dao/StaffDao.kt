package com.manha.eventassettracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.manha.eventassettracker.data.entity.StaffEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff ORDER BY name ASC")
    fun observeAll(): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff WHERE mobile = :mobile LIMIT 1")
    suspend fun findByMobile(mobile: String): StaffEntity?

    @Query("SELECT * FROM staff WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): StaffEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(staff: StaffEntity)

    @Update
    suspend fun update(staff: StaffEntity)

    @Delete
    suspend fun delete(staff: StaffEntity)
}
