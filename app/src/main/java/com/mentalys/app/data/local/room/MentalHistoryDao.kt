package com.mentalys.app.data.local.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mentalys.app.data.local.entity.mental.ScreeningHistoryEntity

@Dao
interface MentalHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScreening(entity: ScreeningHistoryEntity)

    @Query("SELECT * FROM screening_history ORDER BY createdAt DESC")
    fun getAllScreenings(): LiveData<List<ScreeningHistoryEntity>>

    @Query("SELECT * FROM screening_history ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentScreenings(limit: Int): List<ScreeningHistoryEntity>

    @Query("DELETE FROM screening_history")
    suspend fun clearScreenings()
}
