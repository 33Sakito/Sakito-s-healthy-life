package com.sakito.healthylife.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DietRecordDao {

    @Query("SELECT * FROM diet_records WHERE date = :date ORDER BY createdAt ASC")
    fun observeByDate(date: String): Flow<List<DietRecordWithEntries>>

    @Query("SELECT * FROM diet_records WHERE date BETWEEN :start AND :end ORDER BY date ASC, createdAt ASC")
    fun observeBetween(start: String, end: String): Flow<List<DietRecordWithEntries>>

    @Query("SELECT * FROM diet_records WHERE id = :id")
    fun observeById(id: Long): Flow<DietRecordWithEntries?>

    @Query("SELECT * FROM diet_records WHERE id = :id")
    suspend fun getById(id: Long): DietRecordWithEntries?

    @Query("SELECT * FROM diet_records WHERE date = :date ORDER BY createdAt ASC")
    suspend fun getByDate(date: String): List<DietRecordWithEntries>

    @Query("SELECT * FROM diet_records WHERE date BETWEEN :start AND :end ORDER BY date ASC, createdAt ASC")
    suspend fun getBetween(start: String, end: String): List<DietRecordWithEntries>

    @Insert
    suspend fun insert(record: DietRecordEntity): Long

    @Update
    suspend fun update(record: DietRecordEntity)

    @Delete
    suspend fun delete(record: DietRecordEntity)

    @Query("SELECT DISTINCT date FROM diet_records")
    suspend fun getAllDates(): List<String>

    @Query("SELECT COUNT(DISTINCT date) FROM diet_records WHERE date BETWEEN :start AND :end")
    suspend fun countDays(start: String, end: String): Int
}
