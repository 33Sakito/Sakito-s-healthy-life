package com.sakito.healthylife.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_ranges")
data class SavedRangeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val startDate: String,
    val endDate: String,
    val createdAt: Long = System.currentTimeMillis()
)
