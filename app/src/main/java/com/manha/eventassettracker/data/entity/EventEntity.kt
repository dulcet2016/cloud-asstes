package com.manha.eventassettracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val name: String,
    val eventDate: String,
    val venue: String = "",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
