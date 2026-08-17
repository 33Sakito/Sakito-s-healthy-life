package com.sakito.healthylife.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedRangeDao {

    @Query("SELECT * FROM saved_ranges ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<SavedRangeEntity>>

    @Query("SELECT * FROM saved_ranges ORDER BY createdAt ASC")
    suspend fun getAll(): List<SavedRangeEntity>

    @Insert
    suspend fun insert(range: SavedRangeEntity): Long

    @Update
    suspend fun update(range: SavedRangeEntity)

    @Delete
    suspend fun delete(range: SavedRangeEntity)
}
