package com.sakito.healthylife.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "foods",
    indices = [Index(value = ["name"], unique = true)]
)
data class FoodEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val animalProteinPer100g: Double? = null,
    val plantProteinPer100g: Double? = null,
    val fatPer100g: Double,
    val carbPer100g: Double,
    val fiberPer100g: Double? = null,
    val note: String? = null,
    val isCustom: Boolean = true,
    val isCombined: Boolean = false,
    val usageCount: Int = 0,
    val lastUsedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
