package com.mentalys.app.ml.face

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object FacePreprocessor {

    private val detector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .build()
        FaceDetection.getClient(options)
    }

    private const val INPUT_SIZE = 224

    suspend fun preprocess(bitmap: Bitmap): FloatArray? {
        val faces = detectFaces(bitmap) ?: return null
        if (faces.isEmpty()) return null

        val face = faces[0]
        val box = face.boundingBox

        val left = box.left.coerceAtLeast(0)
        val top = box.top.coerceAtLeast(0)
        val right = box.right.coerceAtMost(bitmap.width)
        val bottom = box.bottom.coerceAtMost(bitmap.height)
        val width = (right - left).coerceAtLeast(1)
        val height = (bottom - top).coerceAtLeast(1)

        val cropped = Bitmap.createBitmap(bitmap, left, top, width, height)
        val resized = Bitmap.createScaledBitmap(cropped, INPUT_SIZE, INPUT_SIZE, true)

        return bitmapToFloatArray(resized)
    }

    private suspend fun detectFaces(bitmap: Bitmap) = suspendCancellableCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)
        detector.process(image)
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    private fun bitmapToFloatArray(bitmap: Bitmap): FloatArray {
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        val input = FloatArray(INPUT_SIZE * INPUT_SIZE * 3)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val red = ((pixel shr 16) and 0xFF).toFloat()
            val green = ((pixel shr 8) and 0xFF).toFloat()
            val blue = (pixel and 0xFF).toFloat()

            // VGG16 preprocess_input: RGB [0,255] -> BGR with ImageNet mean subtraction.
            input[i * 3 + 0] = blue - 103.939f
            input[i * 3 + 1] = green - 116.779f
            input[i * 3 + 2] = red - 123.68f
        }
        return input
    }
}
