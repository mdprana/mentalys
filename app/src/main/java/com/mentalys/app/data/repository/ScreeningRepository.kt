package com.mentalys.app.data.repository

import androidx.lifecycle.LiveData
import com.mentalys.app.data.local.entity.mental.ScreeningHistoryEntity
import com.mentalys.app.data.local.room.MentalHistoryDao

class ScreeningRepository private constructor(
    private val dao: MentalHistoryDao
) {

    suspend fun saveScreening(entity: ScreeningHistoryEntity) = dao.insertScreening(entity)

    fun getHistory(): LiveData<List<ScreeningHistoryEntity>> = dao.getAllScreenings()

    suspend fun clearHistory() = dao.clearScreenings()

    companion object {
        @Volatile
        private var instance: ScreeningRepository? = null

        fun getInstance(dao: MentalHistoryDao): ScreeningRepository =
            instance ?: synchronized(this) {
                instance ?: ScreeningRepository(dao).also { instance = it }
            }
    }
}
