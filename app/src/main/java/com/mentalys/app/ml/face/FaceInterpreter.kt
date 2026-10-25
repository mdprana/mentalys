package com.mentalys.app.ml.face

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class FaceInterpreter(context: Context) {

    companion object {
        private const val MODEL_FILE = "models/fer_vgg16_attention_best.tflite"
        private const val INPUT_SIZE = 224
        private const val NUM_CLASSES = 7
        val LABELS = arrayOf("angry", "disgust", "fear", "happy", "neutral", "sad", "surprise")
    }

    private val interpreter: Interpreter

    init {
        val assetFd = context.assets.openFd(MODEL_FILE)
        val fileChannel = FileInputStream(assetFd.fileDescriptor).channel
        val buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, assetFd.startOffset, assetFd.declaredLength)
        interpreter = Interpreter(buffer)

        val inputShape = interpreter.getInputTensor(0).shape()
        val expectedShape = intArrayOf(1, INPUT_SIZE, INPUT_SIZE, 3)
        if (!inputShape.contentEquals(expectedShape)) {
            error("Face model input shape mismatch: expected ${expectedShape.contentToString()}, got ${inputShape.contentToString()}")
        }
    }

    fun run(input: FloatArray): FloatArray {
        val inputBuffer = ByteBuffer.allocateDirect(input.size * 4)
            .order(ByteOrder.nativeOrder())
        input.forEach { inputBuffer.putFloat(it) }
        inputBuffer.rewind()

        val output = Array(1) { FloatArray(NUM_CLASSES) }
        interpreter.run(inputBuffer, output)
        return output[0]
    }

    fun close() = interpreter.close()
}
