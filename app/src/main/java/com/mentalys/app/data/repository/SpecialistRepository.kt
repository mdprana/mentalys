package com.mentalys.app.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import com.mentalys.app.data.local.entity.SpecialistEntity
import com.mentalys.app.data.local.room.SpecialistDao
import com.mentalys.app.data.remote.retrofit.SpecialistApiService
import com.mentalys.app.utils.ApiContentLocalizer
import com.mentalys.app.utils.Resource
import com.mentalys.app.utils.getErrorMessage

class SpecialistRepository(
    private val apiService: SpecialistApiService,
    private val dao: SpecialistDao,
) {

    fun getConsultations(): LiveData<Resource<List<SpecialistEntity>>> = liveData {
        emit(Resource.Loading)
        var remoteError: String? = null
        try {
            val response = apiService.getSpecialists()
            if (response.isSuccessful) {
                val specialists = response.body()?.data?.map { it.toEntity() }
                if (!specialists.isNullOrEmpty()) {
                    dao.clearSpecialists()
                    dao.insertSpecialists(specialists)
                    emit(Resource.Success(localizeSpecialists(specialists)))
                } else {
                    remoteError = "No specialist data available from the API."
                    Log.d("ConsultationRepository", remoteError)
                }
            } else {
                remoteError = response.message() ?: "Unknown error"
                Log.d("ConsultationRepository", "API call failed: $remoteError")
            }
        } catch (e: Exception) {
            remoteError = getErrorMessage(e)
            Log.d("ConsultationRepository", "Error: $remoteError", e)
        }

        dao.clearGeneratedSpecialists()

        val cachedSpecialists = dao.getSpecialistsOnce()
        if (cachedSpecialists.isNotEmpty()) {
            emit(Resource.Success(localizeSpecialists(cachedSpecialists)))
        } else {
            emit(Resource.Error(remoteError ?: "No specialist data available from the API."))
        }
    }

    fun getConsultation(id: String): LiveData<Resource<SpecialistEntity>> = liveData {
        emit(Resource.Loading)
        var remoteError: String? = null
        try {
            val response = apiService.getSpecialist(id)
            if (response.isSuccessful) {
                val specialists = response.body()?.data?.toEntity()
                if (specialists != null) {
                    dao.insertSpecialist(specialists)
                    emit(Resource.Success(ApiContentLocalizer.specialist(specialists)))
                    return@liveData
                } else {
                    remoteError = "No specialist data available from the API."
                    Log.d("ConsultationRepository", remoteError)
                }
            } else {
                remoteError = response.message() ?: "Unknown error"
                Log.d("ConsultationRepository", "API call failed: $remoteError")
            }
        } catch (e: Exception) {
            remoteError = getErrorMessage(e)
            Log.d("ConsultationRepository", "Error: $remoteError", e)
        }

        val localData = dao.getSpecialist(id).map { specialists ->
            if (specialists != null) {
                Resource.Success(ApiContentLocalizer.specialist(specialists))
            } else {
                Resource.Error(remoteError ?: "No specialist data available from the API.")
            }
        }

        emitSource(localData)
    }

    private fun localizeSpecialists(specialists: List<SpecialistEntity>): List<SpecialistEntity> {
        return specialists
            .map(ApiContentLocalizer::specialist)
            .sortedByDescending { it.ratings ?: 0.0 }
    }


    companion object {
        @Volatile
        private var instance: SpecialistRepository? = null
        fun getInstance(
            apiService: SpecialistApiService,
            dao: SpecialistDao,
        ): SpecialistRepository = instance ?: synchronized(this) {
            instance ?: SpecialistRepository(
                apiService,
                dao,
            )
        }.also { instance = it }
    }


}
