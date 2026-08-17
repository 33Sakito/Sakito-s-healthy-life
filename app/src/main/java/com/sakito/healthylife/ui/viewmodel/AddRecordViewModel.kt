package com.sakito.healthylife.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sakito.healthylife.HealthyLifeApp
import com.sakito.healthylife.data.local.DietEntryEntity
import com.sakito.healthylife.data.local.DietRecordEntity
import com.sakito.healthylife.data.local.FoodUnitEntity
import com.sakito.healthylife.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EntryDraft(
    val foodId: Long?,
    val foodName: String,
    val unitId: Long?,
    val unitName: String,
    val unitGrams: Double,
    val quantityText: String = "1",
    val actualWeightText: String = "",
    val note: String = "",
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val animalProtein: Double? = null,
    val plantProtein: Double? = null,
    val fat: Double = 0.0,
    val carb: Double = 0.0,
    val fiber: Double? = null
) {
    val quantity: Double get() = quantityText.toDoubleOrNull() ?: 0.0
    val actualWeight: Double get() = actualWeightText.toDoubleOrNull() ?: (unitGrams * quantity)
}

data class AddRecordUiState(
    val recordId: Long = 0,
    val date: String = DateUtils.today(),
    val createdAt: Long = System.currentTimeMillis(),
    val mealType: String = "未分餐",
    val note: String = "",
    val entries: List<EntryDraft> = emptyList(),
    val saving: Boolean = false,
    val error: String? = null
)

class AddRecordViewModel(app: HealthyLifeApp, recordId: Long, date: String?) : ViewModel() {

    private val dietRepository = app.dietRepository
    private val foodRepository = app.foodRepository

    private val _uiState = MutableStateFlow(
        AddRecordUiState(recordId = recordId, date = date ?: DateUtils.today())
    )
    val uiState: StateFlow<AddRecordUiState> = _uiState.asStateFlow()

    init {
        if (recordId != 0L) {
            viewModelScope.launch {
                val record = dietRepository.getRecord(recordId) ?: return@launch
                _uiState.value = AddRecordUiState(
                    recordId = record.record.id,
                    date = record.record.date,
                    createdAt = record.record.createdAt,
                    mealType = record.record.mealType,
                    note = record.record.note.orEmpty(),
                    entries = record.entries.map {
                        EntryDraft(
                            foodId = it.foodId,
                            foodName = it.foodName,
                            unitId = null,
                            unitName = it.unitName,
                            unitGrams = it.unitGrams,
                            quantityText = it.quantity.toPlainString(),
                            actualWeightText = it.actualWeight.toPlainString(),
                            note = it.note.orEmpty(),
                            calories = it.calories,
                            protein = it.protein,
                            animalProtein = it.animalProtein,
                            plantProtein = it.plantProtein,
                            fat = it.fat,
                            carb = it.carb,
                            fiber = it.fiber
                        )
                    }
                )
            }
        }
    }

    fun updateDate(value: String) = _uiState.update { it.copy(date = value) }
    fun updateMealType(value: String) = _uiState.update { it.copy(mealType = value) }
    fun updateNote(value: String) = _uiState.update { it.copy(note = value) }

    fun addFood(
        foodId: Long,
        unitId: Long?,
        quantityText: String,
        actualWeightText: String?,
        note: String
    ) {
        viewModelScope.launch {
            val food = foodRepository.getFood(foodId) ?: return@launch
            val units = food.units
            val unit = units.firstOrNull { it.id == unitId } ?: units.firstOrNull { it.isBase } ?: units.first()
            val qty = quantityText.toDoubleOrNull() ?: 1.0
            val actual = actualWeightText?.toDoubleOrNull() ?: (unit.grams * qty)
            val factor = actual / 100.0
            val entry = EntryDraft(
                foodId = food.food.id,
                foodName = food.food.name,
                unitId = unit.id,
                unitName = unit.name,
                unitGrams = unit.grams,
                quantityText = quantityText,
                actualWeightText = actual.toPlainString(),
                note = note,
                calories = food.food.caloriesPer100g * factor,
                protein = food.food.proteinPer100g * factor,
                animalProtein = food.food.animalProteinPer100g?.let { it * factor },
                plantProtein = food.food.plantProteinPer100g?.let { it * factor },
                fat = food.food.fatPer100g * factor,
                carb = food.food.carbPer100g * factor,
                fiber = food.food.fiberPer100g?.let { it * factor }
            )
            _uiState.update { it.copy(entries = it.entries + entry) }
        }
    }

    fun updateEntryQuantity(index: Int, quantityText: String) {
        _uiState.update { state ->
            if (index !in state.entries.indices) return@update state
            val old = state.entries[index]
            val qty = quantityText.toDoubleOrNull() ?: 0.0
            val actual = old.unitGrams * qty
            state.copy(entries = state.entries.mapIndexed { i, e ->
                if (i == index) e.copy(quantityText = quantityText, actualWeightText = actual.toPlainString()) else e
            })
        }
    }

    fun updateEntryActualWeight(index: Int, actualWeightText: String) {
        _uiState.update { state ->
            if (index !in state.entries.indices) return@update state
            state.copy(entries = state.entries.mapIndexed { i, e ->
                if (i == index) e.copy(actualWeightText = actualWeightText) else e
            })
        }
    }

    fun updateEntryNote(index: Int, note: String) {
        _uiState.update { state ->
            state.copy(entries = state.entries.mapIndexed { i, e -> if (i == index) e.copy(note = note) else e })
        }
    }

    fun removeEntry(index: Int) {
        _uiState.update { state ->
            state.copy(entries = state.entries.filterIndexed { i, _ -> i != index })
        }
    }

    fun save(onSaved: (Long) -> Unit) {
        val s = _uiState.value
        if (s.entries.isEmpty()) {
            _uiState.update { it.copy(error = "请至少添加一个食物") }
            return
        }
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val record = DietRecordEntity(
                id = s.recordId,
                date = s.date,
                createdAt = s.createdAt,
                mealType = s.mealType,
                note = s.note.trim().ifEmpty { null }
            )
            val entries = s.entries.map { e ->
                DietEntryEntity(
                    recordId = s.recordId,
                    foodId = e.foodId,
                    foodName = e.foodName,
                    unitName = e.unitName,
                    unitGrams = e.unitGrams,
                    quantity = e.quantity,
                    actualWeight = e.actualWeight,
                    calories = e.calories,
                    protein = e.protein,
                    animalProtein = e.animalProtein,
                    plantProtein = e.plantProtein,
                    fat = e.fat,
                    carb = e.carb,
                    fiber = e.fiber,
                    note = e.note.trim().ifEmpty { null }
                )
            }
            val id = dietRepository.saveRecord(record, entries)
            _uiState.update { it.copy(saving = false) }
            onSaved(id)
        }
    }

    private fun Double.toPlainString(): String {
        return if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()
    }

    companion object {
        fun factory(app: HealthyLifeApp, recordId: Long, date: String?): ViewModelProvider.Factory = viewModelFactory {
            initializer { AddRecordViewModel(app, recordId, date) }
        }
    }
}
