package com.mentalys.app.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mentalys.app.data.repository.ArticleRepository
import com.mentalys.app.data.repository.ClinicRepository
import com.mentalys.app.data.repository.HistoryRepository
import com.mentalys.app.data.repository.MainRepository
import com.mentalys.app.data.repository.MusicRepository
import com.mentalys.app.data.repository.PsychiatristContactRepository
import com.mentalys.app.data.repository.ScreeningRepository
import com.mentalys.app.data.repository.SpecialistRepository
import com.mentalys.app.di.Injection
import com.mentalys.app.ui.article.ArticleViewModel
import com.mentalys.app.ui.auth.AuthViewModel
import com.mentalys.app.ui.clinic.ClinicViewModel
import com.mentalys.app.ui.mental.history.ScreeningHistoryViewModel
import com.mentalys.app.ui.mental.test.MentalTestResultViewModel
import com.mentalys.app.ui.music.MusicViewModel
import com.mentalys.app.ui.profile.ProfileViewModel
import com.mentalys.app.ui.specialist.PsychiatristContactViewModel
import com.mentalys.app.ui.specialist.SpecialistViewModel
import com.mentalys.app.utils.SettingsPreferences
import com.mentalys.app.utils.dataStore

class ViewModelFactory(
    private val mainRepository: MainRepository,
    private val articleRepository: ArticleRepository,
    private val screeningRepository: ScreeningRepository,
    private val historyRepository: HistoryRepository,
    private val specialistRepository: SpecialistRepository,
    private val psychiatristContactRepository: PsychiatristContactRepository,
    private val clinicRepository: ClinicRepository,
    private val musicRepository: MusicRepository,
    private val preferences: SettingsPreferences
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(mainRepository, preferences) as T
        }
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(mainRepository, preferences) as T
        }
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel(mainRepository, preferences) as T
        }
        if (modelClass.isAssignableFrom(ArticleViewModel::class.java)) {
            return ArticleViewModel(articleRepository) as T
        }
        if (modelClass.isAssignableFrom(ClinicViewModel::class.java)) {
            return ClinicViewModel(clinicRepository) as T
        }
        if (modelClass.isAssignableFrom(GeminiViewModel::class.java)) {
            return GeminiViewModel(mainRepository, preferences) as T
        }
        if (modelClass.isAssignableFrom(MentalTestResultViewModel::class.java)) {
            return MentalTestResultViewModel(articleRepository) as T
        }
        if (modelClass.isAssignableFrom(ScreeningHistoryViewModel::class.java)) {
            return ScreeningHistoryViewModel(historyRepository) as T
        }
        if (modelClass.isAssignableFrom(SpecialistViewModel::class.java)) {
            return SpecialistViewModel(specialistRepository, mainRepository) as T
        }
        if (modelClass.isAssignableFrom(PsychiatristContactViewModel::class.java)) {
            return PsychiatristContactViewModel(psychiatristContactRepository) as T
        }
        if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
            return MusicViewModel(musicRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
    }

    companion object {
        @Volatile
        private var instance: ViewModelFactory? = null

        fun getInstance(context: Context): ViewModelFactory =
            instance ?: synchronized(this) {
                val mainRepository = Injection.provideMainRepository(context)
                val articlesRepository = Injection.provideArticlesRepository(context)
                val clinicRepository = Injection.provideClinicsRepository(context)
                val screeningRepository = Injection.provideScreeningRepository(context)
                val historyRepository = Injection.provideHistoryRepository(context)
                val specialistRepository = Injection.provideSpecialistRepository(context)
                val psychiatristContactRepository = Injection.providePsychiatristContactRepository()
                val musicRepository = Injection.provideMusicRepository(context)
                val preferences = SettingsPreferences.getInstance(context.dataStore, context)
                ViewModelFactory(
                    mainRepository,
                    articlesRepository,
                    screeningRepository,
                    historyRepository,
                    specialistRepository,
                    psychiatristContactRepository,
                    clinicRepository,
                    musicRepository,
                    preferences
                ).also { instance = it }
            }
    }
}
