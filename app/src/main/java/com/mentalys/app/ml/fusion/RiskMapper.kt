package com.mentalys.app.ml.fusion

import kotlin.math.pow

object RiskMapper {

    /**
     * Temperature-soften an over-confident softmax distribution so the
     * P(sad) + 0.5*P(neutral) - P(happy) formula does not collapse to a
     * binary 0.00 / 1.00 outcome. Higher T = softer distribution.
     */
    private fun softenProbs(probs: FloatArray, temperature: Float): FloatArray {
        if (probs.isEmpty() || temperature <= 0f) return probs
        val invT = 1.0 / temperature
        val raised = DoubleArray(probs.size) { i ->
            (probs[i].coerceAtLeast(0f).toDouble()).pow(invT)
        }
        val sum = raised.sum()
        if (sum <= 0.0) return probs
        return FloatArray(probs.size) { i -> (raised[i] / sum).toFloat() }
    }

    // Face model order: [angry(0), disgust(1), fear(2), happy(3), neutral(4), sad(5), surprise(6)]
    fun computeFaceRisk(probs: FloatArray): Float {
        val p = softenProbs(probs, temperature = 1.5f)
        return (
            p.getOrElse(5) { 0f } +             // sad
                (0.5f * p.getOrElse(4) { 0f }) - // neutral / flat affect
                p.getOrElse(3) { 0f }            // happy / protective affect
            ).coerceIn(0f, 1f)
    }

    // Audio class order from label_map.json: [angry(0), calm(1), disgust(2), fearful(3), happy(4), neutral(5), sad(6), surprised(7)]
    // RAVDESS "calm" is the "neutral relaxed" recording style — acoustically adjacent
    // to "neutral" rather than its own affect. Casual user speech lands on `calm`, so
    // we weight it as a softer neutral (0.25) so flat affect still contributes to risk.
    // Voice model is highly confident -> apply stronger softening (T=2.5) to spread
    // probabilities and prevent the risk score from saturating at 0.00 or 1.00.
    fun computeAudioRisk(probs: FloatArray): Float {
        val p = softenProbs(probs, temperature = 2.5f)
        return (
            p.getOrElse(6) { 0f } +              // sad
                (0.5f * p.getOrElse(5) { 0f }) + // neutral / flat affect
                (0.25f * p.getOrElse(1) { 0f }) - // calm / RAVDESS "neutral relaxed"
                p.getOrElse(4) { 0f }            // happy / protective affect
            ).coerceIn(0f, 1f)
    }

    fun normalizePHQ(rawScore: Int): Float = rawScore / 27f

    fun phqCategory(rawScore: Int): String = when {
        rawScore <= 4 -> "Minimal"
        rawScore <= 9 -> "Mild"
        rawScore <= 14 -> "Moderate"
        rawScore <= 19 -> "Moderately Severe"
        else -> "Severe"
    }
}
