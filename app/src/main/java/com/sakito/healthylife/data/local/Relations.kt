package com.sakito.healthylife.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class FoodWithDetails(
    @Embedded val food: FoodEntity,
    @Relation(parentColumn = "id", entityColumn = "foodId")
    val units: List<FoodUnitEntity> = emptyList(),
    @Relation(parentColumn = "id", entityColumn = "foodId")
    val micronutrients: List<MicronutrientEntity> = emptyList(),
    @Relation(parentColumn = "id", entityColumn = "parentFoodId")
    val components: List<CombinedComponentEntity> = emptyList()
)

data class DietRecordWithEntries(
    @Embedded val record: DietRecordEntity,
    @Relation(parentColumn = "id", entityColumn = "recordId")
    val entries: List<DietEntryEntity> = emptyList()
)
