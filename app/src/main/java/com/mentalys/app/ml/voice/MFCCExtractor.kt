package com.mentalys.app.ml.voice

import com.jlibrosa.audio.JLibrosa

/**
 * librosa-compatible MFCC + delta + delta-delta.
 *
 * Why this exists: the speech-emotion model was trained with librosa's default
 * MFCC pipeline:
 *
 *     librosa.feature.mfcc(y, sr=22050, n_mfcc=40, hop_length=512)
 *     librosa.feature.delta(mfcc)            # order=1, width=9 (default)
 *     librosa.feature.delta(mfcc, order=2)   # order=2, width=9 (default)
 *
 * Feeding the model anything else (TarsosDSP MFCC, different fmax, Hamming
 * instead of Hann, magnitude instead of power, etc.) collapses every prediction
 * onto whatever class dominated training. JLibrosa replicates librosa's MFCC
 * almost exactly; deltas are computed here with the same Savitzky-Golay
 * coefficients scipy.signal.savgol_filter uses inside librosa.feature.delta.
 */
object MFCCExtractor {

    const val SAMPLE_RATE = 22050
    const val DURATION_SEC = 3.0f
    const val OFFSET_SEC = 0.5f
    const val N_MFCC = 40
    const val MAX_LEN = 130
    const val HOP_LENGTH = 512
    const val N_FEATURES = 120
    private const val N_FFT = 2048
    private const val N_MELS = 128

    private val jLibrosa: JLibrosa by lazy {
        JLibrosa().apply { sampleRate = SAMPLE_RATE }
    }

    // Savitzky-Golay coefficients used by scipy.signal.savgol_filter
    // (which is exactly what librosa.feature.delta calls internally).
    // width=9, polyorder=order, deriv=order.
    private val SG_DERIV1 = floatArrayOf(
        -4f / 60f, -3f / 60f, -2f / 60f, -1f / 60f, 0f / 60f,
        1f / 60f, 2f / 60f, 3f / 60f, 4f / 60f
    )
    private val SG_DERIV2 = floatArrayOf(
        28f / 462f, 7f / 462f, -8f / 462f, -17f / 462f, -20f / 462f,
        -17f / 462f, -8f / 462f, 7f / 462f, 28f / 462f
    )

    fun extract(pcmSamples: FloatArray): FloatArray {
        // JLibrosa returns shape (n_mfcc, T) — same as librosa.
        val mfcc = jLibrosa.generateMFCCFeatures(
            pcmSamples, SAMPLE_RATE, N_MFCC, N_FFT, N_MELS, HOP_LENGTH
        )
        if (mfcc.isEmpty() || mfcc[0].isEmpty()) {
            return FloatArray(MAX_LEN * N_FEATURES)
        }
        val delta = sgFilter(mfcc, SG_DERIV1)
        val delta2 = sgFilter(mfcc, SG_DERIV2)
        return packToFixedLength(mfcc, delta, delta2)
    }

    /**
     * Apply a width-9 Savitzky-Golay filter along the time axis (axis=1).
     * Edges use 'nearest' index clamping — slightly different from librosa's
     * 'interp' mode, but it only affects the first/last 4 frames out of 130.
     */
    private fun sgFilter(matrix: Array<FloatArray>, coeffs: FloatArray): Array<FloatArray> {
        val nMfcc = matrix.size
        val tLen = matrix[0].size
        val half = coeffs.size / 2
        val out = Array(nMfcc) { FloatArray(tLen) }
        for (i in 0 until nMfcc) {
            val row = matrix[i]
            val outRow = out[i]
            for (t in 0 until tLen) {
                var sum = 0f
                for (k in coeffs.indices) {
                    val idx = (t + k - half).coerceIn(0, tLen - 1)
                    sum += coeffs[k] * row[idx]
                }
                outRow[t] = sum
            }
        }
        return out
    }

    /**
     * Stack mfcc / delta / delta-delta into a (MAX_LEN, N_FEATURES) flat layout
     * matching the training pipeline:
     *
     *     feat = concat([mfcc, delta, delta2], axis=0)  # (120, T)
     *     feat = feat.T                                  # (T, 120)
     *
     * Output is row-major flat: frame-major, then 120 features per frame.
     */
    private fun packToFixedLength(
        mfcc: Array<FloatArray>,
        delta: Array<FloatArray>,
        delta2: Array<FloatArray>,
    ): FloatArray {
        val tLen = mfcc[0].size
        val frameCount = tLen.coerceAtMost(MAX_LEN)
        val output = FloatArray(MAX_LEN * N_FEATURES)
        for (frame in 0 until frameCount) {
            val base = frame * N_FEATURES
            for (coef in 0 until N_MFCC) {
                output[base + coef] = mfcc[coef][frame]
                output[base + N_MFCC + coef] = delta[coef][frame]
                output[base + N_MFCC * 2 + coef] = delta2[coef][frame]
            }
        }
        return output
    }
}
