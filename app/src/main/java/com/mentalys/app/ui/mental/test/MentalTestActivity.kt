package com.mentalys.app.ui.mental.test

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.mentalys.app.R
import com.mentalys.app.data.local.entity.mental.ScreeningHistoryEntity
import com.mentalys.app.data.remote.request.history.HistoryMetadataRequest
import com.mentalys.app.databinding.ActivityMentalTestBinding
import com.mentalys.app.di.Injection
import com.mentalys.app.ml.fusion.RiskFusionEngine
import com.mentalys.app.ui.mental.test.face.MentalTestFaceActivity
import com.mentalys.app.ui.mental.test.voice.MentalTestVoiceActivity
import com.mentalys.app.utils.AuthTokenProvider
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class MentalTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMentalTestBinding
    private val sessionViewModel: ScreeningSessionViewModel by viewModels()

    private val phqLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            sessionViewModel.phqRawScore = data.getIntExtra(ScreeningSessionActivity.RESULT_PHQ_RAW, 0)
            sessionViewModel.phqNorm     = data.getFloatExtra(ScreeningSessionActivity.RESULT_PHQ_NORM, 0f)
            sessionViewModel.phqCategory = data.getStringExtra(ScreeningSessionActivity.RESULT_PHQ_CATEGORY) ?: ""
            sessionViewModel.q9Answer    = data.getIntExtra(ScreeningSessionActivity.RESULT_Q9_ANSWER, 0)
            updateUI()
        }
    }

    private val faceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            sessionViewModel.faceRiskScore =
                result.data?.getFloatExtra(MentalTestFaceActivity.EXTRA_FACE_RISK, 0f) ?: 0f
            updateUI()
        }
    }

    private val voiceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            sessionViewModel.audioRiskScore =
                result.data?.getFloatExtra(MentalTestVoiceActivity.EXTRA_AUDIO_RISK, 0f) ?: 0f
            updateUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMentalTestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.backButton.setOnClickListener { finish() }

        binding.btnStartPhq.setOnClickListener {
            phqLauncher.launch(Intent(this, ScreeningSessionActivity::class.java))
        }
        binding.btnStartFace.setOnClickListener {
            faceLauncher.launch(Intent(this, MentalTestFaceActivity::class.java))
        }
        binding.btnStartVoice.setOnClickListener {
            voiceLauncher.launch(Intent(this, MentalTestVoiceActivity::class.java))
        }
        binding.btnGetResults.setOnClickListener { generateResults() }

        updateUI()
    }

    private fun updateUI() {
        updateCard(binding.chipPhqStatus, binding.tvPhqResult, binding.btnStartPhq,
            sessionViewModel.isPhqDone(),
            sessionViewModel.phqRawScore?.let {
                getString(R.string.phq_result_fmt, it, sessionViewModel.phqCategory)
            })

        updateCard(binding.chipFaceStatus, binding.tvFaceResult, binding.btnStartFace,
            sessionViewModel.isFaceDone(),
            sessionViewModel.faceRiskScore?.let { getString(R.string.risk_score_fmt, it) })

        updateCard(binding.chipVoiceStatus, binding.tvVoiceResult, binding.btnStartVoice,
            sessionViewModel.isVoiceDone(),
            sessionViewModel.audioRiskScore?.let { getString(R.string.risk_score_fmt, it) })

        binding.btnGetResults.isEnabled = true
    }

    private fun updateCard(
        statusChip: TextView,
        resultText: TextView,
        startBtn: MaterialButton,
        isDone: Boolean,
        resultLabel: String?
    ) {
        if (isDone) {
            statusChip.text = getString(R.string.status_done)
            statusChip.background.mutate().setTint(Color.parseColor("#4CAF50"))
            resultText.text = resultLabel
            resultText.visibility = View.VISIBLE
            startBtn.text = getString(R.string.btn_redo)
        } else {
            statusChip.text = getString(R.string.status_pending)
            statusChip.background.mutate().setTint(Color.parseColor("#9E9E9E"))
            resultText.visibility = View.GONE
            startBtn.text = getString(R.string.btn_start)
        }
    }

    private fun generateResults() {
        val vm = sessionViewModel

        if (!vm.isAnyDone()) {
            com.mentalys.app.utils.showToast(this, getString(R.string.screening_pending_error))
            return
        }

        val safetyOverride = RiskFusionEngine.isSafetyOverride(vm.phqRawScore, vm.q9Answer)

        val finalScore = if (safetyOverride) 1f
        else RiskFusionEngine.fuseAdaptive(vm.phqNorm, vm.audioRiskScore, vm.faceRiskScore)

        val riskCategory = if (safetyOverride) "High Risk – Safety Override"
        else RiskFusionEngine.categorize(finalScore)

        val ts = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .also { it.timeZone = TimeZone.getTimeZone("UTC") }.format(Date())

        val entity = ScreeningHistoryEntity(
            id             = UUID.randomUUID().toString(),
            phqScore       = vm.phqRawScore,
            phqCategory    = vm.phqCategory,
            faceRiskScore  = vm.faceRiskScore,
            audioRiskScore = vm.audioRiskScore,
            riskFinal      = finalScore,
            riskCategory   = riskCategory,
            modalitiesUsed = vm.modalitiesUsed(),
            createdAt      = ts
        )

        lifecycleScope.launch {
            val historyRepository = Injection.provideHistoryRepository(this@MentalTestActivity)
            historyRepository.saveLocalScreening(entity)

            startActivity(
                FusionResultActivity.newIntent(
                    context        = this@MentalTestActivity,
                    historyId      = entity.id,
                    createdAt      = entity.createdAt,
                    phqScore       = vm.phqRawScore ?: -1,
                    phqCategory    = vm.phqCategory ?: "",
                    faceRisk       = vm.faceRiskScore ?: -1f,
                    audioRisk      = vm.audioRiskScore ?: -1f,
                    finalRisk      = finalScore,
                    riskCategory   = riskCategory,
                    modalitiesUsed = entity.modalitiesUsed
                )
            )

            val token = AuthTokenProvider.getFreshToken(this@MentalTestActivity)
            if (token.isNotBlank()) {
                historyRepository.saveRemoteHistorySilently(
                    token,
                    entity.toHistoryMetadataRequest()
                )
            }
        }
    }

    private fun ScreeningHistoryEntity.toHistoryMetadataRequest(): HistoryMetadataRequest {
        val modalities = modalitiesUsed.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val type = when {
            modalities.size > 1 -> "multimodal_requests"
            modalities.firstOrNull() == "voice" -> "audio_requests"
            modalities.firstOrNull() == "face" -> "face_requests"
            else -> "quiz_requests"
        }

        return HistoryMetadataRequest(
            type = type,
            prediction = riskCategory,
            confidence = null,
            riskScore = riskFinal.toDouble(),
            riskCategory = riskCategory,
            modalitiesUsed = modalities.map { if (it == "voice") "audio" else it },
            additionalPrompt = mapOf(
                "phqScore" to phqScore,
                "phqCategory" to phqCategory,
                "audioScore" to audioRiskScore,
                "faceScore" to faceRiskScore,
                "finalScore" to riskFinal
            ),
            timestamp = createdAt
        )
    }
}
