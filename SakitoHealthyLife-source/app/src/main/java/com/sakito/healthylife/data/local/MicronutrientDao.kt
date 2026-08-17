package com.sakito.healthylife.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MicronutrientDao {

    @Query("SELECT * FROM micronutrients WHERE foodId = :foodId ORDER BY name COLLATE NOCASE")
    fun observeByFood(foodId: Long): Flow<List<MicronutrientEntity>>

    @Query("SELECT * FROM micronutrients WHERE foodId = :foodId ORDER BY name COLLATE NOCASE")
    suspend fun getByFood(foodId: Long): List<MicronutrientEntity>

    @Insert
    suspend fun insert(item: MicronutrientEntity): Long

    @Insert
    suspend fun insertAll(items: List<MicronutrientEntity>)

    @Update
    suspend fun update(item: MicronutrientEntity)

    @Delete
    suspend fun delete(item: MicronutrientEntity)

    @Query("DELETE FROM micronutrients WHERE foodId = :foodId")
    suspend fun deleteByFood(foodId: Long)
}
