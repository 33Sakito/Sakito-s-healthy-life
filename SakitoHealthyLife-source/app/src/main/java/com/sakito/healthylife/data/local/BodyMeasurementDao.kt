package com.sakito.healthylife.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyMeasurementDao {

    @Query("SELECT * FROM body_measurements WHERE date BETWEEN :start AND :end ORDER BY date ASC, createdAt ASC")
    fun observeBetween(start: String, end: String): Flow<List<BodyMeasurementEntity>>

    @Query("SELECT * FROM body_measurements WHERE date BETWEEN :start AND :end AND dimensionTypeId = :typeId ORDER BY date ASC, createdAt ASC")
    fun observeBetween(typeId: Long, start: String, end: String): Flow<List<BodyMeasurementEntity>>

    @Query("SELECT * FROM body_measurements WHERE date BETWEEN :start AND :end ORDER BY date ASC, createdAt ASC")
    suspend fun getBetween(start: String, end: String): List<BodyMeasurementEntity>

    @Query("SELECT * FROM body_measurements WHERE id = :id")
    suspend fun getById(id: Long): BodyMeasurementEntity?

    @Insert
    suspend fun insert(record: BodyMeasurementEntity): Long

    @Update
    suspend fun update(record: BodyMeasurementEntity)

    @Delete
    suspend fun delete(record: BodyMeasurementEntity)

    @Query("SELECT DISTINCT date FROM body_measurements")
    suspend fun getAllDates(): List<String>

    @Query("SELECT COUNT(DISTINCT date) FROM body_measurements WHERE date BETWEEN :start AND :end")
    suspend fun countDays(start: String, end: String): Int
}
