package com.sakito.healthylife.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CombinedComponentDao {

    @Query("SELECT * FROM combined_components WHERE parentFoodId = :parentFoodId ORDER BY id ASC")
    suspend fun getByParent(parentFoodId: Long): List<CombinedComponentEntity>

    @Query("SELECT * FROM combined_components WHERE ingredientFoodId = :ingredientFoodId")
    suspend fun getByIngredient(ingredientFoodId: Long): List<CombinedComponentEntity>

    @Query("SELECT DISTINCT parentFoodId FROM combined_components WHERE ingredientFoodId = :ingredientFoodId")
    suspend fun getParentIdsUsingIngredient(ingredientFoodId: Long): List<Long>

    @Insert
    suspend fun insertAll(components: List<CombinedComponentEntity>)

    @Insert
    suspend fun insert(component: CombinedComponentEntity): Long

    @Delete
    suspend fun delete(component: CombinedComponentEntity)

    @Query("DELETE FROM combined_components WHERE parentFoodId = :parentFoodId")
    suspend fun deleteByParent(parentFoodId: Long)
}
