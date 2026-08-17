package com.sakito.healthylife.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "combined_components",
    foreignKeys = [
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentFoodId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["ingredientFoodId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("parentFoodId"), Index("ingredientFoodId")]
)
data class CombinedComponentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val parentFoodId: Long,
    val ingredientFoodId: Long,
    val unitId: Long? = null,
    val unitName: String,
    val unitGrams: Double,
    val quantity: Double
)
