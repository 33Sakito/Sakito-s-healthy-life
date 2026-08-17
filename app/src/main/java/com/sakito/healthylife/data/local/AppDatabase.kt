package com.sakito.healthylife.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FoodEntity::class,
        FoodUnitEntity::class,
        MicronutrientEntity::class,
        CombinedComponentEntity::class,
        DietRecordEntity::class,
        DietEntryEntity::class,
        WeightRecordEntity::class,
        BodyDimensionTypeEntity::class,
        BodyMeasurementEntity::class,
        SavedRangeEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun foodDao(): FoodDao
    abstract fun foodUnitDao(): FoodUnitDao
    abstract fun micronutrientDao(): MicronutrientDao
    abstract fun combinedComponentDao(): CombinedComponentDao
    abstract fun dietRecordDao(): DietRecordDao
    abstract fun dietEntryDao(): DietEntryDao
    abstract fun weightRecordDao(): WeightRecordDao
    abstract fun bodyDimensionTypeDao(): BodyDimensionTypeDao
    abstract fun bodyMeasurementDao(): BodyMeasurementDao
    abstract fun savedRangeDao(): SavedRangeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun build(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sakito_healthy_life.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
