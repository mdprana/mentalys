package com.mentalys.app.ui.mental.test

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.telephony.TelephonyManager
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.mentalys.app.R
import com.mentalys.app.data.local.entity.mental.ScreeningHistoryEntity
import com.mentalys.app.data.remote.request.history.HistoryMetadataRequest
import com.mentalys.app.databinding.ActivityFusionResultBinding
import com.mentalys.app.di.Injection
import com.mentalys.app.ui.activities.MainActivity
import com.mentalys.app.ui.chat.AiChatActivity
import com.mentalys.app.ui.specialist.SpecialistActivity
import com.mentalys.app.utils.AuthTokenProvider
import com.mentalys.app.utils.Resource
import com.mentalys.app.utils.SettingsPreferences
import com.mentalys.app.utils.TextSanitizer
import com.mentalys.app.utils.dataStore
import io.noties.markwon.Markwon
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class FusionResultActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_HISTORY_ID     = "extra_history_id"
        private const val EXTRA_CREATED_AT     = "extra_created_at"
        private const val EXTRA_PHQ_SCORE     = "extra_phq_score"
        private const val EXTRA_PHQ_CATEGORY  = "extra_phq_category"
        private const val EXTRA_FACE_RISK     = "extra_face_risk"
        private const val EXTRA_AUDIO_RISK    = "extra_audio_risk"
        private const val EXTRA_FINAL_RISK    = "extra_final_risk"
        private const val EXTRA_RISK_CATEGORY = "extra_risk_category"
        private const val EXTRA_MODALITIES    = "extra_modalities"

        fun newIntent(
            context: Context,
            historyId: String,
            createdAt: String,
            phqScore: Int,
            phqCategory: String,
            faceRisk: Float,
            audioRisk: Float,
            finalRisk: Float,
            riskCategory: String,
            modalitiesUsed: String
        ): Intent = Intent(context, FusionResultActivity::class.java).apply {
            putExtra(EXTRA_HISTORY_ID, historyId)
            putExtra(EXTRA_CREATED_AT, createdAt)
            putExtra(EXTRA_PHQ_SCORE, phqScore)
            putExtra(EXTRA_PHQ_CATEGORY, phqCategory)
            putExtra(EXTRA_FACE_RISK, faceRisk)
            putExtra(EXTRA_AUDIO_RISK, audioRisk)
            putExtra(EXTRA_FINAL_RISK, finalRisk)
            putExtra(EXTRA_RISK_CATEGORY, riskCategory)
            putExtra(EXTRA_MODALITIES, modalitiesUsed)
        }
    }

    private lateinit var binding: ActivityFusionResultBinding
    private val viewModel: ScreeningResultViewModel by viewModels()
    private lateinit var markwon: Markwon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityFusionResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        markwon = Markwon.create(this)

        val phqScore     = intent.getIntExtra(EXTRA_PHQ_SCORE, -1)
        val phqCategory  = intent.getStringExtra(EXTRA_PHQ_CATEGORY).orEmpty()
        val faceRisk     = intent.getFloatExtra(EXTRA_FACE_RISK, -1f)
        val audioRisk    = intent.getFloatExtra(EXTRA_AUDIO_RISK, -1f)
        val finalRisk    = intent.getFloatExtra(EXTRA_FINAL_RISK, 0f)
        val riskCategory = intent.getStringExtra(EXTRA_RISK_CATEGORY).orEmpty()
        val modalities   = intent.getStringExtra(EXTRA_MODALITIES).orEmpty()
        val historyId    = intent.getStringExtra(EXTRA_HISTORY_ID) ?: UUID.randomUUID().toString()
        val createdAt    = intent.getStringExtra(EXTRA_CREATED_AT) ?: utcNow()

        bindUI(phqScore, phqCategory, faceRisk, audioRisk, finalRisk, riskCategory, modalities)
        ensureHistorySaved(
            historyId,
            createdAt,
            phqScore,
            phqCategory,
            faceRisk,
            audioRisk,
            finalRisk,
            riskCategory,
            modalities
        )

        lifecycleScope.launch {
            val appLanguage = SettingsPreferences.getInstance(dataStore)
                .getLanguageSetting()
                .firstOrNull()
                ?: "English"

            viewModel.generateExplanation(
                phqScore       = phqScore,
                phqCategory    = phqCategory,
                faceRisk       = faceRisk,
                audioRisk      = audioRisk,
                finalRisk      = finalRisk,
                riskCategory   = riskCategory,
                modalitiesUsed = modalities,
                language       = appLanguage
            )
        }

        viewModel.explanation.observe(this) { resource ->
            when (resource) {
                null -> {
                    binding.progressExplanation.visibility = View.GONE
                    binding.tvExplanation.visibility = View.GONE
                    binding.btnRetryExplanation.visibility = View.GONE
                }
                is Resource.Loading -> {
                    binding.progressExplanation.visibility = View.VISIBLE
                    binding.tvExplanation.visibility = View.GONE
                    binding.btnRetryExplanation.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressExplanation.visibility = View.GONE
                    binding.btnRetryExplanation.visibility = View.GONE
                    binding.tvExplanation.visibility = View.VISIBLE
                    // Sanitize Unicode math chars BEFORE Markwon renders the Markdown
                    val clean = TextSanitizer.sanitizeGemini(resource.data ?: "")
                    markwon.setMarkdown(binding.tvExplanation, clean)
                }
                is Resource.Error -> {
                    binding.progressExplanation.visibility = View.GONE
                    binding.tvExplanation.visibility = View.VISIBLE
                    binding.btnRetryExplanation.visibility = View.VISIBLE
                    binding.tvExplanation.text = getString(R.string.ai_explanation_error)
                }
            }
        }

        // Retry AI explanation
        binding.btnRetryExplanation.setOnClickListener {
            viewModel.retryExplanation()
        }

        binding.closeButton.setOnClickListener { finish() }

        binding.btnHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            })
            finish()
        }

        // FIX: Navigate to SpecialistActivity (was finish())
        binding.btnConsult.setOnClickListener {
            startActivity(Intent(this, SpecialistActivity::class.java))
        }

        // Ask AI — opens AiChatActivity with screening context pre-loaded
        binding.btnAskAi.setOnClickListener {
            val intent = Intent(this, AiChatActivity::class.java).apply {
                putExtra(AiChatActivity.EXTRA_RISK_CATEGORY, riskCategory)
                putExtra(AiChatActivity.EXTRA_FINAL_RISK, finalRisk)
                putExtra(AiChatActivity.EXTRA_MODALITIES, modalities)
                putExtra(AiChatActivity.EXTRA_INCLUDE_HISTORY, true)
            }
            startActivity(intent)
        }
    }

    private fun bindUI(
        phqScore: Int, phqCategory: String,
        faceRisk: Float, audioRisk: Float,
        finalRisk: Float, riskCategory: String,
        modalities: String
    ) {
        binding.cardRiskBanner.setCardBackgroundColor(riskBannerColor(riskCategory))
        binding.tvRiskCategory.text = localizeRiskCategory(riskCategory)
        binding.tvFinalRiskScore.text = getString(R.string.risk_score_fmt).format(finalRisk)
        binding.tvModalitiesUsed.text = getString(R.string.modalities_used_label, formatModalities(modalities))

        val na = getString(R.string.not_available)
        binding.tvPhqScore.text  = if (phqScore != -1) "$phqScore/27 ($phqCategory)" else na
        binding.tvFaceScore.text  = if (faceRisk != -1f) "%.2f".format(faceRisk) else na
        binding.tvAudioScore.text = if (audioRisk != -1f) "%.2f".format(audioRisk) else na

        val isSevere   = riskCategory.contains("Severe") || riskCategory.contains("High")
        val isModerate = riskCategory.contains("Moderate")

        val recommendation = when {
            isSevere   -> getString(R.string.recommendation_high, crisisHotlineHtml())
            isModerate -> getString(R.string.recommendation_moderate)
            else       -> getString(R.string.recommendation_low)
        }
        binding.tvRecommendation.text = HtmlCompat.fromHtml(
            recommendation,
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )

        if (isModerate || isSevere) {
            binding.btnConsult.visibility = View.VISIBLE
        }
    }

    private fun ensureHistorySaved(
        historyId: String,
        createdAt: String,
        phqScore: Int,
        phqCategory: String,
        faceRisk: Float,
        audioRisk: Float,
        finalRisk: Float,
        riskCategory: String,
        modalities: String
    ) {
        val entity = ScreeningHistoryEntity(
            id = historyId,
            phqScore = phqScore.takeIf { it != -1 },
            phqCategory = phqCategory.takeIf { it.isNotBlank() },
            faceRiskScore = faceRisk.takeIf { it != -1f },
            audioRiskScore = audioRisk.takeIf { it != -1f },
            riskFinal = finalRisk,
            riskCategory = riskCategory,
            modalitiesUsed = modalities,
            createdAt = createdAt
        )

        lifecycleScope.launch {
            val historyRepository = Injection.provideHistoryRepository(this@FusionResultActivity)
            historyRepository.saveLocalScreening(entity)

            val token = AuthTokenProvider.getFreshToken(this@FusionResultActivity)
            if (token.isNotBlank()) {
                historyRepository.saveRemoteHistorySilently(token, entity.toHistoryMetadataRequest())
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

    private fun utcNow(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .also { it.timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())

    private fun formatModalities(raw: String): String =
        raw.split(",").mapNotNull { tag ->
            when (tag.trim()) {
                "phq"   -> "PHQ-9"
                "face"  -> getString(R.string.face_label)
                "voice" -> getString(R.string.voice_label)
                else    -> null
            }
        }.joinToString(", ")

    private fun localizeRiskCategory(raw: String): String = when (raw) {
        "Minimal"           -> getString(R.string.risk_minimal)
        "Mild"              -> getString(R.string.risk_mild)
        "Moderately Severe" -> getString(R.string.risk_moderately_severe)
        "Moderate"          -> getString(R.string.risk_moderate)
        "High"              -> getString(R.string.risk_high)
        "Severe"            -> getString(R.string.risk_severe)
        else                -> getString(R.string.risk_high_override)
    }

    private fun riskBannerColor(category: String): Int = when {
        category.contains("Minimal")           -> Color.parseColor("#4CAF50")
        category.contains("Mild")              -> Color.parseColor("#8BC34A")
        category.contains("Moderately Severe") -> Color.parseColor("#FF5722")
        category.contains("Moderate")          -> Color.parseColor("#FF9800")
        category.contains("Severe") || category.contains("High") -> Color.parseColor("#F44336")
        else                                   -> Color.parseColor("#9E9E9E")
    }

    private fun crisisHotlineHtml(): String {
        val countryCode = detectCountryCode()
        val hotline = countryHotlines[countryCode]
        val text = if (hotline != null) {
            "${hotline.label}: ${hotline.number}"
        } else {
            getString(R.string.crisis_hotline_fallback)
        }
        return "<b>$text</b>"
    }

    private fun detectCountryCode(): String {
        val telephonyManager = getSystemService(TELEPHONY_SERVICE) as? TelephonyManager
        val networkCountry = telephonyManager
            ?.networkCountryIso
            ?.takeIf { it.isNotBlank() }
            ?.uppercase(Locale.US)
        return networkCountry ?: "ID"
    }

    private data class HotlineInfo(
        val label: String,
        val number: String
    )

    private val countryHotlines: Map<String, HotlineInfo> by lazy {
        mapOf(
            "" to HotlineInfo("Hotline Krisis Indonesia", "119"),
            "ID" to HotlineInfo("Hotline Krisis Indonesia", "119"),
            "US" to HotlineInfo("988 Suicide & Crisis Lifeline", "988"),
            "CA" to HotlineInfo("Suicide Crisis Helpline", "988"),
            "GB" to HotlineInfo("Samaritans", "116 123"),
            "IE" to HotlineInfo("Samaritans", "116 123"),
            "AU" to HotlineInfo("Lifeline Australia", "13 11 14"),
            "NZ" to HotlineInfo("Need to Talk?", "1737"),
            "SG" to HotlineInfo("Samaritans of Singapore", "1767"),
            "MY" to HotlineInfo("Talian HEAL", "15555"),
            "IN" to HotlineInfo("KIRAN Mental Health Helpline", "1800-599-0019"),
            "PH" to HotlineInfo("NCMH Crisis Hotline", "1553"),
            "BR" to HotlineInfo("CVV", "188"),
            "JP" to HotlineInfo("Yorisoi Hotline", "0120-279-338"),
            "KR" to HotlineInfo("Mental Health Crisis Counseling", "1577-0199")
        )
    }
}
