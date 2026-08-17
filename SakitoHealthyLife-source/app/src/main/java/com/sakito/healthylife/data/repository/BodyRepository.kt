package com.sakito.healthylife.data.repository

import androidx.room.withTransaction
import com.sakito.healthylife.data.local.AppDatabase
import com.sakito.healthylife.data.local.BodyDimensionTypeEntity
import com.sakito.healthylife.data.local.BodyMeasurementEntity
import com.sakito.healthylife.data.local.WeightRecordEntity
import kotlinx.coroutines.flow.Flow

class BodyRepository(
    private val database: AppDatabase,
    private val weightDao: com.sakito.healthylife.data.local.WeightRecordDao,
    private val measurementDao: com.sakito.healthylife.data.local.BodyMeasurementDao,
    private val dimensionTypeDao: com.sakito.healthylife.data.local.BodyDimensionTypeDao
) {

    fun observeWeights(): Flow<List<WeightRecordEntity>> = weightDao.observeAll()

    fun observeWeightsBetween(start: String, end: String): Flow<List<WeightRecordEntity>> =
        weightDao.observeBetween(start, end)

    suspend fun addWeight(date: String, weightKg: Double, note: String?): Long {
        return weightDao.insert(WeightRecordEntity(date = date, weightKg = weightKg, note = note))
    }

    suspend fun updateWeight(record: WeightRecordEntity) = weightDao.update(record)

    suspend fun deleteWeight(recordId: Long) {
        weightDao.getById(recordId)?.let { weightDao.delete(it) }
    }

    fun observeDimensionTypes(): Flow<List<BodyDimensionTypeEntity>> = dimensionTypeDao.observeAll()

    suspend fun addDimensionType(name: String): Long {
        val existing = dimensionTypeDao.findByName(name.trim())
        if (existing != null) return existing.id
        val all = dimensionTypeDao.getAll()
        return dimensionTypeDao.insert(
            BodyDimensionTypeEntity(
                name = name.trim(),
                isCustom = true,
                sortOrder = (all.maxOfOrNull { it.sortOrder } ?: -1) + 1
            )
        )
    }

    suspend fun renameDimensionType(id: Long, newName: String) {
        dimensionTypeDao.getById(id)?.let {
            dimensionTypeDao.update(it.copy(name = newName.trim()))
        }
    }

    suspend fun deleteDimensionType(id: Long) {
        dimensionTypeDao.getById(id)?.let { dimensionTypeDao.delete(it) }
    }

    fun observeMeasurements(start: String, end: String): Flow<List<BodyMeasurementEntity>> =
        measurementDao.observeBetween(start, end)

    fun observeMeasurements(typeId: Long, start: String, end: String): Flow<List<BodyMeasurementEntity>> =
        measurementDao.observeBetween(typeId, start, end)

    suspend fun addMeasurement(
        date: String,
        dimensionTypeId: Long,
        valueCm: Double,
        note: String?
    ): Long {
        return measurementDao.insert(
            BodyMeasurementEntity(
                date = date,
                dimensionTypeId = dimensionTypeId,
                valueCm = valueCm,
                note = note
            )
        )
    }

    suspend fun updateMeasurement(record: BodyMeasurementEntity) = measurementDao.update(record)

    suspend fun deleteMeasurement(recordId: Long) {
        measurementDao.getById(recordId)?.let { measurementDao.delete(it) }
    }

    suspend fun getWeightDates(): Set<String> = weightDao.getAllDates().toSet()
    suspend fun getMeasurementDates(): Set<String> = measurementDao.getAllDates().toSet()
}
