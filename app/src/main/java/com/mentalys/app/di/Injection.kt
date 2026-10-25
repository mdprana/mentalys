package com.mentalys.app.di

import android.content.Context
import com.mentalys.app.data.local.room.MainDatabase
import com.mentalys.app.data.remote.retrofit.ApiConfig
import com.mentalys.app.data.repository.ArticleRepository
import com.mentalys.app.data.repository.ClinicRepository
import com.mentalys.app.data.repository.HistoryRepository
import com.mentalys.app.data.repository.MainRepository
import com.mentalys.app.data.repository.MusicRepository
import com.mentalys.app.data.repository.PsychiatristContactRepository
import com.mentalys.app.data.repository.ScreeningRepository
import com.mentalys.app.data.repository.SpecialistRepository

object Injection {

    fun provideMainRepository(context: Context): MainRepository {
        val apiService = ApiConfig.getMainApiService()
        val database = MainDatabase.getInstance(context)
        val dao = database.articleDao()
        return MainRepository.getInstance(apiService, dao)
    }

    fun provideArticlesRepository(context: Context): ArticleRepository {
        val apiService = ApiConfig.getArticlesApiService()
        val database = MainDatabase.getInstance(context)
        val dao = database.articleDao()
        return ArticleRepository.getInstance(apiService, dao)
    }

    fun provideClinicsRepository(context: Context): ClinicRepository {
        val apiService = ApiConfig.getClinicApiService()
        val database = MainDatabase.getInstance(context)
        val dao = database.clinicDao()
        return ClinicRepository.getInstance(apiService, dao)
    }

    fun provideSpecialistRepository(context: Context): SpecialistRepository {
        val apiService = ApiConfig.getSpecialistApiService()
        val database = MainDatabase.getInstance(context)
        val dao = database.specialistDao()
        return SpecialistRepository.getInstance(apiService, dao)
    }

    fun providePsychiatristContactRepository(): PsychiatristContactRepository {
        val apiService = ApiConfig.getPsychiatristContactApiService()
        return PsychiatristContactRepository.getInstance(apiService)
    }

    fun provideMusicRepository(context: Context): MusicRepository {
        val apiService = ApiConfig.getMusicApiService()
        val database = MainDatabase.getInstance(context)
        val dao = database.musicDao()
        return MusicRepository.getInstance(apiService, dao)
    }

    fun provideScreeningRepository(context: Context): ScreeningRepository {
        val database = MainDatabase.getInstance(context)
        val dao = database.mentalHistoryDao()
        return ScreeningRepository.getInstance(dao)
    }

    fun provideHistoryRepository(context: Context): HistoryRepository {
        val apiService = ApiConfig.getHistoryApiService()
        val database = MainDatabase.getInstance(context)
        val dao = database.mentalHistoryDao()
        return HistoryRepository.getInstance(apiService, dao)
    }
}
