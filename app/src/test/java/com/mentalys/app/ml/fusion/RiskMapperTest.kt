package com.mentalys.app.ml.fusion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskMapperTest {

    @Test
    fun computeAudioRisk_clipsNegativeRiskToZeroWhenHappyDominates() {
        val probs = floatArrayOf(
            0f, // angry
            0f, // calm
            0f, // disgust
            0f, // fearful
            1f, // happy
            0f, // neutral
            0f, // sad
            0f  // surprised
        )

        assertEquals(0f, RiskMapper.computeAudioRisk(probs), 0.0001f)
    }

    @Test
    fun computeFaceRisk_clipsUpperRiskToOneWhenSadDominates() {
        val probs = floatArrayOf(
            0f, // angry
            0f, // disgust
            0f, // fear
            0f, // happy
            0f, // neutral
            1f, // sad
            0f  // surprise
        )

        assertEquals(1f, RiskMapper.computeFaceRisk(probs), 0.0001f)
    }

    @Test
    fun computeFaceRisk_clipsNegativeRiskToZeroWhenHappyDominates() {
        val probs = floatArrayOf(
            0f, // angry
            0f, // disgust
            0f, // fear
            1f, // happy
            0f, // neutral
            0f, // sad
            0f  // surprise
        )

        assertEquals(0f, RiskMapper.computeFaceRisk(probs), 0.0001f)
    }

    @Test
    fun computeAudioRisk_returnsPositiveRiskWhenSadDominates() {
        val probs = floatArrayOf(
            0.02f,
            0.02f,
            0.02f,
            0.02f,
            0.01f,
            0.03f,
            0.86f,
            0.02f
        )

        assertTrue(RiskMapper.computeAudioRisk(probs) > 0f)
    }

    @Test
    fun normalizePHQ_mapsMaxScoreToOne() {
        assertEquals(1f, RiskMapper.normalizePHQ(27), 0.0001f)
    }
}
