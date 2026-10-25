package com.mentalys.app.ml.fusion

object RiskFusionEngine {

    // Full three-modality fusion (original weights)
    fun fuse(phqNorm: Float, audioRisk: Float, faceRisk: Float): Float =
        (0.45f * phqNorm + 0.35f * audioRisk + 0.20f * faceRisk).coerceIn(0f, 1f)

    // Adaptive fusion keeps the thesis formula when the available modalities match it.
    fun fuseAdaptive(phqNorm: Float?, audioRisk: Float?, faceRisk: Float?): Float {
        if (phqNorm == null && audioRisk != null && faceRisk != null) {
            return (0.55f * faceRisk + 0.45f * audioRisk).coerceIn(0f, 1f)
        }

        val wPhq   = if (phqNorm   != null) 0.45f else 0f
        val wAudio = if (audioRisk != null) 0.35f else 0f
        val wFace  = if (faceRisk  != null) 0.20f else 0f
        val total  = wPhq + wAudio + wFace
        if (total == 0f) return 0f
        return (
            (phqNorm   ?: 0f) * wPhq   +
            (audioRisk ?: 0f) * wAudio +
            (faceRisk  ?: 0f) * wFace
        ) / total
    }

    fun categorize(score: Float): String = when {
        score <= 0.20f -> "Minimal"
        score <= 0.40f -> "Mild"
        score <= 0.60f -> "Moderate"
        score <= 0.80f -> "High"
        else           -> "Severe"
    }

    // Only triggers when PHQ is actually done (phqRawScore != null)
    fun isSafetyOverride(phqRawScore: Int?, q9Answer: Int): Boolean =
        phqRawScore != null && (phqRawScore >= 20 || q9Answer > 0)
}
