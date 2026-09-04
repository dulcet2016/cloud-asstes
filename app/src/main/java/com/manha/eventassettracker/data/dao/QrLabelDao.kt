package com.manha.eventassettracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.manha.eventassettracker.data.entity.QrLabelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QrLabelDao {
    @Query("SELECT * FROM qr_labels ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<QrLabelEntity>>

    @Query("SELECT * FROM qr_labels WHERE name = :name AND category = :category ORDER BY assetId ASC")
    suspend fun findByGroup(name: String, category: String): List<QrLabelEntity>

    @Query("SELECT MAX(CAST(SUBSTR(assetId, :prefixLength + 1) AS INTEGER)) FROM qr_labels WHERE assetId LIKE :prefix || '%'")
    suspend fun maxNumberForPrefix(prefix: String, prefixLength: Int): Int?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(labels: List<QrLabelEntity>)

    @Delete
    suspend fun delete(label: QrLabelEntity)

    @Query("DELETE FROM qr_labels WHERE name = :name AND category = :category AND assetId NOT IN (SELECT assetId FROM assets)")
    suspend fun deleteUnassignedInGroup(name: String, category: String)
}
