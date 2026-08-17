package com.sakito.healthylife.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sakito.healthylife.HealthyLifeApp
import com.sakito.healthylife.data.local.DietRecordWithEntries
import com.sakito.healthylife.data.local.FoodEntity
import com.sakito.healthylife.data.model.FoodFilter
import com.sakito.healthylife.data.model.NutrientSummary
import com.sakito.healthylife.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(app: HealthyLifeApp) : ViewModel() {

    private val dietRepository = app.dietRepository
    private val foodRepository = app.foodRepository

    private val _selectedDate = MutableStateFlow(DateUtils.today())
    val selectedDate: StateFlow<String> = _selectedDate

    val records: StateFlow<List<DietRecordWithEntries>> = _selectedDate
        .flatMapLatest { dietRepository.observeByDate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val daySummary: StateFlow<NutrientSummary> = _selectedDate
        .flatMapLatest { date ->
            flow { emit(dietRepository.getDaySummary(date)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NutrientSummary())

    val commonFoods: StateFlow<List<FoodEntity>> = foodRepository.observeFoods(FoodFilter.COMMON, "")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentFoods: StateFlow<List<FoodEntity>> = foodRepository.observeFoods(FoodFilter.RECENT, "")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDate(date: String) {
        _selectedDate.value = date
    }

    fun shiftDate(days: Long) {
        _selectedDate.value = DateUtils.addDays(_selectedDate.value, days)
    }

    fun deleteRecord(recordId: Long) {
        viewModelScope.launch { dietRepository.deleteRecord(recordId) }
    }

    fun copyRecord(recordId: Long, targetDate: String) {
        viewModelScope.launch {
            dietRepository.copyRecord(recordId, targetDate)
        }
    }

    fun copyDay(sourceDate: String, targetDate: String) {
        viewModelScope.launch {
            dietRepository.copyDayToDate(sourceDate, targetDate)
        }
    }

    companion object {
        fun factory(app: HealthyLifeApp): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(app) }
        }
    }
}
