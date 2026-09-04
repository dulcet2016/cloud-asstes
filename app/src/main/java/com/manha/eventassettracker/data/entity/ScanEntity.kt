package com.manha.eventassettracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val assetId: String,
    val assetName: String,
    val category: String,
    val eventId: String,
    val eventName: String,
    val type: String, // ScanType.OUT or ScanType.RETURN
    val staffId: String,
    val staffName: String,
    val timestamp: Long = System.currentTimeMillis()
)
