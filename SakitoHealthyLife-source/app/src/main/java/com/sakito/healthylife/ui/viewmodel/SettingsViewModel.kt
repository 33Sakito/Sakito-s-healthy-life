package com.sakito.healthylife.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sakito.healthylife.HealthyLifeApp
import com.sakito.healthylife.data.settings.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: HealthyLifeApp) : ViewModel() {

    private val settingsRepository = app.settingsRepository
    private val reminderScheduler = app.reminderScheduler

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun setDecimalPlaces(value: Int) {
        viewModelScope.launch { settingsRepository.setDecimalPlaces(value) }
    }

    fun setDefaultMeal(value: String) {
        viewModelScope.launch { settingsRepository.setDefaultMeal(value) }
    }

    fun setTrendDefaultValue(value: String) {
        viewModelScope.launch { settingsRepository.setTrendDefaultValue(value) }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReminderEnabled(enabled)
            val updated = settingsRepository.settings.first()
            reminderScheduler.reschedule(updated)
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setReminderTime(hour, minute)
            val updated = settingsRepository.settings.first()
            reminderScheduler.reschedule(updated)
        }
    }

    companion object {
        fun factory(app: HealthyLifeApp): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(app) }
        }
    }
}
