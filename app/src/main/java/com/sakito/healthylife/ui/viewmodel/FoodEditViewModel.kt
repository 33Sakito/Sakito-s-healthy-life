package com.sakito.healthylife.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sakito.healthylife.HealthyLifeApp
import com.sakito.healthylife.data.local.FoodEntity
import com.sakito.healthylife.data.local.FoodUnitEntity
import com.sakito.healthylife.data.local.MicronutrientEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UnitDraft(
    val name: String = "",
    val grams: String = "1",
    val isBase: Boolean = false
)

data class MicroDraft(
    val name: String = "",
    val amount: String = "0",
    val unit: String = "mg"
)

data class FoodEditUiState(
    val foodId: Long = 0,
    val name: String = "",
    val calories: String = "",
    val protein: String = "",
    val animalProtein: String = "",
    val plantProtein: String = "",
    val fat: String = "",
    val carb: String = "",
    val fiber: String = "",
    val note: String = "",
    val isCombined: Boolean = false,
    val units: List<UnitDraft> = listOf(UnitDraft(name = "克", grams = "1", isBase = true)),
    val micros: List<MicroDraft> = emptyList(),
    val saving: Boolean = false,
    val error: String? = null
)

class FoodEditViewModel(app: HealthyLifeApp, foodId: Long) : ViewModel() {

    private val foodRepository = app.foodRepository

    private val _uiState = MutableStateFlow(FoodEditUiState(foodId = foodId))
    val uiState: StateFlow<FoodEditUiState> = _uiState.asStateFlow()

    init {
        if (foodId != 0L) {
            viewModelScope.launch {
                val food = foodRepository.getFood(foodId) ?: return@launch
                _uiState.value = FoodEditUiState(
                    foodId = food.food.id,
                    name = food.food.name,
                    calories = food.food.caloriesPer100g.toPlainString(),
                    protein = food.food.proteinPer100g.toPlainString(),
                    animalProtein = food.food.animalProteinPer100g?.toPlainString().orEmpty(),
                    plantProtein = food.food.plantProteinPer100g?.toPlainString().orEmpty(),
                    fat = food.food.fatPer100g.toPlainString(),
                    carb = food.food.carbPer100g.toPlainString(),
                    fiber = food.food.fiberPer100g?.toPlainString().orEmpty(),
                    note = food.food.note.orEmpty(),
                    isCombined = food.food.isCombined,
                    units = food.units.ifEmpty {
                        listOf(FoodUnitEntity(foodId = food.food.id, name = "克", grams = 1.0, isBase = true))
                    }.map { UnitDraft(it.name, it.grams.toPlainString(), it.isBase) },
                    micros = food.micronutrients.map { MicroDraft(it.name, it.amountPer100g.toPlainString(), it.unit) }
                )
            }
        }
    }

    fun updateName(value: String) = _uiState.update { it.copy(name = value) }
    fun updateCalories(value: String) = _uiState.update { it.copy(calories = value) }
    fun updateProtein(value: String) = _uiState.update { it.copy(protein = value) }
    fun updateAnimalProtein(value: String) = _uiState.update {
        val next = it.copy(animalProtein = value)
        autoFillProtein(next)
    }
    fun updatePlantProtein(value: String) = _uiState.update {
        val next = it.copy(plantProtein = value)
        autoFillProtein(next)
    }
    fun updateFat(value: String) = _uiState.update { it.copy(fat = value) }
    fun updateCarb(value: String) = _uiState.update { it.copy(carb = value) }
    fun updateFiber(value: String) = _uiState.update { it.copy(fiber = value) }
    fun updateNote(value: String) = _uiState.update { it.copy(note = value) }

    fun addUnit() = _uiState.update { it.copy(units = it.units + UnitDraft()) }
    fun updateUnit(index: Int, unit: UnitDraft) = _uiState.update {
        it.copy(units = it.units.mapIndexed { i, u -> if (i == index) unit else u })
    }
    fun removeUnit(index: Int) = _uiState.update {
        val units = it.units.toMutableList()
        if (index in units.indices && !units[index].isBase) {
            units.removeAt(index)
        }
        it.copy(units = units)
    }

    fun addMicro() = _uiState.update { it.copy(micros = it.micros + MicroDraft()) }
    fun updateMicro(index: Int, micro: MicroDraft) = _uiState.update {
        it.copy(micros = it.micros.mapIndexed { i, m -> if (i == index) micro else m })
    }
    fun removeMicro(index: Int) = _uiState.update {
        it.copy(micros = it.micros.filterIndexed { i, _ -> i != index })
    }

    fun save(onSaved: (Long) -> Unit) {
        val s = _uiState.value
        val name = s.name.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(error = "请输入食物名称") }
            return
        }
        val calories = s.calories.toDoubleOrNull()
        val protein = s.protein.toDoubleOrNull()
        val fat = s.fat.toDoubleOrNull()
        val carb = s.carb.toDoubleOrNull()
        if (calories == null || protein == null || fat == null || carb == null) {
            _uiState.update { it.copy(error = "热量、总蛋白、脂肪、碳水必须为数字") }
            return
        }
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val units = s.units.mapIndexed { index, u ->
                val grams = u.grams.toDoubleOrNull() ?: 1.0
                FoodUnitEntity(
                    foodId = s.foodId,
                    name = if (u.isBase) "克" else u.name.trim().ifEmpty { "单位${index + 1}" },
                    grams = grams,
                    isBase = u.isBase,
                    sortOrder = index
                )
            }.ifEmpty { listOf(FoodUnitEntity(foodId = s.foodId, name = "克", grams = 1.0, isBase = true)) }
            val micros = s.micros.filter { it.name.isNotBlank() }.map {
                MicronutrientEntity(
                    foodId = s.foodId,
                    name = it.name.trim(),
                    amountPer100g = it.amount.toDoubleOrNull() ?: 0.0,
                    unit = it.unit.trim().ifEmpty { "mg" }
                )
            }
            val food = FoodEntity(
                id = s.foodId,
                name = name,
                caloriesPer100g = calories,
                proteinPer100g = protein,
                animalProteinPer100g = s.animalProtein.toDoubleOrNull(),
                plantProteinPer100g = s.plantProtein.toDoubleOrNull(),
                fatPer100g = fat,
                carbPer100g = carb,
                fiberPer100g = s.fiber.toDoubleOrNull(),
                note = s.note.trim().ifEmpty { null },
                isCustom = true,
                isCombined = s.isCombined
            )
            val id = foodRepository.saveFood(food, units, micros)
            _uiState.update { it.copy(saving = false) }
            onSaved(id)
        }
    }

    private fun autoFillProtein(state: FoodEditUiState): FoodEditUiState {
        val animal = state.animalProtein.toDoubleOrNull()
        val plant = state.plantProtein.toDoubleOrNull()
        return if (animal != null && plant != null) {
            state.copy(protein = (animal + plant).toPlainString())
        } else {
            state
        }
    }

    private fun Double.toPlainString(): String {
        return if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()
    }

    companion object {
        fun factory(app: HealthyLifeApp, foodId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer { FoodEditViewModel(app, foodId) }
        }
    }
}
