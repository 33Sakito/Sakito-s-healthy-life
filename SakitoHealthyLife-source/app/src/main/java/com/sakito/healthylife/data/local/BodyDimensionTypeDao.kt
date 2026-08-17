package com.sakito.healthylife.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyDimensionTypeDao {

    @Query("SELECT * FROM body_dimension_types ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<BodyDimensionTypeEntity>>

    @Query("SELECT * FROM body_dimension_types ORDER BY sortOrder ASC, id ASC")
    suspend fun getAll(): List<BodyDimensionTypeEntity>

    @Query("SELECT * FROM body_dimension_types WHERE id = :id")
    suspend fun getById(id: Long): BodyDimensionTypeEntity?

    @Query("SELECT * FROM body_dimension_types WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): BodyDimensionTypeEntity?

    @Insert
    suspend fun insert(type: BodyDimensionTypeEntity): Long

    @Update
    suspend fun update(type: BodyDimensionTypeEntity)

    @Delete
    suspend fun delete(type: BodyDimensionTypeEntity)
}
