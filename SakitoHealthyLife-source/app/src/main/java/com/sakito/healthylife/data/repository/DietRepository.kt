package com.sakito.healthylife.data.repository

import androidx.room.withTransaction
import com.sakito.healthylife.data.local.AppDatabase
import com.sakito.healthylife.data.local.DietEntryEntity
import com.sakito.healthylife.data.local.DietRecordEntity
import com.sakito.healthylife.data.local.DietRecordWithEntries
import com.sakito.healthylife.data.model.NutrientSummary
import kotlinx.coroutines.flow.Flow

class DietRepository(
    private val database: AppDatabase,
    private val recordDao: com.sakito.healthylife.data.local.DietRecordDao,
    private val entryDao: com.sakito.healthylife.data.local.DietEntryDao,
    private val foodRepository: FoodRepository
) {

    fun observeByDate(date: String): Flow<List<DietRecordWithEntries>> = recordDao.observeByDate(date)

    fun observeBetween(start: String, end: String): Flow<List<DietRecordWithEntries>> =
        recordDao.observeBetween(start, end)

    suspend fun getRecord(id: Long): DietRecordWithEntries? = recordDao.getById(id)

    suspend fun getByDate(date: String): List<DietRecordWithEntries> = recordDao.getByDate(date)

    suspend fun saveRecord(
        record: DietRecordEntity,
        entries: List<DietEntryEntity>
    ): Long = database.withTransaction {
        val recordId = if (record.id == 0L) {
            recordDao.insert(record)
        } else {
            recordDao.update(record)
            record.id
        }
        entryDao.deleteByRecord(recordId)
        entryDao.insertAll(entries.map { it.copy(id = 0, recordId = recordId) })
        entries.mapNotNull { it.foodId }.distinct().forEach { foodId ->
            foodRepository.bumpUsage(foodId)
        }
        recordId
    }

    suspend fun deleteRecord(recordId: Long) = database.withTransaction {
        recordDao.getById(recordId)?.let { recordDao.delete(it.record) }
    }

    suspend fun copyDayToDate(sourceDate: String, targetDate: String): Int = database.withTransaction {
        val records = recordDao.getByDate(sourceDate)
        records.forEach { source ->
            val newRecordId = recordDao.insert(
                source.record.copy(id = 0, date = targetDate, createdAt = System.currentTimeMillis())
            )
            entryDao.insertAll(source.entries.map { it.copy(id = 0, recordId = newRecordId) })
        }
        records.size
    }

    suspend fun copyRecord(recordId: Long, targetDate: String, targetMealType: String? = null): Long =
        database.withTransaction {
            val source = recordDao.getById(recordId) ?: return@withTransaction 0L
            val newRecordId = recordDao.insert(
                source.record.copy(
                    id = 0,
                    date = targetDate,
                    createdAt = System.currentTimeMillis(),
                    mealType = targetMealType ?: source.record.mealType
                )
            )
            entryDao.insertAll(source.entries.map { it.copy(id = 0, recordId = newRecordId) })
            newRecordId
        }

    suspend fun getDaySummary(date: String): NutrientSummary {
        val records = recordDao.getByDate(date)
        return summarize(records)
    }

    suspend fun getPeriodSummary(start: String, end: String): NutrientSummary {
        val records = recordDao.getBetween(start, end)
        return summarize(records)
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
}
