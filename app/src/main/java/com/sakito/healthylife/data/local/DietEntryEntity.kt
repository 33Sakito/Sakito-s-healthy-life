package com.sakito.healthylife.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "diet_entries",
    foreignKeys = [
        ForeignKey(
            entity = DietRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("recordId"), Index("foodId")]
)
data class DietEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recordId: Long,
    val foodId: Long?,
    val foodName: String,
    val unitName: String,
    val unitGrams: Double,
    val quantity: Double,
    val actualWeight: Double,
    val calories: Double,
    val protein: Double,
    val animalProtein: Double? = null,
    val plantProtein: Double? = null,
    val fat: Double,
    val carb: Double,
    val fiber: Double? = null,
    val note: String? = null
)
