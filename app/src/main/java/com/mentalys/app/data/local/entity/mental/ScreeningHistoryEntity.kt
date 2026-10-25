package com.mentalys.app.data.local.entity.mental

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "screening_history")
data class ScreeningHistoryEntity(
    @PrimaryKey val id: String,
    val phqScore: Int?,          // null if PHQ-9 not completed
    val phqCategory: String?,    // null if PHQ-9 not completed
    val faceRiskScore: Float?,   // null if face analysis not completed
    val audioRiskScore: Float?,  // null if voice analysis not completed
    val riskFinal: Float,
    val riskCategory: String,
    val modalitiesUsed: String,  // comma-separated: "phq", "face", "voice"
    val createdAt: String
)
