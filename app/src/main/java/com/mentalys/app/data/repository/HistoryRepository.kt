package com.mentalys.app.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.mentalys.app.data.local.entity.mental.ScreeningHistoryEntity
import com.mentalys.app.data.local.room.MentalHistoryDao
import com.mentalys.app.data.remote.request.history.HistoryMetadataRequest
import com.mentalys.app.data.remote.response.history.HistoryListResponse
import com.mentalys.app.data.remote.response.history.SaveHistoryResponse
import com.mentalys.app.data.remote.retrofit.HistoryApiService
import com.mentalys.app.utils.Resource
import com.mentalys.app.utils.getErrorMessage
import java.net.SocketTimeoutException

class HistoryRepository private constructor(
    private val apiService: HistoryApiService,
    private val dao: MentalHistoryDao,
) {

    suspend fun saveLocalScreening(entity: ScreeningHistoryEntity) = dao.insertScreening(entity)

    fun getLocalHistory(): LiveData<List<ScreeningHistoryEntity>> = dao.getAllScreenings()

    suspend fun getRecentLocalHistory(limit: Int): List<ScreeningHistoryEntity> =
        dao.getRecentScreenings(limit)

    suspend fun clearLocalHistory() = dao.clearScreenings()

    fun saveRemoteHistory(
        token: String,
        request: HistoryMetadataRequest,
    ): LiveData<Resource<SaveHistoryResponse>> = liveData {
        emit(Resource.Loading)
        try {
            val response = apiService.saveHistory(token.asBearerToken(), request)
            if (response.isSuccessful) {
                response.body()?.let { emit(Resource.Success(it)) } ?: emit(Resource.InvalidResponse())
            } else {
                emit(response.toResourceError())
            }
        } catch (e: SocketTimeoutException) {
            emit(Resource.NetworkTimeout())
        } catch (e: Exception) {
            emit(Resource.Error(getErrorMessage(e)))
        }
    }

    suspend fun saveRemoteHistorySilently(token: String, request: HistoryMetadataRequest): Boolean {
        return try {
            val response = apiService.saveHistory(token.asBearerToken(), request)
            if (!response.isSuccessful) {
                val errorMessage = response.errorBody()?.string()?.take(240)
                Log.w("HistoryRepository", "Remote history save failed: ${response.code()} $errorMessage")
            }
            response.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    fun getRemoteHistory(
        token: String,
        type: String,
        page: Int? = null,
        limit: Int? = null,
    ): LiveData<Resource<HistoryListResponse>> = liveData {
        emit(Resource.Loading)
        try {
            val response = apiService.getHistory(
                token.asBearerToken(),
                type,
                page,
                limit,
                sortBy = "timestamp",
                sortOrder = "desc"
            )
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    emit(if (body.history.isNullOrEmpty()) Resource.Empty else Resource.Success(body))
                } ?: emit(Resource.InvalidResponse())
            } else {
                emit(response.toResourceError())
            }
        } catch (e: SocketTimeoutException) {
            emit(Resource.NetworkTimeout())
        } catch (e: Exception) {
            emit(Resource.Error(getErrorMessage(e)))
        }
    }

    fun getAllRemoteHistory(
        token: String,
        page: Int? = null,
        limit: Int? = 100,
    ): LiveData<Resource<HistoryListResponse>> = liveData {
        emit(Resource.Loading)
        try {
            val response = apiService.getAllHistory(
                token.asBearerToken(),
                page,
                limit,
                sortBy = "timestamp",
                sortOrder = "desc"
            )
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    emit(if (body.history.isNullOrEmpty()) Resource.Empty else Resource.Success(body))
                } ?: emit(Resource.InvalidResponse())
            } else {
                emit(response.toResourceError())
            }
        } catch (e: SocketTimeoutException) {
            emit(Resource.NetworkTimeout())
        } catch (e: Exception) {
            emit(Resource.Error(getErrorMessage(e)))
        }
    }

    fun deleteAllRemoteHistory(token: String): LiveData<Resource<SaveHistoryResponse>> = liveData {
        emit(Resource.Loading)
        try {
            val response = apiService.deleteAllHistory(token.asBearerToken())
            if (response.isSuccessful) {
                response.body()?.let { emit(Resource.Success(it)) } ?: emit(Resource.InvalidResponse())
            } else {
                emit(response.toResourceError())
            }
        } catch (e: SocketTimeoutException) {
            emit(Resource.NetworkTimeout())
        } catch (e: Exception) {
            emit(Resource.Error(getErrorMessage(e)))
        }
    }

    private fun String.asBearerToken(): String {
        return if (startsWith("Bearer ")) this else "Bearer $this"
    }

    private fun <T> retrofit2.Response<T>.toResourceError(): Resource<Nothing> {
        return when (code()) {
            401 -> Resource.Unauthorized()
            204, 404 -> Resource.Empty
            else -> Resource.Error("Error ${code()}: ${message()}")
        }
    }

    companion object {
        @Volatile
        private var instance: HistoryRepository? = null

        fun getInstance(
            apiService: HistoryApiService,
            dao: MentalHistoryDao,
        ): HistoryRepository =
            instance ?: synchronized(this) {
                instance ?: HistoryRepository(apiService, dao)
            }.also { instance = it }
    }
}
