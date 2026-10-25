package com.mentalys.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

/**
 * Application class.
 *
 * Reads the saved theme preference synchronously from SharedPreferences before any Activity
 * renders, preventing the white-flash / recreation loop that occurs when the theme is applied
 * asynchronously inside a coroutine after the UI has already drawn.
 *
 * SettingsPreferences still owns the DataStore copy (source of truth). We mirror the theme flag
 * to a plain SharedPreferences key so it is readable synchronously here.
 * The mirror is written in SettingsPreferences.saveThemeSettingSync().
 */
class MentalysApplication : Application() {

    override fun onCreate() {
        // Apply theme BEFORE super.onCreate() so the first Activity sees the correct mode.
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val isDark = prefs.getBoolean(KEY_THEME, false)
        currentLanguage = prefs.getString(KEY_LANGUAGE, "English") ?: "English"
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate()
    }

    companion object {
        const val PREFS_NAME = "mentalys_theme_prefs"
        const val KEY_THEME   = "is_dark_mode"
        const val KEY_LANGUAGE = "app_language"

        @Volatile
        var currentLanguage: String = "English"

        fun isIndonesian(): Boolean = currentLanguage == "Bahasa Indonesia"
    }
}
