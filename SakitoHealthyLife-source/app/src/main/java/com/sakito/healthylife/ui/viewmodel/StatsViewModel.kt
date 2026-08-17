package com.sakito.healthylife.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sakito.healthylife.HealthyLifeApp
import com.sakito.healthylife.data.local.BodyDimensionTypeEntity
import com.sakito.healthylife.data.local.SavedRangeEntity
import com.sakito.healthylife.data.model.NutrientSummary
import com.sakito.healthylife.data.repository.TrendPoint
import com.sakito.healthylife.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class StatsTab(val label: String) {
    WEIGHT("体重"),
    MEASUREMENT("围度"),
    NUTRITION("营养"),
    CALENDAR("日历")
}

enum class NutritionMetric(val key: String, val label: String) {
    CALORIES("calories", "热量"),
    PROTEIN("protein", "总蛋白"),
    ANIMAL_PROTEIN("animalProtein", "动物蛋白"),
    PLANT_PROTEIN("plantProtein", "植物蛋白"),
    FAT("fat", "脂肪"),
    CARB("carb", "总碳水"),
    FIBER("fiber", "膳食纤维")
}

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(app: HealthyLifeApp) : ViewModel() {

    private val statsRepository = app.statsRepository

    private val _tab = MutableStateFlow(StatsTab.WEIGHT)
    val tab: StateFlow<StatsTab> = _tab

    private val _startDate = MutableStateFlow(DateUtils.presetRange("最近一月").first)
    private val _endDate = MutableStateFlow(DateUtils.presetRange("最近一月").second)
    val startDate: StateFlow<String> = _startDate
    val endDate: StateFlow<String> = _endDate

    private val _useAverage = MutableStateFlow(false)
    val useAverage: StateFlow<Boolean> = _useAverage

    private val _dimensionTypeId = MutableStateFlow<Long?>(null)
    val dimensionTypeId: StateFlow<Long?> = _dimensionTypeId

    private val _nutritionMetric = MutableStateFlow(NutritionMetric.CALORIES)
    val nutritionMetric: StateFlow<NutritionMetric> = _nutritionMetric

    val dimensionTypes: StateFlow<List<BodyDimensionTypeEntity>> = app.bodyRepository.observeDimensionTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedRanges: StateFlow<List<SavedRangeEntity>> = statsRepository.observeSavedRanges()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trendPoints: StateFlow<List<TrendPoint>> =
        combine(_tab, _startDate, _endDate, _useAverage, _dimensionTypeId, _nutritionMetric) { values ->
            values
        }.flatMapLatest { values ->
            val tab = values[0] as StatsTab
            val start = values[1] as String
            val end = values[2] as String
            val useAvg = values[3] as Boolean
            val dimId = values[4] as Long?
            val metric = values[5] as NutritionMetric
            when (tab) {
                StatsTab.WEIGHT -> if (useAvg) statsRepository.observeWeightAverageTrend(start, end) else statsRepository.observeWeightTrend(start, end)
                StatsTab.MEASUREMENT -> {
                    val id = dimId ?: return@flatMapLatest kotlinx.coroutines.flow.flowOf(emptyList())
                    statsRepository.observeMeasurementTrend(id, start, end, useAvg)
                }
                StatsTab.NUTRITION -> statsRepository.observeNutritionTrend(start, end) { summary -> summaryValue(summary, metric) }
                StatsTab.CALENDAR -> kotlinx.coroutines.flow.flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tab: StatsTab) {
        _tab.value = tab
        if (tab == StatsTab.MEASUREMENT && _dimensionTypeId.value == null) {
            _dimensionTypeId.value = dimensionTypes.value.firstOrNull()?.id
        }
    }

    fun setRange(start: String, end: String) {
        _startDate.value = start
        _endDate.value = end
    }

    fun setPreset(preset: String) {
        val (s, e) = DateUtils.presetRange(preset)
        setRange(s, e)
    }

    fun setUseAverage(value: Boolean) {
        _useAverage.value = value
    }

    fun setDimensionType(id: Long) {
        _dimensionTypeId.value = id
    }

    fun setNutritionMetric(metric: NutritionMetric) {
        _nutritionMetric.value = metric
    }

    fun saveRange(name: String, start: String, end: String) {
        viewModelScope.launch {
            statsRepository.addSavedRange(name, start, end)
        }
    }

    fun deleteRange(id: Long) {
        viewModelScope.launch { statsRepository.deleteSavedRange(id) }
    }

    private fun summaryValue(s: NutrientSummary, metric: NutritionMetric): Double? {
        return when (metric) {
            NutritionMetric.CALORIES -> s.calories
            NutritionMetric.PROTEIN -> s.protein
            NutritionMetric.ANIMAL_PROTEIN -> s.animalProtein
            NutritionMetric.PLANT_PROTEIN -> s.plantProtein
            NutritionMetric.FAT -> s.fat
            NutritionMetric.CARB -> s.carb
            NutritionMetric.FIBER -> s.fiber
        }
    }

    companion object {
        fun factory(app: HealthyLifeApp): ViewModelProvider.Factory = viewModelFactory {
            initializer { StatsViewModel(app) }
        }
    }
}
