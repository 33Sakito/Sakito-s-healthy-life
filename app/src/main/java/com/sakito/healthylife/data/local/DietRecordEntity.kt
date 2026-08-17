package com.sakito.healthylife.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "diet_records",
    indices = [Index("date")]
)
data class DietRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val createdAt: Long = System.currentTimeMillis(),
    val mealType: String = "未分餐",
    val note: String? = null
)
