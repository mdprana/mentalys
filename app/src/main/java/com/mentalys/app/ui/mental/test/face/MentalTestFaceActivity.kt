package com.mentalys.app.ui.mental.test.face

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.mentalys.app.R
import com.mentalys.app.databinding.ActivityMentalTestFaceBinding
import com.mentalys.app.ml.face.FaceInterpreter
import com.mentalys.app.ml.face.FacePreprocessor
import com.mentalys.app.ml.fusion.RiskMapper
import com.mentalys.app.ui.activities.CameraActivity
import com.mentalys.app.ui.activities.CameraActivity.Companion.CAMERAX_RESULT
import com.mentalys.app.utils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MentalTestFaceActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FACE_RISK = "extra_face_risk"
    }

    private lateinit var binding: ActivityMentalTestFaceBinding
    private var currentImageUri: Uri? = null
    private var faceInterpreter: FaceInterpreter? = null

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == CAMERAX_RESULT) {
            result.data?.getStringExtra(CameraActivity.EXTRA_CAMERAX_IMAGE)?.toUri()
                ?.let { uri ->
                    currentImageUri = uri
                    showImage(uri)
                }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMentalTestFaceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        faceInterpreter = FaceInterpreter(this)

        binding.backButton.setOnClickListener { finish() }
        binding.cameraButton.setOnClickListener {
            cameraLauncher.launch(Intent(this, CameraActivity::class.java))
        }
        binding.faceImgPreview.setOnClickListener {
            cameraLauncher.launch(Intent(this, CameraActivity::class.java))
        }
        binding.analyseButton.setOnClickListener { analyseImage() }
    }

    private fun showImage(uri: Uri) {
        binding.faceImgIcon.visibility = View.GONE
        binding.faceImgPreview.setImageURI(uri)
    }

    private fun analyseImage() {
        val uri = currentImageUri ?: run {
            showToast(this, getString(R.string.alert_empty_image))
            return
        }

        showLoading(true)
        val totalStartNanos = SystemClock.elapsedRealtimeNanos()

        lifecycleScope.launch {
            try {
                var decodeMs = 0.0
                val bitmap = withContext(Dispatchers.IO) {
                    val start = SystemClock.elapsedRealtimeNanos()
                    val decoded = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                    decodeMs = elapsedMs(start)
                    decoded
                } ?: throw Exception("Could not load image")

                val preprocessStartNanos = SystemClock.elapsedRealtimeNanos()
                val input = withContext(Dispatchers.Default) {
                    FacePreprocessor.preprocess(bitmap)
                }
                val preprocessMs = elapsedMs(preprocessStartNanos)

                if (input == null) {
                    showLoading(false)
                    Log.i(
                        "FaceTiming",
                        "decode_ms=${decodeMs.round1()} preprocessing_ms=${preprocessMs.round1()} status=no_face_detected"
                    )
                    showToast(this@MentalTestFaceActivity, getString(R.string.no_face_detected))
                    return@launch
                }

                val inferenceStartNanos = SystemClock.elapsedRealtimeNanos()
                val probs = withContext(Dispatchers.Default) {
                    faceInterpreter!!.run(input)
                }
                val inferenceMs = elapsedMs(inferenceStartNanos)
                val totalMs = elapsedMs(totalStartNanos)
                val faceRisk = RiskMapper.computeFaceRisk(probs)

                Log.i(
                    "PerfEvidence",
                    "VGG-16+SE+Attention Wajah manual | decode_ms=${decodeMs.round1()} | preprocessing_ms=${preprocessMs.round1()} | inference_ms=${inferenceMs.round1()} | total_ms=${totalMs.round1()} | risk=$faceRisk"
                )
                Log.i(
                    "FaceTiming",
                    "decode_ms=${decodeMs.round1()} preprocessing_ms=${preprocessMs.round1()} inference_ms=${inferenceMs.round1()} total_ms=${totalMs.round1()} risk=$faceRisk"
                )
                Log.d(
                    "FaceTest",
                    "probs=${FaceInterpreter.LABELS.zip(probs.toList()).joinToString { "${it.first}=${"%.3f".format(it.second)}" }} risk=$faceRisk"
                )

                setResult(RESULT_OK, Intent().putExtra(EXTRA_FACE_RISK, faceRisk))
                finish()
            } catch (e: Exception) {
                showLoading(false)
                Log.e("FaceTest", "Error: ${e.message}", e)
                showToast(this@MentalTestFaceActivity, "Analysis failed: ${e.message}")
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.faceTestLoadingLayout.root.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.faceTestLayout.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun elapsedMs(startNanos: Long): Double =
        (SystemClock.elapsedRealtimeNanos() - startNanos) / 1_000_000.0

    private fun Double.round1(): Double =
        kotlin.math.round(this * 10.0) / 10.0

    override fun onDestroy() {
        super.onDestroy()
        faceInterpreter?.close()
    }
}
