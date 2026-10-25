package com.mentalys.app.data.remote.retrofit

import com.mentalys.app.data.remote.response.music.MusicDetailResponse
import com.mentalys.app.data.remote.response.music.MusicResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MusicApiService {
    @GET("search/")
    suspend fun searchSounds(
        @Query("query") query: String,
        @Query("fields") fields: String = "id,name,tags,license,username,created,url,previews",
        @Query("page_size") pageSize: Int = 60,
        @Query("filter") filter: String = "duration:[30 TO 600]"
    ): Response<MusicResponse>

    @GET("sounds/{id}/")
    suspend fun searchSound(
        @Path("id") id: Int
    ): Response<MusicDetailResponse>
}
