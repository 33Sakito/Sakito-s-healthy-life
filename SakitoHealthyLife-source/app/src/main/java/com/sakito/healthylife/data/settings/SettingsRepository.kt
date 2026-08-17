package com.sakito.healthylife.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val decimalPlaces: Int = 1,
    val defaultMeal: String = "未分餐",
    val trendDefaultValue: String = "latest",
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val DECIMAL_PLACES = intPreferencesKey("decimal_places")
        val DEFAULT_MEAL = stringPreferencesKey("default_meal")
        val TREND_DEFAULT = stringPreferencesKey("trend_default_value")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
    }

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw exception
        }
        .map { prefs ->
            AppSettings(
                decimalPlaces = prefs[Keys.DECIMAL_PLACES] ?: 1,
                defaultMeal = prefs[Keys.DEFAULT_MEAL] ?: "未分餐",
                trendDefaultValue = prefs[Keys.TREND_DEFAULT] ?: "latest",
                reminderEnabled = prefs[Keys.REMINDER_ENABLED] ?: true,
                reminderHour = prefs[Keys.REMINDER_HOUR] ?: 20,
                reminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 0
            )
        }

    suspend fun setDecimalPlaces(value: Int) {
        context.dataStore.edit { it[Keys.DECIMAL_PLACES] = value.coerceIn(0, 3) }
    }

    suspend fun setDefaultMeal(value: String) {
        context.dataStore.edit { it[Keys.DEFAULT_MEAL] = value }
    }

    suspend fun setTrendDefaultValue(value: String) {
        context.dataStore.edit { it[Keys.TREND_DEFAULT] = value }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REMINDER_ENABLED] = enabled }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.REMINDER_HOUR] = hour.coerceIn(0, 23)
            it[Keys.REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }
}
