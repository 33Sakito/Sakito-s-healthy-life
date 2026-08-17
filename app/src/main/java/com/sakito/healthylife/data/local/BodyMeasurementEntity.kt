package com.sakito.healthylife.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "body_measurements",
    foreignKeys = [
        ForeignKey(
            entity = BodyDimensionTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["dimensionTypeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("date"), Index("dimensionTypeId")]
)
data class BodyMeasurementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val dimensionTypeId: Long,
    val valueCm: Double,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
