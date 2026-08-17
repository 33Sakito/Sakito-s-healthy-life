package com.sakito.healthylife.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DietEntryDao {

    @Insert
    suspend fun insert(entry: DietEntryEntity): Long

    @Insert
    suspend fun insertAll(entries: List<DietEntryEntity>)

    @Query("SELECT * FROM diet_entries WHERE recordId = :recordId ORDER BY id ASC")
    suspend fun getByRecord(recordId: Long): List<DietEntryEntity>

    @Query("DELETE FROM diet_entries WHERE recordId = :recordId")
    suspend fun deleteByRecord(recordId: Long)
}
