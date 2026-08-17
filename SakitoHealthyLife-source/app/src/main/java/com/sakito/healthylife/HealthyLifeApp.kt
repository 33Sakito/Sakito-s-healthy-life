package com.sakito.healthylife

import android.app.Application
import com.sakito.healthylife.data.local.AppDatabase
import com.sakito.healthylife.data.local.SeedData
import com.sakito.healthylife.data.repository.BodyRepository
import com.sakito.healthylife.data.repository.DietRepository
import com.sakito.healthylife.data.repository.FoodRepository
import com.sakito.healthylife.data.repository.StatsRepository
import com.sakito.healthylife.data.settings.SettingsRepository
import com.sakito.healthylife.notification.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HealthyLifeApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.build(this) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }

    val foodRepository: FoodRepository by lazy {
        FoodRepository(database, database.foodDao(), database.foodUnitDao(), database.micronutrientDao(), database.combinedComponentDao())
    }

    val dietRepository: DietRepository by lazy {
        DietRepository(database.dietRecordDao(), database.dietEntryDao(), foodRepository)
    }

    val bodyRepository: BodyRepository by lazy {
        BodyRepository(database.weightRecordDao(), database.bodyMeasurementDao(), database.bodyDimensionTypeDao())
    }

    val statsRepository: StatsRepository by lazy {
        StatsRepository(
            database.dietRecordDao(),
            database.dietEntryDao(),
            database.weightRecordDao(),
            database.bodyMeasurementDao(),
            database.bodyDimensionTypeDao(),
            database.savedRangeDao()
        )
    }

    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(this) }

    val backupManager: com.sakito.healthylife.data.backup.BackupManager by lazy {
        com.sakito.healthylife.data.backup.BackupManager(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        applicationScope.launch {
            runCatching { SeedData.seedIfEmpty(database) }
        }
        reminderScheduler.scheduleDailyReminder()
    }

    companion object {
        lateinit var instance: HealthyLifeApp
            private set
    }
}
