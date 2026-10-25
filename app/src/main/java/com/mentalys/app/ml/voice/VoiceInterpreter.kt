package com.mentalys.app.ml.voice

import android.content.Context
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.flex.FlexDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class VoiceInterpreter(context: Context) {

    companion object {
        private const val MODEL_FILE = "models/audio_ser.tflite"
        private const val MAX_LEN = 130
        private const val NUM_FEATURES = 120
        private const val NUM_CLASSES = 8
        val LABELS = arrayOf("angry", "calm", "disgust", "fearful", "happy", "neutral", "sad", "surprised")
    }

    private val flexDelegate = FlexDelegate()
    private val interpreter: Interpreter
    private val outputIs1D: Boolean

    init {
        val assetFd = context.assets.openFd(MODEL_FILE)
        val fileChannel = FileInputStream(assetFd.fileDescriptor).channel
        val buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, assetFd.startOffset, assetFd.declaredLength)
        val options = Interpreter.Options().apply {
            addDelegate(flexDelegate)
            setNumThreads(4)
        }
        interpreter = Interpreter(buffer, options)

        val inputShape = interpreter.getInputTensor(0).shape()
        if (inputShape.size < 3 || inputShape[inputShape.size - 2] != MAX_LEN || inputShape.last() != NUM_FEATURES) {
            error("Voice model input shape mismatch: expected (*, $MAX_LEN, $NUM_FEATURES), got ${inputShape.contentToString()}")
        }
        val outputShape = interpreter.getOutputTensor(0).shape()
        outputIs1D = outputShape.size == 1
    }

    fun run(features: FloatArray): FloatArray {
        val expectedSize = MAX_LEN * NUM_FEATURES
        if (features.size != expectedSize) {
            error("Voice feature size mismatch: expected $expectedSize floats, got ${features.size}")
        }
        val inputBuffer = ByteBuffer.allocateDirect(features.size * 4)
            .order(ByteOrder.nativeOrder())
        features.forEach { inputBuffer.putFloat(it) }
        inputBuffer.rewind()

        return if (outputIs1D) {
            val output = FloatArray(NUM_CLASSES)
            interpreter.run(inputBuffer, output)
            output
        } else {
            val output = Array(1) { FloatArray(NUM_CLASSES) }
            interpreter.run(inputBuffer, output)
            output[0]
        }
    }

    fun close() {
        interpreter.close()
        flexDelegate.close()
    }
}
