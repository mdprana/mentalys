package com.mentalys.app.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import com.mentalys.app.data.local.entity.MusicDetailEntity
import com.mentalys.app.data.local.entity.MusicItemEntity
import com.mentalys.app.data.local.room.MusicDao
import com.mentalys.app.data.remote.retrofit.MusicApiService
import com.mentalys.app.utils.Resource
import com.mentalys.app.utils.getErrorMessage

class MusicRepository(
    private val apiService: MusicApiService,
    private val dao: MusicDao,
) {

    private val backgroundQueries = listOf(
        "calm meditation ambient relaxing piano",
        "peaceful piano meditation soft ambient",
        "soft piano meditation ambient",
        "gentle instrumental meditation ambient",
        "calm relaxing music soft piano"
    )

    private val relaxMusicQueries = listOf(
        "calm meditation",
        "relaxing piano",
        "soft ambient",
        "meditation music",
        "peaceful instrumental"
    )

    fun getMusics(query: String): LiveData<Resource<List<MusicItemEntity>>> = liveData {
        emit(Resource.Loading)
        try {
            val queries = (listOf(query) + relaxMusicQueries).distinct()
            val musics = queries
                .flatMap { searchQuery ->
                    val response = apiService.searchSounds(
                        query = searchQuery,
                        pageSize = 30,
                        filter = "duration:[30 TO 900]"
                    )
                    if (response.isSuccessful) {
                        response.body()?.results.orEmpty().map { it.toEntity() }
                    } else {
                        Log.d("MusicRepository", "API call failed for '$searchQuery': ${response.message()}")
                        emptyList()
                    }
                }
                .distinctBy { it.id }

            if (musics.isNotEmpty()) {
                dao.insertMusics(musics)
            } else {
                Log.d("MusicRepository", "No music found in response.")
            }
        } catch (e: Exception) {
            val errorMessage = getErrorMessage(e)
            Log.d("MusicRepository", "Error: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }

        // Fetch data from the local database (Room)
        val localData = dao.getMusics().map { specialists ->
            if (specialists.isNotEmpty()) {
                Resource.Success(specialists) // Emit data only if not empty
            } else {
                Resource.Error("No local data available.") // Emit error if database is empty
            }
        }

        emitSource(localData)

    }

    fun searchMusic(id: Int): LiveData<Resource<MusicDetailEntity>> = liveData {
        emit(Resource.Loading)
        try {
            val response = apiService.searchSound(id)
            if (response.isSuccessful) {
                val music = response.body()?.toEntity()
                if (music != null) {
                     dao.insertMusic(music)
                } else {
                    Log.d("MusicRepository", "No music found in response.")
                }
            } else {
                // Handle the case when the response is not successful
                val errorMessage = response.message() ?: "Unknown error"
                Log.d("MusicRepository", "API call failed: $errorMessage")
                emit(Resource.Error(errorMessage))  // Emit error state with the response error message
            }
        } catch (e: Exception) {
            val errorMessage = getErrorMessage(e)
            Log.d("MusicRepository", "Error: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }

        // Fetch data from the local database (Room)
        val localData = dao.getMusic(id).map { specialists ->
            if (specialists != null) {
                Resource.Success(specialists) // Emit data only if not empty
            } else {
                Resource.Error("No local data available.") // Emit error if database is empty
            }
        }

        emitSource(localData) // Start observing the local data as the source
    }

    suspend fun getBackgroundPreviewUrl(excludeUrl: String? = null): String? {
        return try {
            val response = apiService.searchSounds(
                query = backgroundQueries.random(),
                pageSize = 40,
                filter = "duration:[45 TO 900] -tag:rain -tag:storm -tag:thunder -tag:ocean -tag:waves -tag:sea -tag:water -tag:river -tag:wind -tag:noise -tag:field-recording"
            )
            if (!response.isSuccessful) return null
            response.body()
                ?.results
                ?.mapNotNull { it.previews?.previewHqMp3 }
                ?.filter { it != excludeUrl }
                ?.randomOrNull()
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        @Volatile
        private var instance: MusicRepository? = null
        fun getInstance(
            apiService: MusicApiService,
            dao: MusicDao,
        ): MusicRepository = instance ?: synchronized(this) {
            instance ?: MusicRepository(
                apiService,
                dao,
            )
        }.also { instance = it }
    }

}
