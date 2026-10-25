package com.mentalys.app.ml

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mentalys.app.data.local.entity.mental.ScreeningHistoryEntity
import com.mentalys.app.data.local.room.MainDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomHistoryInstrumentedTest {

    @Test
    fun screeningHistoryRoomStoresOnlyScreeningMetadata() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, MainDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val dao = db.mentalHistoryDao()
            val entity = ScreeningHistoryEntity(
                id = "instrumented-room-test",
                phqScore = 9,
                phqCategory = "Mild",
                faceRiskScore = null,
                audioRiskScore = 0.25f,
                riskFinal = 0.35f,
                riskCategory = "Mild",
                modalitiesUsed = "phq,voice",
                createdAt = "2026-05-16T00:00:00Z"
            )

            dao.insertScreening(entity)
            val recent = dao.getRecentScreenings(1)

            assertEquals(1, recent.size)
            assertEquals("instrumented-room-test", recent.first().id)
            assertEquals("phq,voice", recent.first().modalitiesUsed)
            assertNull(recent.first().faceRiskScore)
        } finally {
            db.close()
        }
    }
}
