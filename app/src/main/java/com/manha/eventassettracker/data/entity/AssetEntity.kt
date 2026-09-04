package com.manha.eventassettracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Status of an asset's most recent movement. */
object ScanType {
    const val OUT = "OUT"
    const val RETURN = "RETURN"
}

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey val assetId: String,
    val name: String,
    val category: String,
    val currentEventId: String? = null,
    val currentEventName: String? = null,
    val status: String = ScanType.RETURN, // OUT = currently out at an event, RETURN = back in storage
    val lastStaffId: String? = null,
    val lastStaffName: String? = null,
    val lastScanAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
