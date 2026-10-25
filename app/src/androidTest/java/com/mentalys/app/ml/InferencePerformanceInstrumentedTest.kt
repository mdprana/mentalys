package com.mentalys.app.ml

import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mentalys.app.ml.face.FaceInterpreter
import com.mentalys.app.ml.voice.AudioPreprocessor
import com.mentalys.app.ml.voice.VoiceInterpreter
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.roundToLong
import kotlin.math.sin

@RunWith(AndroidJUnit4::class)
class InferencePerformanceInstrumentedTest {

    @Test
    fun measureDummyTensorInferenceTenRunsPerModel() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val device = "${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE}"

        val voice = VoiceInterpreter(context)
        val face = FaceInterpreter(context)
        val wavFile = createSyntheticWav(File(context.cacheDir, "perf_synthetic_22050.wav"))

        try {
            val voiceInput = FloatArray(130 * 120) { 0f }
            val faceInput = FloatArray(224 * 224 * 3) { 0f }

            val audioPreprocessTimes = measureRuns(10) {
                AudioPreprocessor.processWindows(context, wavFile, maxWindows = 5)
            }
            val voiceTimes = measureRuns(10) { voice.run(voiceInput) }
            val faceTimes = measureRuns(10) { face.run(faceInput) }
            val audioTotalTimes = measureRuns(10) {
                val windows = AudioPreprocessor.processWindows(context, wavFile, maxWindows = 5)
                windows.forEach { voice.run(it) }
            }

            Log.i(
                "PerfEvidence",
                format(
                    model = "CNN-BiLSTM Audio",
                    device = device,
                    preprocess = audioPreprocessTimes,
                    inference = voiceTimes,
                    total = audioTotalTimes
                )
            )
            Log.i(
                "PerfEvidence",
                format(
                    model = "VGG-16+SE+Attn",
                    device = device,
                    preprocess = null,
                    inference = faceTimes,
                    total = faceTimes
                )
            )

            assertTrue(voiceTimes.average() < 3000.0)
            assertTrue(faceTimes.average() < 3000.0)
        } finally {
            voice.close()
            face.close()
            wavFile.delete()
        }
    }

    private fun measureRuns(count: Int, block: () -> Unit): List<Double> =
        (1..count).map {
            val start = SystemClock.elapsedRealtimeNanos()
            block()
            val end = SystemClock.elapsedRealtimeNanos()
            (end - start) / 1_000_000.0
        }

    private fun format(
        model: String,
        device: String,
        preprocess: List<Double>?,
        inference: List<Double>,
        total: List<Double>
    ): String {
        return buildString {
            append("$model | device=$device | runs=${inference.size}")
            append(" | preprocessing=${preprocess?.stats() ?: "not_measured"}")
            append(" | inference=${inference.stats()}")
            append(" | total=${total.stats()}")
        }
    }

    private fun createSyntheticWav(file: File): File {
        val sampleRate = 22_050
        val seconds = 4
        val samples = sampleRate * seconds
        val dataSize = samples * 2
        file.outputStream().use { out ->
            fun writeAscii(value: String) = out.write(value.toByteArray(Charsets.US_ASCII))
            fun writeIntLE(value: Int) {
                out.write(value and 0xFF)
                out.write((value shr 8) and 0xFF)
                out.write((value shr 16) and 0xFF)
                out.write((value shr 24) and 0xFF)
            }
            fun writeShortLE(value: Int) {
                out.write(value and 0xFF)
                out.write((value shr 8) and 0xFF)
            }

            writeAscii("RIFF")
            writeIntLE(36 + dataSize)
            writeAscii("WAVE")
            writeAscii("fmt ")
            writeIntLE(16)
            writeShortLE(1)
            writeShortLE(1)
            writeIntLE(sampleRate)
            writeIntLE(sampleRate * 2)
            writeShortLE(2)
            writeShortLE(16)
            writeAscii("data")
            writeIntLE(dataSize)

            for (i in 0 until samples) {
                val value = (sin(2.0 * Math.PI * 440.0 * i / sampleRate) * 10_000).toInt()
                writeShortLE(value)
            }
        }
        return file
    }

    private fun List<Double>.stats(): String =
        "avg_ms=${average().round1()},min_ms=${(minOrNull() ?: 0.0).round1()},max_ms=${(maxOrNull() ?: 0.0).round1()}"

    private fun Double.round1(): Double = (this * 10.0).roundToLong() / 10.0
}
