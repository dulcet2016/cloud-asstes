package com.manha.eventassettracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.manha.eventassettracker.data.entity.AdminEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AdminDao {
    @Query("SELECT * FROM admins ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<AdminEntity>>

    @Query("SELECT * FROM admins WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): AdminEntity?

    @Query("SELECT * FROM admins WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): AdminEntity?

    @Query("SELECT COUNT(*) FROM admins")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(admin: AdminEntity)

    @Update
    suspend fun update(admin: AdminEntity)

    @Delete
    suspend fun delete(admin: AdminEntity)
}
