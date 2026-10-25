package com.mentalys.app.ml

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mentalys.app.ml.face.FaceInterpreter
import com.mentalys.app.ml.voice.VoiceInterpreter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TfliteRuntimeSmokeInstrumentedTest {

    @Test
    fun voiceModel_loadsWithFlexDelegateAndReturnsEightFiniteValues() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val interpreter = VoiceInterpreter(context)

        try {
            val output = interpreter.run(FloatArray(130 * 120) { 0f })

            assertEquals(VoiceInterpreter.LABELS.toList(), outputLabelsAudio)
            assertEquals(8, output.size)
            assertTrue(output.all { it.isFinite() })
            assertFalse(output.any { it.isNaN() })
        } finally {
            interpreter.close()
        }
    }

    @Test
    fun faceModel_loadsWithStandardInterpreterAndReturnsSevenFiniteValues() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val interpreter = FaceInterpreter(context)

        try {
            val output = interpreter.run(FloatArray(224 * 224 * 3) { 0f })

            assertEquals(FaceInterpreter.LABELS.toList(), outputLabelsFace)
            assertEquals(7, output.size)
            assertTrue(output.all { it.isFinite() })
            assertFalse(output.any { it.isNaN() })
        } finally {
            interpreter.close()
        }
    }

    private val outputLabelsAudio = listOf(
        "angry", "calm", "disgust", "fearful", "happy", "neutral", "sad", "surprised"
    )

    private val outputLabelsFace = listOf(
        "angry", "disgust", "fear", "happy", "neutral", "sad", "surprise"
    )
}
