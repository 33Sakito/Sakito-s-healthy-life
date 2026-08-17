package com.sakito.healthylife.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sakito.healthylife.HealthyLifeApp
import com.sakito.healthylife.data.local.BodyDimensionTypeEntity
import com.sakito.healthylife.data.local.BodyMeasurementEntity
import com.sakito.healthylife.data.local.WeightRecordEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BodyViewModel(app: HealthyLifeApp) : ViewModel() {

    private val bodyRepository = app.bodyRepository

    val weights: StateFlow<List<WeightRecordEntity>> = bodyRepository.observeWeights()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dimensionTypes: StateFlow<List<BodyDimensionTypeEntity>> = bodyRepository.observeDimensionTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val measurements: StateFlow<List<BodyMeasurementEntity>> = bodyRepository.observeMeasurements("0000-01-01", "9999-12-31")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addWeight(date: String, weightKg: Double, note: String?) {
        viewModelScope.launch { bodyRepository.addWeight(date, weightKg, note) }
    }

    fun deleteWeight(id: Long) {
        viewModelScope.launch { bodyRepository.deleteWeight(id) }
    }

    fun addMeasurement(date: String, typeId: Long, valueCm: Double, note: String?) {
        viewModelScope.launch { bodyRepository.addMeasurement(date, typeId, valueCm, note) }
    }

    fun deleteMeasurement(id: Long) {
        viewModelScope.launch { bodyRepository.deleteMeasurement(id) }
    }

    fun addDimensionType(name: String) {
        viewModelScope.launch { bodyRepository.addDimensionType(name) }
    }

    fun renameDimensionType(id: Long, name: String) {
        viewModelScope.launch { bodyRepository.renameDimensionType(id, name) }
    }

    fun deleteDimensionType(id: Long) {
        viewModelScope.launch { bodyRepository.deleteDimensionType(id) }
    }

    companion object {
        fun factory(app: HealthyLifeApp): ViewModelProvider.Factory = viewModelFactory {
            initializer { BodyViewModel(app) }
        }
    }
}
