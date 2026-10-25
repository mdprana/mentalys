package com.mentalys.app.data.remote.retrofit

import com.mentalys.app.data.remote.response.clinic.ClinicResponse
import com.mentalys.app.data.remote.response.clinic.ClinicDetailResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ClinicApiService {
    @GET("api/places/search")
    suspend fun getNearbyClinic(
        @Query("lat") lat : Number,
        @Query("lng") lng : Number,
        @Query("radius") radius : Int,
        @Query("openNow") openNow: Boolean? = null,
        @Query("language") language: String? = null,
    ) : Response<ClinicResponse>

    @GET("api/places/details/{placeId}")
    suspend fun getClinicDetail(
        @Path("placeId") placeId: String,
        @Query("language") language: String? = null,
    ): Response<ClinicDetailResponse>
}
