package com.sakito.healthylife.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodUnitDao {

    @Query("SELECT * FROM food_units WHERE foodId = :foodId ORDER BY sortOrder ASC, id ASC")
    fun observeByFood(foodId: Long): Flow<List<FoodUnitEntity>>

    @Query("SELECT * FROM food_units WHERE foodId = :foodId ORDER BY sortOrder ASC, id ASC")
    suspend fun getByFood(foodId: Long): List<FoodUnitEntity>

    @Query("SELECT * FROM food_units WHERE id = :id")
    suspend fun getById(id: Long): FoodUnitEntity?

    @Insert
    suspend fun insert(unit: FoodUnitEntity): Long

    @Insert
    suspend fun insertAll(units: List<FoodUnitEntity>)

    @Update
    suspend fun update(unit: FoodUnitEntity)

    @Delete
    suspend fun delete(unit: FoodUnitEntity)

    @Query("DELETE FROM food_units WHERE foodId = :foodId")
    suspend fun deleteByFood(foodId: Long)
}
