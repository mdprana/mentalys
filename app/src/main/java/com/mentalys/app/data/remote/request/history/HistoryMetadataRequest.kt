package com.mentalys.app.data.remote.request.history

data class HistoryMetadataRequest(
    val type: String,
    val prediction: String,
    val confidence: Double? = null,
    val riskScore: Double? = null,
    val riskCategory: String? = null,
    val modalitiesUsed: List<String>? = null,
    val additionalPrompt: Map<String, Any?>? = null,
    val timestamp: String? = null
)
