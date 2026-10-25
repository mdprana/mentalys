package com.mentalys.app.ml.voice

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.roundToInt

object AudioPreprocessor {

    private data class ScalerParams(
        @SerializedName("mean_") val mean: List<Double>,
        @SerializedName("scale_") val scale: List<Double>
    )

    private var scalerParams: ScalerParams? = null

    fun loadScaler(context: Context) {
        if (scalerParams != null) return
        val json = context.assets.open("metadata/audio_scaler_params.json").bufferedReader().readText()
        scalerParams = Gson().fromJson(json, ScalerParams::class.java)
    }

    fun process(context: Context, wavFile: File): FloatArray {
        loadScaler(context)
        val samples = readWavSamples(wavFile)
        val trimmed = trimSamples(samples, offsetStartSamples = null)
        val mfcc = MFCCExtractor.extract(trimmed)
        return applyScaling(mfcc)
    }

    /**
     * Returns up to [maxWindows] feature tensors sampled from evenly-spaced offsets
     * across the recording. Used to average inference results so the final risk
     * score is continuous instead of binary 0/1 from a single confident window.
     */
    fun processWindows(context: Context, wavFile: File, maxWindows: Int = 5): List<FloatArray> {
        loadScaler(context)
        val samples = readWavSamples(wavFile)
        val durationSamples = (MFCCExtractor.DURATION_SEC * MFCCExtractor.SAMPLE_RATE).roundToInt()
        val totalSamples = samples.size

        if (totalSamples <= durationSamples) {
            val mfcc = MFCCExtractor.extract(samples)
            return listOf(applyScaling(mfcc))
        }

        val baseOffsetSamples = (MFCCExtractor.OFFSET_SEC * MFCCExtractor.SAMPLE_RATE).roundToInt()
        val firstStart = baseOffsetSamples.coerceAtMost(totalSamples - durationSamples)
        val lastStart = (totalSamples - durationSamples).coerceAtLeast(firstStart)
        val span = lastStart - firstStart
        val windowCount = maxWindows.coerceAtLeast(1)

        val starts = if (span <= 0 || windowCount == 1) {
            listOf(firstStart)
        } else {
            (0 until windowCount).map { i ->
                firstStart + (span.toLong() * i / (windowCount - 1)).toInt()
            }.distinct()
        }

        return starts.map { start ->
            val end = (start + durationSamples).coerceAtMost(totalSamples)
            val window = samples.copyOfRange(start, end)
            val mfcc = MFCCExtractor.extract(window)
            applyScaling(mfcc)
        }
    }

    private fun trimSamples(samples: FloatArray, offsetStartSamples: Int?): FloatArray {
        val offsetSamples = offsetStartSamples
            ?: (MFCCExtractor.OFFSET_SEC * MFCCExtractor.SAMPLE_RATE).roundToInt()
        val durationSamples = (MFCCExtractor.DURATION_SEC * MFCCExtractor.SAMPLE_RATE).roundToInt()
        val start = offsetSamples.coerceAtMost(samples.size)
        val end = (offsetSamples + durationSamples).coerceAtMost(samples.size)
        return if (end > start) samples.copyOfRange(start, end)
        else samples.copyOf(durationSamples.coerceAtMost(samples.size))
    }

    /**
     * Header-aware WAV loader. Parses RIFF/fmt chunks instead of assuming a
     * 44-byte header, downmixes to mono if needed, and linearly resamples to
     * [MFCCExtractor.SAMPLE_RATE] (22050 Hz). This lets the file-picker
     * accept RAVDESS clips (48 kHz) and any other 16-bit PCM WAV — not just
     * the app's own 22050 Hz recordings.
     */
    private fun readWavSamples(wavFile: File): FloatArray {
        val raf = RandomAccessFile(wavFile, "r")
        try {
            val riff = ByteArray(12)
            raf.readFully(riff)
            require(String(riff, 0, 4) == "RIFF" && String(riff, 8, 4) == "WAVE") {
                "Not a RIFF/WAVE file"
            }

            var sampleRate = MFCCExtractor.SAMPLE_RATE
            var channels = 1
            var bitsPerSample = 16
            var dataBytes: ByteArray? = null

            while (raf.filePointer < raf.length()) {
                val header = ByteArray(8)
                if (raf.read(header) < 8) break
                val id = String(header, 0, 4)
                val size = ((header[4].toInt() and 0xFF)) or
                    ((header[5].toInt() and 0xFF) shl 8) or
                    ((header[6].toInt() and 0xFF) shl 16) or
                    ((header[7].toInt() and 0xFF) shl 24)
                when (id) {
                    "fmt " -> {
                        val fmt = ByteArray(size)
                        raf.readFully(fmt)
                        channels = ((fmt[2].toInt() and 0xFF) or ((fmt[3].toInt() and 0xFF) shl 8))
                        sampleRate = (fmt[4].toInt() and 0xFF) or
                            ((fmt[5].toInt() and 0xFF) shl 8) or
                            ((fmt[6].toInt() and 0xFF) shl 16) or
                            ((fmt[7].toInt() and 0xFF) shl 24)
                        bitsPerSample = (fmt[14].toInt() and 0xFF) or
                            ((fmt[15].toInt() and 0xFF) shl 8)
                    }
                    "data" -> {
                        val bytes = ByteArray(size)
                        raf.readFully(bytes)
                        dataBytes = bytes
                    }
                    else -> raf.skipBytes(size)
                }
                if (size % 2 == 1 && raf.filePointer < raf.length()) raf.skipBytes(1)
                if (dataBytes != null) break
            }

            val bytes = dataBytes ?: return FloatArray(0)
            require(bitsPerSample == 16) { "Only 16-bit PCM WAV is supported (got $bitsPerSample)" }

            val bytesPerSample = 2
            val frameStride = bytesPerSample * channels
            val frameCount = bytes.size / frameStride
            val mono = FloatArray(frameCount) { f ->
                var sum = 0
                for (c in 0 until channels) {
                    val idx = f * frameStride + c * bytesPerSample
                    val low = bytes[idx].toInt() and 0xFF
                    val high = bytes[idx + 1].toInt()
                    sum += ((high shl 8) or low).toShort().toInt()
                }
                (sum.toFloat() / channels) / 32768f
            }

            return if (sampleRate == MFCCExtractor.SAMPLE_RATE) mono
            else linearResample(mono, sampleRate, MFCCExtractor.SAMPLE_RATE)
        } finally {
            raf.close()
        }
    }

    private fun linearResample(input: FloatArray, srcRate: Int, dstRate: Int): FloatArray {
        if (input.isEmpty() || srcRate == dstRate) return input
        val ratio = srcRate.toDouble() / dstRate.toDouble()
        val outLen = (input.size / ratio).toInt()
        val out = FloatArray(outLen)
        for (i in 0 until outLen) {
            val srcPos = i * ratio
            val i0 = srcPos.toInt()
            val i1 = (i0 + 1).coerceAtMost(input.size - 1)
            val frac = (srcPos - i0).toFloat()
            out[i] = input[i0] * (1f - frac) + input[i1] * frac
        }
        return out
    }

    private fun applyScaling(features: FloatArray): FloatArray {
        val params = scalerParams ?: return features
        return FloatArray(features.size) { i ->
            val featureIndex = i % MFCCExtractor.N_FEATURES
            val mean = params.mean.getOrElse(featureIndex) { 0.0 }.toFloat()
            val scale = params.scale.getOrElse(featureIndex) { 1.0 }.toFloat()
            (features[i] - mean) / scale
        }
    }
}
