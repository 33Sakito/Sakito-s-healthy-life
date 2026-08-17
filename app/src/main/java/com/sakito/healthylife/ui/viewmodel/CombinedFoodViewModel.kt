package com.sakito.healthylife.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sakito.healthylife.HealthyLifeApp
import com.sakito.healthylife.data.local.FoodEntity
import com.sakito.healthylife.data.local.FoodWithDetails
import com.sakito.healthylife.data.model.CombinedComponentInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CombinedIngredientDraft(
    val food: FoodWithDetails,
    val unitId: Long?,
    val unitName: String,
    val unitGrams: Double,
    val quantity: String = "1"
)

data class CombinedFoodUiState(
    val name: String = "",
    val ingredients: List<CombinedIngredientDraft> = emptyList(),
    val keepPer100g: Boolean = true,
    val keepServing: Boolean = true,
    val extraUnits: List<Pair<String, String>> = emptyList(),
    val saving: Boolean = false,
    val error: String? = null
)

class CombinedFoodViewModel(app: HealthyLifeApp) : ViewModel() {

    private val foodRepository = app.foodRepository

    private val _uiState = MutableStateFlow(CombinedFoodUiState())
    val uiState: StateFlow<CombinedFoodUiState> = _uiState.asStateFlow()

    val allFoods: StateFlow<List<FoodEntity>> = foodRepository.observeFoods(com.sakito.healthylife.data.model.FoodFilter.ALL, "")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateName(value: String) = _uiState.value.let { _uiState.value = it.copy(name = value) }

    fun addIngredient(foodId: Long, unitId: Long?, quantity: String) {
        viewModelScope.launch {
            val food = foodRepository.getFood(foodId) ?: return@launch
            val units = food.units
            val unit = units.firstOrNull { it.id == unitId } ?: units.firstOrNull { it.isBase } ?: units.first()
            val draft = CombinedIngredientDraft(
                food = food,
                unitId = unit.id,
                unitName = unit.name,
                unitGrams = unit.grams,
                quantity = quantity
            )
            _uiState.value = _uiState.value.copy(ingredients = _uiState.value.ingredients + draft)
        }
    }

    fun updateIngredientQuantity(index: Int, quantity: String) {
        _uiState.value = _uiState.value.copy(
            ingredients = _uiState.value.ingredients.mapIndexed { i, it ->
                if (i == index) it.copy(quantity = quantity) else it
            }
        )
    }

    fun updateIngredientUnit(index: Int, unitId: Long) {
        _uiState.value = _uiState.value.copy(
            ingredients = _uiState.value.ingredients.mapIndexed { i, it ->
                if (i == index) {
                    val unit = it.food.units.firstOrNull { u -> u.id == unitId } ?: it.food.units.first()
                    it.copy(unitId = unit.id, unitName = unit.name, unitGrams = unit.grams)
                } else it
            }
        )
    }

    fun removeIngredient(index: Int) {
        _uiState.value = _uiState.value.copy(
            ingredients = _uiState.value.ingredients.filterIndexed { i, _ -> i != index }
        )
    }

    fun setKeepPer100g(value: Boolean) = _uiState.value.let { _uiState.value = it.copy(keepPer100g = value) }
    fun setKeepServing(value: Boolean) = _uiState.value.let { _uiState.value = it.copy(keepServing = value) }

    fun addExtraUnit() = _uiState.value.let {
        _uiState.value = it.copy(extraUnits = it.extraUnits + ("单位" to "100"))
    }

    fun updateExtraUnit(index: Int, name: String, grams: String) {
        _uiState.value = _uiState.value.copy(
            extraUnits = _uiState.value.extraUnits.mapIndexed { i, pair -> if (i == index) name to grams else pair }
        )
    }

    fun removeExtraUnit(index: Int) {
        _uiState.value = _uiState.value.copy(
            extraUnits = _uiState.value.extraUnits.filterIndexed { i, _ -> i != index }
        )
    }

    fun save(onSaved: (Long) -> Unit) {
        val s = _uiState.value
        if (s.name.isBlank()) {
            _uiState.value = s.copy(error = "请输入组合食物名称")
            return
        }
        if (s.ingredients.isEmpty()) {
            _uiState.value = s.copy(error = "请至少选择一个食材")
            return
        }
        val components = s.ingredients.map {
            CombinedComponentInput(
                ingredientFoodId = it.food.food.id,
                unitId = it.unitId,
                unitName = it.unitName,
                unitGrams = it.unitGrams,
                quantity = it.quantity.toDoubleOrNull() ?: 1.0
            )
        }
        _uiState.value = s.copy(saving = true, error = null)
        viewModelScope.launch {
            val id = foodRepository.createCombinedFood(
                name = s.name,
                components = components,
                keepPer100g = s.keepPer100g,
                keepServing = s.keepServing,
                extraUnits = s.extraUnits.mapNotNull { (name, grams) ->
                    val g = grams.toDoubleOrNull() ?: return@mapNotNull null
                    if (name.isBlank() || g <= 0) null else name to g
                },
                note = null
            )
            _uiState.value = s.copy(saving = false)
            onSaved(id)
        }
    }

    companion object {
        fun factory(app: HealthyLifeApp): ViewModelProvider.Factory = viewModelFactory {
            initializer { CombinedFoodViewModel(app) }
        }
    }
}
