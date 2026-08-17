package com.sakito.healthylife.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "food_units",
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
data class FoodUnitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val foodId: Long,
    val name: String,
    val grams: Double,
    val isBase: Boolean = false,
    val sortOrder: Int = 0
)
