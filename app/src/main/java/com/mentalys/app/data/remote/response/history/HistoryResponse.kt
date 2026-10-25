package com.mentalys.app.data.remote.response.history

data class SaveHistoryResponse(
    val status: String? = null,
    val data: HistoryItemResponse? = null,
    val message: String? = null
)

data class HistoryListResponse(
    val status: String? = null,
    val history: List<HistoryItemResponse>? = null,
    val total: Int? = null,
    val page: Int? = null,
    val limit: Int? = null,
    val totalPages: Int? = null,
    val message: String? = null
)

data class HistoryItemResponse(
    val id: String? = null,
    val type: String? = null,
    val prediction: HistoryPredictionResponse? = null,
    val inputData: Map<String, Any?>? = null,
    val timestamp: String? = null,
    val metadata: Map<String, Any?>? = null
)

data class HistoryPredictionResponse(
    val result: Any? = null
)
