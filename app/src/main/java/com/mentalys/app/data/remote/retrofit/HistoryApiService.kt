package com.mentalys.app.data.remote.retrofit

import com.mentalys.app.data.remote.request.history.HistoryMetadataRequest
import com.mentalys.app.data.remote.response.history.HistoryListResponse
import com.mentalys.app.data.remote.response.history.SaveHistoryResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface HistoryApiService {

    @POST("ml/history")
    suspend fun saveHistory(
        @Header("Authorization") token: String,
        @Body request: HistoryMetadataRequest
    ): Response<SaveHistoryResponse>

    @GET("ml/history")
    suspend fun getHistory(
        @Header("Authorization") token: String,
        @Query("type") type: String,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("sortBy") sortBy: String? = null,
        @Query("sortOrder") sortOrder: String? = null,
    ): Response<HistoryListResponse>

    @GET("ml/all-history")
    suspend fun getAllHistory(
        @Header("Authorization") token: String,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("sortBy") sortBy: String? = null,
        @Query("sortOrder") sortOrder: String? = null,
    ): Response<HistoryListResponse>

    @DELETE("ml/history")
    suspend fun deleteAllHistory(
        @Header("Authorization") token: String
    ): Response<SaveHistoryResponse>
}
