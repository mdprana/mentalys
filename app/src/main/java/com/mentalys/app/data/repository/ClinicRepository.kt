package com.mentalys.app.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import com.mentalys.app.data.local.entity.ClinicEntity
import com.mentalys.app.data.local.room.ClinicDao
import com.mentalys.app.data.remote.response.clinic.PlaceDetails
import com.mentalys.app.data.remote.retrofit.ClinicApiService
import com.mentalys.app.utils.Resource
import com.mentalys.app.utils.getErrorMessage
import com.mentalys.app.MentalysApplication
import java.net.SocketTimeoutException

class ClinicRepository(
    private val apiService: ClinicApiService,
    private val clinicDao: ClinicDao,
) {

    // Get Nearby Clinic
    fun getNearbyClinic(lat: Number, lng: Number ): LiveData<Resource<List<ClinicEntity>>> = liveData {
        emit(Resource.Loading)
        try {
            val response = apiService.getNearbyClinic(lat, lng, 5000, language = apiLanguage())
            if (response.isSuccessful) {
                val clinic = response.body()?.data.orEmpty()
                val clinicEntities = clinic?.map { it.toClinicEntity() }
                if (!clinicEntities.isNullOrEmpty()) {
                    clinicDao.clearClinics()
                    clinicDao.insertListClinics(clinicEntities)
                } else {
                    emit(Resource.Empty)
                }
            } else {
                // Handle the case when the response is not successful
                emit(response.toResourceError())
            }
        } catch (e: SocketTimeoutException) {
            emit(Resource.NetworkTimeout())
        } catch (e: Exception) {
            val errorMessage = getErrorMessage(e)
            Log.d("ClinicRepository", "Error: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }

        // Fetch data from the local database (Room)
        val localData = clinicDao.getAllClinics().map { clinicList ->
            if (clinicList.isNotEmpty()) {
                Resource.Success(clinicList) // Emit data only if not empty
            } else {
                Resource.Empty
            }
        }
        emitSource(localData) // Start observing the local data as the source
    }

    fun get4NearbyClinic(lat: Number, lng: Number ): LiveData<Resource<List<ClinicEntity>>> = liveData {
        emit(Resource.Loading)
        try {
            val response = apiService.getNearbyClinic(lat, lng, 5000, language = apiLanguage())
            if (response.isSuccessful) {
                val clinic = response.body()?.data.orEmpty()
                val clinicEntities = clinic?.map { it.toClinicEntity() }
                if (!clinicEntities.isNullOrEmpty()) {
                    clinicDao.clearClinics()
                    clinicDao.insertListClinics(clinicEntities)
                } else {
                    emit(Resource.Empty)
                }
            } else {
                // Handle the case when the response is not successful
                emit(response.toResourceError())
            }
        } catch (e: SocketTimeoutException) {
            emit(Resource.NetworkTimeout())
        } catch (e: Exception) {
            val errorMessage = getErrorMessage(e)
            Log.d("ClinicRepository", "Error: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }
        // Fetch data from the local database (Room)
        val localData = clinicDao.get4Clinics().map { clinicList ->
            if (clinicList.isNotEmpty()) {
                Resource.Success(clinicList) // Emit data only if not empty
            } else {
                Resource.Empty
            }
        }
        emitSource(localData) // Start observing the local data as the source
    }

    fun getClinicDetail(placeId: String): LiveData<Resource<PlaceDetails>> = liveData {
        emit(Resource.Loading)
        try {
            val response = apiService.getClinicDetail(placeId, apiLanguage())
            if (response.isSuccessful) {
                response.body()?.data?.let { emit(Resource.Success(it)) } ?: emit(Resource.Empty)
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

    private fun apiLanguage(): String = if (MentalysApplication.isIndonesian()) "id" else "en"

    companion object {
        @Volatile
        private var instance: ClinicRepository? = null
        fun getInstance(
            clinicApiService: ClinicApiService,
            clinicDao: ClinicDao
        ): ClinicRepository = instance ?: synchronized(this) {
            instance ?: ClinicRepository(
                clinicApiService,
                clinicDao,
            )
        }.also { instance = it }
    }

}
