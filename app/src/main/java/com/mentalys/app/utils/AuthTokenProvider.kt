package com.mentalys.app.utils

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

object AuthTokenProvider {
    suspend fun getFreshToken(context: Context): String {
        val preferences = SettingsPreferences.getInstance(context.dataStore)
        val storedToken = preferences.getTokenSetting().first()
        val storedUid = preferences.getUidSetting().first()
        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return storedToken
        if (storedUid.isNotBlank() && firebaseUser.uid != storedUid) {
            return storedToken
        }

        return try {
            val freshToken = firebaseUser.getIdToken(true).await().token.orEmpty()
            if (freshToken.isNotBlank() && freshToken != storedToken) {
                preferences.saveTokenSetting(freshToken)
            }
            freshToken.ifBlank { storedToken }
        } catch (_: Exception) {
            storedToken
        }
    }
}
