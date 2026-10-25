package com.mentalys.app.ml.fusion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskFusionEngineTest {

    @Test
    fun fuseAdaptive_allModalities_usesConfiguredWeights() {
        val actual = RiskFusionEngine.fuseAdaptive(
            phqNorm = 0.50f,
            audioRisk = 0.40f,
            faceRisk = 0.30f
        )

        assertEquals(0.425f, actual, 0.0001f)
    }

    @Test
    fun fuseAdaptive_phqAndAudio_renormalizesAvailableWeights() {
        val actual = RiskFusionEngine.fuseAdaptive(
            phqNorm = 0.60f,
            audioRisk = 0.50f,
            faceRisk = null
        )

        val expected = (0.45f * 0.60f + 0.35f * 0.50f) / (0.45f + 0.35f)
        assertEquals(expected, actual, 0.0001f)
    }

    @Test
    fun fuseAdaptive_phqAndFace_renormalizesAvailableWeights() {
        val actual = RiskFusionEngine.fuseAdaptive(
            phqNorm = 0.40f,
            audioRisk = null,
            faceRisk = 0.50f
        )

        val expected = (0.45f * 0.40f + 0.20f * 0.50f) / (0.45f + 0.20f)
        assertEquals(expected, actual, 0.0001f)
    }

    @Test
    fun fuseAdaptive_phqOnly_returnsPhqNorm() {
        val actual = RiskFusionEngine.fuseAdaptive(
            phqNorm = 0.22f,
            audioRisk = null,
            faceRisk = null
        )

        assertEquals(0.22f, actual, 0.0001f)
    }

    @Test
    fun fuseAdaptive_audioAndFaceWithoutPhq_usesSpecialVoiceFaceFormula() {
        val actual = RiskFusionEngine.fuseAdaptive(
            phqNorm = null,
            audioRisk = 0.50f,
            faceRisk = 0.40f
        )

        assertEquals(0.445f, actual, 0.0001f)
    }

    @Test
    fun isSafetyOverride_returnsTrueWhenQ9AnswerPositiveAndPhqWasCompleted() {
        assertTrue(RiskFusionEngine.isSafetyOverride(phqRawScore = 12, q9Answer = 1))
    }

    @Test
    fun isSafetyOverride_returnsTrueWhenPhqScoreSevere() {
        assertTrue(RiskFusionEngine.isSafetyOverride(phqRawScore = 20, q9Answer = 0))
    }

    @Test
    fun isSafetyOverride_returnsFalseWhenPhqWasNotCompleted() {
        assertFalse(RiskFusionEngine.isSafetyOverride(phqRawScore = null, q9Answer = 1))
    }

    @Test
    fun categorize_respectsDocumentedBoundaries() {
        assertEquals("Minimal", RiskFusionEngine.categorize(0.20f))
        assertEquals("Mild", RiskFusionEngine.categorize(0.21f))
        assertEquals("Mild", RiskFusionEngine.categorize(0.40f))
        assertEquals("Moderate", RiskFusionEngine.categorize(0.41f))
        assertEquals("Moderate", RiskFusionEngine.categorize(0.60f))
        assertEquals("High", RiskFusionEngine.categorize(0.61f))
        assertEquals("High", RiskFusionEngine.categorize(0.80f))
        assertEquals("Severe", RiskFusionEngine.categorize(0.81f))
    }
}
