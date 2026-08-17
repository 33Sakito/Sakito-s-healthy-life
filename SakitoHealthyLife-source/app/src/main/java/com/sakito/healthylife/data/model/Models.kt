package com.sakito.healthylife.data.model

import com.sakito.healthylife.data.local.FoodEntity
import com.sakito.healthylife.data.local.FoodUnitEntity

enum class FoodFilter(val label: String) {
    ALL("全部"),
    COMMON("常用"),
    RECENT("最近使用"),
    CUSTOM("自定义")
}

data class CombinedComponentInput(
    val ingredientFoodId: Long,
    val unitId: Long?,
    val unitName: String,
    val unitGrams: Double,
    val quantity: Double
)

data class NutrientSummary(
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val animalProtein: Double? = null,
    val plantProtein: Double? = null,
    val fat: Double = 0.0,
    val carb: Double = 0.0,
    val fiber: Double? = null
) {
    fun plus(other: NutrientSummary): NutrientSummary {
        return NutrientSummary(
            calories = calories + other.calories,
            protein = protein + other.protein,
            animalProtein = (animalProtein ?: 0.0) + (other.animalProtein ?: 0.0),
            plantProtein = (plantProtein ?: 0.0) + (other.plantProtein ?: 0.0),
            fat = fat + other.fat,
            carb = carb + other.carb,
            fiber = (fiber ?: 0.0) + (other.fiber ?: 0.0)
        )
    }
}

fun FoodEntity.toSummary(): NutrientSummary {
    return NutrientSummary(
        calories = caloriesPer100g,
        protein = proteinPer100g,
        animalProtein = animalProteinPer100g,
        plantProtein = plantProteinPer100g,
        fat = fatPer100g,
        carb = carbPer100g,
        fiber = fiberPer100g
    )
}

fun FoodUnitEntity.displayName(): String {
    return if (isBase) "克" else name
}
