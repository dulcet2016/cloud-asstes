package com.manha.eventassettracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "staff")
data class StaffEntity(
    @PrimaryKey val id: String,
    val name: String,
    val mobile: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
