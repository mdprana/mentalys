package com.mentalys.app.utils

object RiskLanguage {

    fun isIndonesian(language: String): Boolean =
        language.equals("Bahasa Indonesia", ignoreCase = true)

    fun displayRisk(raw: String, language: String): String {
        if (!isIndonesian(language)) return raw
        return when {
            raw.contains("Safety Override", ignoreCase = true) -> "Risiko Tinggi - Safety Override"
            raw.equals("Minimal", ignoreCase = true) -> "Minimal"
            raw.equals("Mild", ignoreCase = true) -> "Ringan"
            raw.equals("Moderate", ignoreCase = true) -> "Menengah"
            raw.equals("Moderately Severe", ignoreCase = true) -> "Cukup Parah"
            raw.equals("High", ignoreCase = true) -> "Tinggi"
            raw.equals("Severe", ignoreCase = true) -> "Parah"
            else -> raw
        }
    }

    fun displayPhqCategory(raw: String?, language: String): String {
        val value = raw?.takeIf { it.isNotBlank() } ?: return ""
        if (!isIndonesian(language)) return value
        return when {
            value.equals("Minimal", ignoreCase = true) -> "Minimal"
            value.equals("Mild", ignoreCase = true) -> "Ringan"
            value.equals("Moderate", ignoreCase = true) -> "Menengah"
            value.equals("Moderately Severe", ignoreCase = true) -> "Cukup Parah"
            value.equals("Severe", ignoreCase = true) -> "Parah"
            else -> value
        }
    }

    fun displayModalities(raw: String?, language: String): String {
        val tags = raw.orEmpty()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return tags.joinToString(", ") { tag ->
            when (tag) {
                "phq" -> "PHQ-9"
                "face" -> if (isIndonesian(language)) "wajah" else "face"
                "voice", "audio" -> if (isIndonesian(language)) "suara" else "voice"
                else -> tag
            }
        }
    }

    fun isHighRisk(raw: String): Boolean =
        raw.contains("Severe", ignoreCase = true) ||
            raw.contains("High", ignoreCase = true) ||
            raw.contains("Parah", ignoreCase = true) ||
            raw.contains("Tinggi", ignoreCase = true)

    fun isModerateRisk(raw: String): Boolean =
        raw.contains("Moderate", ignoreCase = true) ||
            raw.contains("Menengah", ignoreCase = true)
}
