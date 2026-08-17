package com.sakito.healthylife.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sakito.healthylife.HealthyLifeApp
import com.sakito.healthylife.data.local.FoodEntity
import com.sakito.healthylife.data.model.FoodFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class FoodLibraryViewModel(app: HealthyLifeApp) : ViewModel() {

    private val foodRepository = app.foodRepository

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _filter = MutableStateFlow(FoodFilter.ALL)
    val filter: StateFlow<FoodFilter> = _filter

    val foods: StateFlow<List<FoodEntity>> = combine(_query, _filter) { q, f -> q to f }
        .flatMapLatest { (q, f) -> foodRepository.observeFoods(f, q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(value: String) {
        _query.value = value
    }

    fun setFilter(value: FoodFilter) {
        _filter.value = value
    }

    fun deleteFood(foodId: Long) {
        viewModelScope.launch { foodRepository.deleteFood(foodId) }
    }

    companion object {
        fun factory(app: HealthyLifeApp): ViewModelProvider.Factory = viewModelFactory {
            initializer { FoodLibraryViewModel(app) }
        }
    }
}
