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
import kotlinx.coroutines.flow.stateIn

class MainViewModel(app: HealthyLifeApp) : ViewModel() {
    val settings: StateFlow<AppSettings> = app.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    companion object {
        fun factory(app: HealthyLifeApp): ViewModelProvider.Factory = viewModelFactory {
            initializer { MainViewModel(app) }
        }
    }
}
