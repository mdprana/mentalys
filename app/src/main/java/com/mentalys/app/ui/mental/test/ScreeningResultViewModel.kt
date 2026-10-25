package com.mentalys.app.ui.mental.test

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.mentalys.app.BuildConfig
import com.mentalys.app.utils.Resource
import com.mentalys.app.utils.RiskLanguage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ScreeningResultViewModel : ViewModel() {

    // Primary stable model. If a request fails, we retry on the same model and then
    // fall back to a lighter/faster sibling so the AI interpretation still runs.
    private val primaryModel by lazy {
        GenerativeModel(modelName = "gemini-2.5-flash", apiKey = BuildConfig.GEMINI_API_KEY)
    }
    private val fallbackModel by lazy {
        GenerativeModel(modelName = "gemini-2.5-flash-lite", apiKey = BuildConfig.GEMINI_API_KEY)
    }

    private val _explanation = MutableLiveData<Resource<String>?>()
    val explanation: LiveData<Resource<String>?> = _explanation

    // Store last params so retry can re-use them
    private var lastParams: ExplanationParams? = null

    private data class ExplanationParams(
        val phqScore: Int, val phqCategory: String,
        val faceRisk: Float, val audioRisk: Float,
        val finalRisk: Float, val riskCategory: String,
        val modalitiesUsed: String,
        val language: String
    )

    fun generateExplanation(
        phqScore: Int, phqCategory: String,
        faceRisk: Float, audioRisk: Float,
        finalRisk: Float, riskCategory: String,
        modalitiesUsed: String,
        language: String
    ) {
        if (_explanation.value != null) return  // don't re-trigger on config change
        lastParams = ExplanationParams(phqScore, phqCategory, faceRisk, audioRisk, finalRisk, riskCategory, modalitiesUsed, language)
        requestExplanation(lastParams!!)
    }

    /** Clears the current explanation and re-fetches from Gemini. */
    fun retryExplanation() {
        val params = lastParams ?: return
        _explanation.value = null
        requestExplanation(params)
    }

    private fun requestExplanation(p: ExplanationParams) {
        _explanation.value = Resource.Loading
        viewModelScope.launch {
            val prompt = buildPrompt(
                p.phqScore, p.phqCategory, p.faceRisk, p.audioRisk,
                p.finalRisk, p.riskCategory, p.modalitiesUsed, p.language
            )

            // Retry primary model up to 3 times with backoff, then fall back to a
            // lighter model. This significantly reduces "AI unavailable" cases caused
            // by transient network errors or short rate-limit windows.
            val attempts = listOf(
                primaryModel to 0L,
                primaryModel to 800L,
                primaryModel to 1800L,
                fallbackModel to 0L,
                fallbackModel to 1500L,
            )

            var lastError: Exception? = null
            for ((model, backoffMs) in attempts) {
                if (backoffMs > 0) delay(backoffMs)
                try {
                    val response = model.generateContent(prompt)
                    val text = response.text
                    if (!text.isNullOrBlank()) {
                        _explanation.postValue(Resource.Success(text))
                        return@launch
                    }
                } catch (e: Exception) {
                    lastError = e
                    Log.w("ScreeningResultVM", "Gemini attempt failed: ${e.message}")
                }
            }

            Log.e("ScreeningResultVM", "All Gemini attempts failed", lastError)
            _explanation.postValue(Resource.Error("AI interpretation unavailable at this time."))
        }
    }

    private fun buildPrompt(
        phqScore: Int, phqCategory: String,
        faceRisk: Float, audioRisk: Float,
        finalRisk: Float, riskCategory: String,
        modalitiesUsed: String,
        language: String
    ): String = buildString {
        val displayRiskCategory = RiskLanguage.displayRisk(riskCategory, language)
        val displayPhqCategory = RiskLanguage.displayPhqCategory(phqCategory, language)
        val displayModalities = RiskLanguage.displayModalities(modalitiesUsed, language)

        if (language != "Bahasa Indonesia") {
            append("You are a mental-health psychoeducation assistant for the Mentalys app. ")
            append("Write like a calm doctor or psychologist explaining a screening result in humane, professional English. ")
            append("Do not diagnose depression. Use phrases like 'your screening result suggests' and 'this should be understood as an early signal'.\n\n")

            append("=== SCREENING RESULT DATA ===\n")
            append("Risk Level: $displayRiskCategory (Combined Score: ${"%.2f".format(finalRisk)}, range 0-1)\n")
            if (phqScore != -1) append("- PHQ-9 Mood Questionnaire: $phqScore/27 – $displayPhqCategory\n")
            if (faceRisk != -1f) append("- Facial Expression Analysis: ${"%.2f".format(faceRisk)} (0 = no indication, 1 = strong indication)\n")
            if (audioRisk != -1f) append("- Voice Tone Analysis: ${"%.2f".format(audioRisk)}\n")
            append("Modalities used: $displayModalities\n\n")

            append("=== OUTPUT INSTRUCTIONS ===\n")
            append("Use simple Markdown, no emoji, no decorative symbols, and no excessive technical terms.\n")
            append("Use this exact structure:\n\n")
            append("## Result Summary\n")
            append("Write one substantial paragraph explaining what the $displayRiskCategory category and ${"%.2f".format(finalRisk)} combined score mean. Explain that a higher score means a stronger risk signal, but this remains an initial screening, not a diagnosis.\n\n")
            append("## What This May Mean Right Now\n")
            append("Write one empathetic paragraph connecting the result to daily experiences such as energy, sleep, focus, motivation, emotions, or withdrawal. Validate the user without making certain claims.\n\n")
            append("## Professional Recommendation\n")
            append("Write one long paragraph like a doctor's recommendation: explain what to do in the next 24 hours, over the next week, and when to seek professional support. Match the intensity to the risk category. Include social support, sleep routine, meals, light movement, journaling, and reducing isolation when relevant.\n\n")
            append("## Practical Steps Today\n")
            append("Give 4 specific realistic bullet points the user can do today.\n\n")
            append("## When to Seek Immediate Help\n")
            append("Write one paragraph. ")
            if (RiskLanguage.isHighRisk(riskCategory)) {
                append("Because the result indicates high risk, advise the user to contact a mental-health professional, emergency service, or local crisis hotline if they feel unsafe, have thoughts of self-harm, or cannot manage basic daily activities. ")
            } else {
                append("Explain that immediate help is needed if symptoms worsen, thoughts of self-harm appear, or the user feels unsafe. ")
            }
            append("Emphasize that seeking help is a strong and reasonable action.\n\n")
            append("## Important Disclaimer\n")
            append("Write this sentence: This result is an AI-based initial screening and does not replace diagnosis or advice from a licensed mental-health professional. For an accurate assessment, please consult a psychologist, psychiatrist, or medical professional.\n\n")
            append("End with one sincere, hopeful sentence.")
            return@buildString
        }

        append("Anda adalah asisten psikoedukasi kesehatan mental untuk aplikasi Mentalys. ")
        append("Tulis seperti dokter atau psikolog yang menjelaskan hasil skrining dengan bahasa manusiawi, ")
        append("tenang, profesional, dan tidak menghakimi. Jangan mendiagnosis depresi. ")
        append("Gunakan frasa seperti 'hasil skrining menunjukkan indikasi' dan 'ini perlu dipahami sebagai sinyal awal'.\n\n")

        // Result data
        append("=== DATA HASIL SKRINING ===\n")
        append("Tingkat Risiko: $displayRiskCategory (Skor Gabungan: ${"%.2f".format(finalRisk)}, rentang 0-1)\n")
        if (phqScore != -1) append("- PHQ-9 (Kuesioner Mood): $phqScore/27 – $displayPhqCategory\n")
        if (faceRisk != -1f) append("- Analisis Ekspresi Wajah: ${"%.2f".format(faceRisk)} (0 = tidak ada indikasi, 1 = indikasi kuat)\n")
        if (audioRisk != -1f) append("- Analisis Nada Suara: ${"%.2f".format(audioRisk)}\n")
        append("Modalitas yang digunakan: $displayModalities\n\n")

        append("=== INSTRUKSI FORMAT OUTPUT ===\n")
        append("Tulis dalam Bahasa Indonesia. Gunakan Markdown sederhana, tanpa emoji, tanpa karakter dekoratif, dan tanpa istilah teknis berlebihan.\n")
        append("Gunakan struktur berikut:\n\n")

        append("## Ringkasan Hasil\n")
        append("Tulis 1 paragraf panjang yang menjelaskan arti kategori $displayRiskCategory dan skor gabungan ${"%.2f".format(finalRisk)}. ")
        append("Jelaskan bahwa skor lebih tinggi berarti sinyal risiko lebih kuat, tetapi hasil ini tetap skrining awal, bukan diagnosis.\n\n")

        append("## Makna untuk Kondisi Anda Saat Ini\n")
        append("Tulis 1 paragraf empatik yang menghubungkan hasil dengan kemungkinan pengalaman sehari-hari seperti energi, tidur, fokus, motivasi, emosi, atau menarik diri. ")
        append("Validasi perasaan pengguna tanpa membuat klaim pasti.\n\n")

        append("## Rekomendasi Profesional\n")
        append("Tulis 1 paragraf panjang seperti rekomendasi dokter: jelaskan langkah yang sebaiknya dilakukan dalam 24 jam, 1 minggu, dan kapan perlu bantuan profesional. ")
        append("Sesuaikan intensitas saran dengan kategori risiko. Sertakan dukungan sosial, rutinitas tidur, makan, aktivitas ringan, journaling, dan mengurangi isolasi bila relevan.\n\n")

        append("## Langkah Praktis Hari Ini\n")
        append("Berikan 4 bullet point yang spesifik, realistis, dan bisa dilakukan hari ini.\n\n")

        append("## Kapan Harus Mencari Bantuan Segera\n")
        append("Tulis 1 paragraf. ")
        if (RiskLanguage.isHighRisk(riskCategory)) {
            append("Karena hasil menunjukkan risiko tinggi, anjurkan pengguna menghubungi profesional kesehatan mental atau layanan darurat/hotline setempat jika merasa tidak aman, punya pikiran menyakiti diri, atau tidak bisa menjalani aktivitas dasar. ")
        } else {
            append("Jelaskan bahwa bantuan segera dibutuhkan jika gejala memburuk, muncul pikiran menyakiti diri, atau pengguna merasa tidak aman. ")
        }
        append("Tekankan bahwa mencari bantuan adalah tindakan yang kuat dan masuk akal.\n\n")

        append("## Disclaimer Penting\n")
        append("Tuliskan kalimat ini: ")
        append("Hasil ini adalah skrining awal berbasis AI dan tidak menggantikan diagnosis atau saran dari profesional kesehatan mental berlisensi. ")
        append("Untuk penilaian yang akurat, silakan berkonsultasi dengan psikolog, psikiater, atau tenaga medis profesional.\n\n")

        append("Akhiri dengan satu kalimat penyemangat yang tulus dan hangat.")
    }
}
