package com.sakito.healthylife.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "micronutrients",
    foreignKeys = [
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("foodId")]
)
data class MicronutrientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val foodId: Long,
    val name: String,
    val amountPer100g: Double,
    val unit: String
)
