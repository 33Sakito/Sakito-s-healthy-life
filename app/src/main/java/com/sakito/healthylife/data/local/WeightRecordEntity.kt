package com.sakito.healthylife.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weight_records",
    indices = [Index("date")]
)
data class WeightRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val weightKg: Double,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
