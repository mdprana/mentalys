package com.mentalys.app.ui.chat

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.mentalys.app.BuildConfig
import com.mentalys.app.R
import com.mentalys.app.databinding.ActivityAiChatBinding
import com.mentalys.app.di.Injection
import com.mentalys.app.utils.TextSanitizer
import com.mentalys.app.utils.SettingsPreferences
import com.mentalys.app.utils.RiskLanguage
import com.mentalys.app.utils.dataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class AiChatActivity : AppCompatActivity() {

    companion object {
        /** Optional extras to pre-load screening context into the chat session. */
        const val EXTRA_RISK_CATEGORY = "extra_risk_category"
        const val EXTRA_FINAL_RISK    = "extra_final_risk"
        const val EXTRA_MODALITIES    = "extra_modalities"
        const val EXTRA_INCLUDE_HISTORY = "extra_include_history"
    }

    private lateinit var binding: ActivityAiChatBinding
    private lateinit var adapter: ChatAdapter

    private val model by lazy {
        GenerativeModel(modelName = "gemini-2.5-flash", apiKey = BuildConfig.GEMINI_API_KEY)
    }

    private val chat by lazy {
        val history = buildChatHistory()
        model.startChat(history = history)
    }

    private var historyContext: String = ""
    private var responseLanguage: String = "English"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAiChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right,
                maxOf(systemBars.bottom, ime.bottom))
            insets
        }

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = ChatAdapter()
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter

        binding.btnSend.isEnabled = false
        lifecycleScope.launch {
            responseLanguage = SettingsPreferences.getInstance(dataStore)
                .getLanguageSetting()
                .firstOrNull()
                ?: "English"

            historyContext = if (intent.getBooleanExtra(EXTRA_INCLUDE_HISTORY, false)) {
                buildRecentHistoryContext()
            } else {
                ""
            }

            val riskCategory = intent.getStringExtra(EXTRA_RISK_CATEGORY)
            val welcomeMessage = if (riskCategory != null) {
                buildContextualWelcome(riskCategory)
            } else if (historyContext.isNotBlank()) {
                getString(R.string.ai_chat_welcome_with_history)
            } else {
                getString(R.string.ai_chat_welcome)
            }
            adapter.addWelcome(welcomeMessage)
            binding.btnSend.isEnabled = true
        }

        binding.btnSend.setOnClickListener { sendMessage() }
        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { sendMessage(); true } else false
        }
    }

    /**
     * Builds the chat history with a system-role context message.
     * If screening extras are present, they are included as safe metadata
     * (no raw images/audio — only risk category and modalities used).
     */
    private fun buildChatHistory() = buildList {
        val riskCategory = intent.getStringExtra(EXTRA_RISK_CATEGORY)
        val finalRisk    = intent.getFloatExtra(EXTRA_FINAL_RISK, -1f)
        val modalities   = intent.getStringExtra(EXTRA_MODALITIES)
        val displayRiskCategory = riskCategory?.let { RiskLanguage.displayRisk(it, responseLanguage) }
        val displayModalities = RiskLanguage.displayModalities(modalities, responseLanguage)

        val systemPrompt = buildString {
            if (responseLanguage != "Bahasa Indonesia") {
                append("From now on, you are Mentalys AI, an empathetic mental-health assistant. ")
                append("Answer only in English. Provide emotional support, psychoeducation, and self-care suggestions. ")
                append("Use warm, non-judgmental, empowering language. ")
                append("Never say 'you have depression'; use phrases like 'your screening result suggests...' or 'you may be experiencing...'. ")
                append("Always remind the user that you are not a substitute for professional care when the situation is serious.\n\n")

                if (riskCategory != null) {
                    append("=== PRIVATE USER SCREENING CONTEXT ===\n")
                    append("Initial screening risk level: $displayRiskCategory")
                    if (finalRisk >= 0) append(" (score: ${"%.2f".format(finalRisk)})")
                    if (displayModalities.isNotBlank()) append("\nModalities used: $displayModalities")
                    append("\nUse this context to respond more personally. Do not mention the numeric score directly to the user.")
                }

                if (historyContext.isNotBlank()) {
                    append("\n\n=== USER'S RECENT MENTAL TEST HISTORY ===\n")
                    append(historyContext)
                    append("\nUse these trends to understand the user's current feelings. ")
                    append("You may mention broad patterns like 'recently your results seem to...', but do not diagnose or scare the user.")
                }
                return@buildString
            }

            append("Mulai sekarang kamu berperan sebagai Mentalys AI, asisten kesehatan mental yang empatik. ")
            append("Jawab hanya dalam ${languageInstruction()}. ")
            append("Berikan dukungan emosional, psikoedukasi, dan saran perawatan diri. ")
            append("Gunakan bahasa yang hangat, tidak menghakimi, dan memberdayakan. ")
            append("Jangan pernah mengatakan 'kamu menderita depresi' — gunakan frasa seperti ")
            append("'hasil skrining menunjukkan...' atau 'kamu mungkin sedang mengalami...'. ")
            append("Selalu ingatkan bahwa kamu bukan pengganti profesional jika kondisi serius.\n\n")

            if (riskCategory != null) {
                append("=== KONTEKS SKRINING PENGGUNA (RAHASIA — JANGAN diungkapkan ke pengguna) ===\n")
                append("Hasil skrining awal menunjukkan tingkat risiko: $displayRiskCategory")
                if (finalRisk >= 0) append(" (skor: ${"%.2f".format(finalRisk)})")
                if (displayModalities.isNotBlank()) append("\nModalitas yang digunakan: $displayModalities")
                append("\n\nGunakan konteks ini untuk memberikan respons yang lebih relevan dan personal. ")
                append("Jangan menyebutkan angka skor secara langsung kepada pengguna.")
            }

            if (historyContext.isNotBlank()) {
                append("\n\n=== RIWAYAT TES MENTAL TERBARU PENGGUNA ===\n")
                append(historyContext)
                append("\nGunakan tren ini untuk memahami kondisi pengguna saat ini. ")
                append("Boleh menyebutkan pola umum seperti 'akhir-akhir ini hasil Anda cenderung...', ")
                append("tetapi jangan membuat diagnosis dan jangan menakut-nakuti pengguna.")
            }
        }

        add(content("user") { text(systemPrompt) })
        add(content("model") {
            text(
                if (responseLanguage == "Bahasa Indonesia") {
                    "Baik, saya siap sebagai Mentalys AI untuk memberikan dukungan kesehatan mental yang empatik. " +
                        "Saya akan menggunakan bahasa yang hangat dan mengingatkan pengguna untuk konsultasi profesional bila diperlukan."
                } else {
                    "Understood. I am ready as Mentalys AI to provide empathetic mental-health support. " +
                        "I will use warm language and remind the user to seek professional care when needed."
                }
            )
        })
    }

    private fun buildContextualWelcome(riskCategory: String): String {
        if (responseLanguage != "Bahasa Indonesia") {
            val tone = when {
                RiskLanguage.isHighRisk(riskCategory) ->
                    "I can see your screening result points to something that deserves careful attention. I am here to listen and support you."
                RiskLanguage.isModerateRisk(riskCategory) ->
                    "Thank you for taking a brave step to understand your condition more clearly."
                else ->
                    "It is good to see you taking care of your mental health."
            }
            return "Hi! I am Mentalys AI. $tone What would you like to share or ask today?"
        }

        val tone = when {
            RiskLanguage.isHighRisk(riskCategory) ->
                "Saya melihat hasil skrining Anda menunjukkan beberapa hal yang perlu diperhatikan. Saya di sini untuk mendengarkan dan mendukung Anda."
            RiskLanguage.isModerateRisk(riskCategory) ->
                "Terima kasih sudah mengambil langkah berani untuk memahami kondisi Anda lebih baik."
            else ->
                "Senang melihat Anda merawat kesehatan mental Anda."
        }
        return "Halo! Saya Mentalys AI. $tone Apa yang ingin Anda ceritakan atau tanyakan hari ini?"
    }

    private fun languageInstruction(): String =
        if (responseLanguage == "Bahasa Indonesia") "Bahasa Indonesia" else "English"

    private suspend fun buildRecentHistoryContext(): String {
        val recent = Injection.provideHistoryRepository(this)
            .getRecentLocalHistory(limit = 5)

        if (recent.isEmpty()) return ""

        return recent.joinToString(separator = "\n") { item ->
            val risk = RiskLanguage.displayRisk(item.riskCategory, responseLanguage)
            val phqCategory = RiskLanguage.displayPhqCategory(item.phqCategory, responseLanguage)
            val modalities = RiskLanguage.displayModalities(item.modalitiesUsed, responseLanguage)

            if (responseLanguage == "Bahasa Indonesia") {
                val phq = item.phqScore?.let { "PHQ-9 $it/27 ($phqCategory)" }
                    ?: "PHQ-9 belum dilakukan"
                val face = item.faceRiskScore?.let { "wajah ${"%.2f".format(it)}" } ?: "wajah belum dilakukan"
                val audio = item.audioRiskScore?.let { "suara ${"%.2f".format(it)}" } ?: "suara belum dilakukan"
                "- ${item.createdAt}: tingkat risiko $risk, skor akhir ${"%.2f".format(item.riskFinal)}, $phq, $face, $audio, modalitas $modalities"
            } else {
                val phq = item.phqScore?.let { "PHQ-9 $it/27 (${item.phqCategory ?: "unknown"})" }
                    ?: "PHQ-9 not completed"
                val face = item.faceRiskScore?.let { "face ${"%.2f".format(it)}" } ?: "face not completed"
                val audio = item.audioRiskScore?.let { "voice ${"%.2f".format(it)}" } ?: "voice not completed"
                "- ${item.createdAt}: $risk, final score ${"%.2f".format(item.riskFinal)}, $phq, $face, $audio, modalities $modalities"
            }
        }
    }

    private fun sendMessage() {
        if (!binding.btnSend.isEnabled) return

        val text = binding.etMessage.text?.toString()?.trim() ?: return
        if (text.isEmpty()) return

        binding.etMessage.text?.clear()
        binding.btnSend.isEnabled = false

        adapter.addUser(text)
        scrollToBottom()

        val loadingIndex = adapter.addAiLoading()
        scrollToBottom()

        lifecycleScope.launch {
            // Retry transient failures so the chat does not silently fail on a single
            // network/rate-limit hiccup.
            val backoffs = listOf(0L, 800L, 1800L)
            var success: String? = null
            for (delayMs in backoffs) {
                if (delayMs > 0) delay(delayMs)
                try {
                    val response = chat.sendMessage(text)
                    val raw = response.text
                    if (!raw.isNullOrBlank()) {
                        success = TextSanitizer.sanitizeGemini(raw)
                        break
                    }
                } catch (_: Exception) {
                    // ignore and retry
                }
            }
            adapter.finishAiLoading(
                loadingIndex,
                success ?: getString(R.string.ai_explanation_error)
            )
            binding.btnSend.isEnabled = true
            scrollToBottom()
        }
    }

    private fun scrollToBottom() {
        binding.rvMessages.postDelayed({
            if (adapter.itemCount > 0) {
                binding.rvMessages.smoothScrollToPosition(adapter.itemCount - 1)
            }
        }, 100)
    }
}
