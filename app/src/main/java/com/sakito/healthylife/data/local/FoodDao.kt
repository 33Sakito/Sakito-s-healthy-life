package com.sakito.healthylife.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

    @Query("SELECT * FROM foods ORDER BY usageCount DESC, lastUsedAt DESC, name COLLATE NOCASE")
    fun observeAll(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods ORDER BY name COLLATE NOCASE")
    fun observeAllByName(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE name LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY name COLLATE NOCASE")
    fun search(query: String): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE isCustom = 0 ORDER BY name COLLATE NOCASE")
    fun observeBuiltIn(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE isCustom = 1 ORDER BY name COLLATE NOCASE")
    fun observeCustom(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE usageCount > 0 ORDER BY usageCount DESC, lastUsedAt DESC LIMIT :limit")
    fun observeCommon(limit: Int): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE lastUsedAt IS NOT NULL ORDER BY lastUsedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE id = :id")
    fun observeById(id: Long): Flow<FoodWithDetails?>

    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun getById(id: Long): FoodWithDetails?

    @Query("SELECT * FROM foods WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<FoodEntity>

    @Query("SELECT * FROM foods WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): FoodEntity?

    @Query("SELECT * FROM foods ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<FoodEntity>

    @Insert
    suspend fun insert(food: FoodEntity): Long

    @Update
    suspend fun update(food: FoodEntity)

    @Delete
    suspend fun delete(food: FoodEntity)

    @Query("UPDATE foods SET usageCount = usageCount + 1, lastUsedAt = :now WHERE id = :id")
    suspend fun bumpUsage(id: Long, now: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM foods")
    suspend fun count(): Int
}
