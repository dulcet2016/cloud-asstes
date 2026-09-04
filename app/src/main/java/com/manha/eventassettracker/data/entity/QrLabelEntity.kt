package com.manha.eventassettracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "qr_labels")
data class QrLabelEntity(
    @PrimaryKey val assetId: String,
    val name: String,
    val category: String,
    val sizeCm: Int,
    val createdAt: Long = System.currentTimeMillis()
)
