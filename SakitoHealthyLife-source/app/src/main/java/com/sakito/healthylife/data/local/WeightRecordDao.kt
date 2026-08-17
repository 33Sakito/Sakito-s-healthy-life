package com.sakito.healthylife.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightRecordDao {

    @Query("SELECT * FROM weight_records ORDER BY date DESC, createdAt DESC")
    fun observeAll(): Flow<List<WeightRecordEntity>>

    @Query("SELECT * FROM weight_records WHERE date BETWEEN :start AND :end ORDER BY date ASC, createdAt ASC")
    fun observeBetween(start: String, end: String): Flow<List<WeightRecordEntity>>

    @Query("SELECT * FROM weight_records WHERE date BETWEEN :start AND :end ORDER BY date ASC, createdAt ASC")
    suspend fun getBetween(start: String, end: String): List<WeightRecordEntity>

    @Query("SELECT * FROM weight_records WHERE id = :id")
    suspend fun getById(id: Long): WeightRecordEntity?

    @Insert
    suspend fun insert(record: WeightRecordEntity): Long

    @Update
    suspend fun update(record: WeightRecordEntity)

    @Delete
    suspend fun delete(record: WeightRecordEntity)

    @Query("SELECT DISTINCT date FROM weight_records")
    suspend fun getAllDates(): List<String>

    @Query("SELECT COUNT(DISTINCT date) FROM weight_records WHERE date BETWEEN :start AND :end")
    suspend fun countDays(start: String, end: String): Int
}
