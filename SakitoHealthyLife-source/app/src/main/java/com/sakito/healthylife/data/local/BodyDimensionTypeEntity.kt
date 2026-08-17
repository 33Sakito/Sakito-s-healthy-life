package com.sakito.healthylife.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "body_dimension_types",
    indices = [Index(value = ["name"], unique = true)]
)
data class BodyDimensionTypeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isCustom: Boolean = true,
    val sortOrder: Int = 0
)
