package com.mentalys.app.data.remote.retrofit

import com.mentalys.app.data.remote.response.psychiatrist.PsychiatristContactResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PsychiatristContactApiService {

    @GET("google-maps/psychiatrists")
    suspend fun getPsychiatrists(
        @Query("location") location: String,
        @Query("radius") radius: Int,
    ): Response<List<PsychiatristContactResponse>>

    @GET("google-maps/psychiatrist/{placeId}")
    suspend fun getPsychiatristDetail(
        @Path("placeId") placeId: String,
    ): Response<PsychiatristContactResponse>
}
