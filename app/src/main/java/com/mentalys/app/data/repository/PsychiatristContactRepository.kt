package com.mentalys.app.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.mentalys.app.data.remote.response.psychiatrist.PsychiatristContactResponse
import com.mentalys.app.data.remote.retrofit.PsychiatristContactApiService
import com.mentalys.app.utils.Resource
import com.mentalys.app.utils.getErrorMessage
import java.net.SocketTimeoutException

class PsychiatristContactRepository(
    private val apiService: PsychiatristContactApiService,
) {

    fun getPsychiatrists(
        latitude: Double,
        longitude: Double,
        radius: Int = 5000,
    ): LiveData<Resource<List<PsychiatristContactResponse>>> = liveData {
        emit(Resource.Loading)
        try {
            val response = apiService.getPsychiatrists("$latitude,$longitude", radius)
            if (response.isSuccessful) {
                val body = response.body().orEmpty()
                emit(if (body.isEmpty()) Resource.Empty else Resource.Success(body))
            } else {
                emit(response.toResourceError())
            }
        } catch (e: SocketTimeoutException) {
            emit(Resource.NetworkTimeout())
        } catch (e: Exception) {
            emit(Resource.Error(getErrorMessage(e)))
        }
    }

    fun getPsychiatristDetail(placeId: String): LiveData<Resource<PsychiatristContactResponse>> =
        liveData {
            emit(Resource.Loading)
            try {
                val response = apiService.getPsychiatristDetail(placeId)
                if (response.isSuccessful) {
                    response.body()?.let { emit(Resource.Success(it)) } ?: emit(Resource.Empty)
                } else {
                    emit(response.toResourceError())
                }
            } catch (e: SocketTimeoutException) {
                emit(Resource.NetworkTimeout())
            } catch (e: Exception) {
                emit(Resource.Error(getErrorMessage(e)))
            }
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
        private var instance: PsychiatristContactRepository? = null

        fun getInstance(apiService: PsychiatristContactApiService): PsychiatristContactRepository =
            instance ?: synchronized(this) {
                instance ?: PsychiatristContactRepository(apiService)
            }.also { instance = it }
    }
}
