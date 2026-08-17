package com.sakito.healthylife.data.repository

import com.sakito.healthylife.data.local.AppDatabase
import com.sakito.healthylife.data.local.DietRecordWithEntries
import com.sakito.healthylife.data.local.SavedRangeEntity
import com.sakito.healthylife.data.model.NutrientSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class TrendPoint(val date: String, val value: Double)

data class DayStats(
    val dietDays: Int = 0,
    val weightDays: Int = 0,
    val measurementDays: Int = 0,
    val totalDays: Int = 0
)

class StatsRepository(
    private val database: AppDatabase,
    private val dietRecordDao: com.sakito.healthylife.data.local.DietRecordDao,
    private val dietEntryDao: com.sakito.healthylife.data.local.DietEntryDao,
    private val weightDao: com.sakito.healthylife.data.local.WeightRecordDao,
    private val measurementDao: com.sakito.healthylife.data.local.BodyMeasurementDao,
    private val dimensionTypeDao: com.sakito.healthylife.data.local.BodyDimensionTypeDao,
    private val savedRangeDao: com.sakito.healthylife.data.local.SavedRangeDao
) {

    fun observeWeightTrend(start: String, end: String): Flow<List<TrendPoint>> {
        return weightDao.observeBetween(start, end).map { records ->
            records.groupBy { it.date }.mapNotNull { (date, list) ->
                val latest = list.maxByOrNull { it.createdAt } ?: return@mapNotNull null
                TrendPoint(date, latest.weightKg)
            }.sortedBy { it.date }
        }
    }

    fun observeWeightAverageTrend(start: String, end: String): Flow<List<TrendPoint>> {
        return weightDao.observeBetween(start, end).map { records ->
            records.groupBy { it.date }.mapNotNull { (date, list) ->
                if (list.isEmpty()) return@mapNotNull null
                TrendPoint(date, list.map { it.weightKg }.average())
            }.sortedBy { it.date }
        }
    }

    fun observeMeasurementTrend(
        typeId: Long,
        start: String,
        end: String,
        useAverage: Boolean
    ): Flow<List<TrendPoint>> {
        return measurementDao.observeBetween(typeId, start, end).map { records ->
            records.groupBy { it.date }.mapNotNull { (date, list) ->
                if (list.isEmpty()) return@mapNotNull null
                val value = if (useAverage) list.map { it.valueCm }.average() else list.maxByOrNull { it.createdAt }!!.valueCm
                TrendPoint(date, value)
            }.sortedBy { it.date }
        }
    }

    fun observeNutritionTrend(
        start: String,
        end: String,
        selector: (NutrientSummary) -> Double?
    ): Flow<List<TrendPoint>> {
        return dietRecordDao.observeBetween(start, end).map { records ->
            records.groupBy { it.record.date }.mapNotNull { (date, dayRecords) ->
                val summary = summarize(dayRecords)
                val value = selector(summary) ?: return@mapNotNull null
                TrendPoint(date, value)
            }.sortedBy { it.date }
        }
    }

    fun observeNutritionAverageTrend(
        start: String,
        end: String,
        selector: (NutrientSummary) -> Double?
    ): Flow<List<TrendPoint>> {
        return dietRecordDao.observeBetween(start, end).map { records ->
            records.groupBy { it.record.date }.mapNotNull { (date, dayRecords) ->
                val summary = summarize(dayRecords)
                val value = selector(summary) ?: return@mapNotNull null
                TrendPoint(date, value)
            }.sortedBy { it.date }
        }
    }

    suspend fun getDayStats(start: String, end: String): DayStats {
        val dietDays = dietRecordDao.countDays(start, end)
        val weightDays = weightDao.countDays(start, end)
        val measurementDays = measurementDao.countDays(start, end)
        return DayStats(
            dietDays = dietDays,
            weightDays = weightDays,
            measurementDays = measurementDays,
            totalDays = totalDaysBetween(start, end)
        )
    }

    suspend fun getDietDates(): Set<String> = dietRecordDao.getAllDates().toSet()
    suspend fun getWeightDates(): Set<String> = weightDao.getAllDates().toSet()
    suspend fun getMeasurementDates(): Set<String> = measurementDao.getAllDates().toSet()

    fun observeSavedRanges(): Flow<List<SavedRangeEntity>> = savedRangeDao.observeAll()

    suspend fun getSavedRanges(): List<SavedRangeEntity> = savedRangeDao.getAll()

    suspend fun addSavedRange(name: String, start: String, end: String): Long {
        return savedRangeDao.insert(SavedRangeEntity(name = name.trim(), startDate = start, endDate = end))
    }

    suspend fun updateSavedRange(range: SavedRangeEntity) = savedRangeDao.update(range)

    suspend fun deleteSavedRange(id: Long) {
        savedRangeDao.getAll().firstOrNull { it.id == id }?.let { savedRangeDao.delete(it) }
    }

    private fun summarize(records: List<DietRecordWithEntries>): NutrientSummary {
        var summary = NutrientSummary()
        records.forEach { record ->
            record.entries.forEach { entry ->
                summary = summary.plus(
                    NutrientSummary(
                        calories = entry.calories,
                        protein = entry.protein,
                        animalProtein = entry.animalProtein,
                        plantProtein = entry.plantProtein,
                        fat = entry.fat,
                        carb = entry.carb,
                        fiber = entry.fiber
                    )
                )
            }
        }
        return summary
    }

    private fun totalDaysBetween(start: String, end: String): Int {
        return try {
            val s = java.time.LocalDate.parse(start)
            val e = java.time.LocalDate.parse(end)
            java.time.temporal.ChronoUnit.DAYS.between(s, e).toInt() + 1
        } catch (ex: Exception) {
            0
        }
    }
}
