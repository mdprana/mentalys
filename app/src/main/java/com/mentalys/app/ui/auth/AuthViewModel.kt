package com.mentalys.app.ui.auth

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.mentalys.app.data.remote.response.auth.LoginData
import com.mentalys.app.data.remote.response.auth.LoginProfile
import com.mentalys.app.data.remote.response.auth.LoginResponse
import com.mentalys.app.data.remote.response.auth.RegisterResponse
import com.mentalys.app.data.remote.response.auth.ResetPasswordResponse
import com.mentalys.app.data.remote.response.profile.ProfileData
import com.mentalys.app.data.repository.MainRepository
import com.mentalys.app.utils.Resource
import com.mentalys.app.utils.SettingsPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.Normalizer
import java.util.Locale

class AuthViewModel(
    private val repository: MainRepository,
    private val preferences: SettingsPreferences
) : ViewModel() {

    private val _registerResult = MutableLiveData<Resource<RegisterResponse>>()
    val registerResult: LiveData<Resource<RegisterResponse>> get() = _registerResult

    private val _loginResult = MutableLiveData<Resource<LoginResponse>>()
    val loginResult: LiveData<Resource<LoginResponse>> get() = _loginResult

    private val _resetPasswordResult = MutableLiveData<Resource<ResetPasswordResponse>>()
    val resetPasswordResult: LiveData<Resource<ResetPasswordResponse>> get() = _resetPasswordResult

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> get() = _isLoggedIn

    private val _currentImageUri = MutableLiveData<Uri?>()
    val currentImageUri: LiveData<Uri?> = _currentImageUri

    fun setImageUri(uri: Uri) {
        _currentImageUri.value = uri
    }

    fun registerUser(
        firstName: String,
        lastName: String,
        username: String,
        email: String,
        phoneNumber: String,
        password: String,
        confirmPassword: String
    ) {
        viewModelScope.launch {
            val result = repository.registerUser(
                firstName = firstName,
                lastName = lastName,
                username = username,
                email = email,
                phoneNumber = phoneNumber,
                password = password,
                confirmPassword = confirmPassword
            )
            result.observeForever {
                _registerResult.postValue(it)
            }
        }
    }

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            val result = repository.loginUser(email, password)
            result.observeForever { data ->
                if (data is Resource.Success) {
                    data.data?.let { response ->
                        val profile = response.profile
                        viewModelScope.launch {
                            persistUserLoginSession(
                                uid = response.data?.uid.orEmpty(),
                                token = response.idToken,
                                email = response.data?.email.orEmpty(),
                                fullName = profile?.full_name.orEmpty().ifBlank {
                                    listOfNotNull(profile?.firstName, profile?.lastName)
                                        .joinToString(" ")
                                        .trim()
                                },
                                username = profile?.username.orEmpty(),
                                phoneNumber = profile?.phoneNumber.orEmpty()
                            )
                            _loginResult.postValue(data)
                        }
                        return@observeForever
                    }
                }
                _loginResult.postValue(data)
            }
        }
    }

    /**
     * Sign in with a Google ID token using Firebase Authentication.
     * On success, retrieves the Firebase ID token and stores the session via [saveUserLoginSession].
     */
    fun loginWithGoogle(
        idToken: String,
        googleDisplayName: String? = null,
        googleEmail: String? = null,
        phoneNumber: String = ""
    ) {
        viewModelScope.launch {
            _loginResult.postValue(Resource.Loading)
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val firebaseUser = FirebaseAuth.getInstance()
                    .signInWithCredential(credential)
                    .await()
                    .user ?: throw Exception("Firebase user is null")

                val token = firebaseUser.getIdToken(false).await().token
                    ?: throw Exception("Firebase ID token is null")

                val email = firebaseUser.email ?: googleEmail.orEmpty()
                val displayName = (firebaseUser.displayName
                    ?: googleDisplayName
                    ?: email.substringBefore("@").replace('.', ' '))
                    .ifBlank { "Mentalys User" }
                val uid = firebaseUser.uid
                val username = generateUsername(displayName, email, uid)
                val backendProfile = loadGoogleProfile(token, displayName, username, phoneNumber)
                val savedFullName = backendProfile?.fullName.orEmpty().ifBlank {
                    listOfNotNull(backendProfile?.firstName, backendProfile?.lastName)
                        .joinToString(" ")
                        .trim()
                        .ifBlank { displayName }
                }
                val savedUsername = backendProfile?.username.orEmpty().ifBlank { username }
                val savedPhoneNumber = backendProfile?.phoneNumber.orEmpty().ifBlank { phoneNumber }

                persistUserLoginSession(
                    uid = uid,
                    token = token,
                    email = email,
                    fullName = savedFullName,
                    username = savedUsername,
                    phoneNumber = savedPhoneNumber
                )

                // Emit a synthetic LoginResponse success so observers react the same way
                // as for email login. The Activity checks resource.data != null and
                // uses the token from DataStore (already saved above), so these can be stubs.
                _loginResult.postValue(
                    Resource.Success(
                        LoginResponse(
                            status = "success",
                            idToken = token,
                            message = "Google Sign-In successful",
                            data = LoginData(uid = uid, email = email),
                            profile = LoginProfile(
                                firstName = backendProfile?.firstName ?: savedFullName.substringBefore(" "),
                                lastName = backendProfile?.lastName.orEmpty(),
                                username = savedUsername,
                                phoneNumber = savedPhoneNumber,
                                full_name = savedFullName,
                                created_at = null,
                                uid = uid
                            )
                        )
                    )
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google Sign-In failed", e)
                _loginResult.postValue(Resource.Error(e.message ?: "Google Sign-In failed"))
            }
        }
    }

    private fun generateUsername(displayName: String, email: String, uid: String): String {
        val source = displayName.ifBlank { email.substringBefore("@") }.ifBlank { "mentalys" }
        val ascii = Normalizer.normalize(source, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
        val cleaned = ascii
            .lowercase(Locale.US)
            .replace("[^a-z0-9._]".toRegex(), "")
            .trim('.', '_')
        val fallbackSuffix = uid.take(6).lowercase(Locale.US)
        val base = cleaned.ifBlank { "user$fallbackSuffix" }
        val withMinLength = if (base.length >= 5) base else "${base}$fallbackSuffix"
        return withMinLength.take(30)
    }

    private suspend fun loadGoogleProfile(
        token: String,
        fullName: String,
        username: String,
        phoneNumber: String
    ): ProfileData? {
        val existingProfile = repository.getProfileOnce(token)
        if (existingProfile is Resource.Success) {
            val profile = existingProfile.data.data
            if (profile != null) {
                val shouldFillFullName = profile.fullName.isNullOrBlank() && fullName.isNotBlank()
                val shouldFillUsername = profile.username.isNullOrBlank() && username.isNotBlank()
                if (shouldFillFullName || shouldFillUsername) {
                    val updatedProfile = repository.updateProfileOnce(
                        token = token,
                        username = username.takeIf { shouldFillUsername },
                        fullName = fullName.takeIf { shouldFillFullName },
                        birthDate = null,
                        location = null,
                        gender = null,
                        profilePicFile = null,
                        phoneNumber = null
                    )
                    if (updatedProfile is Resource.Success) {
                        return updatedProfile.data.data ?: profile
                    }
                }
                return profile
            }
        }

        if (existingProfile is Resource.Error && !existingProfile.error.contains("404")) {
            Log.w("AuthViewModel", "Google profile fetch failed: ${existingProfile.error}")
            return null
        }

        val createdProfile = repository.updateProfileOnce(
            token = token,
            username = username,
            fullName = fullName,
            birthDate = null,
            location = null,
            gender = null,
            profilePicFile = null,
            phoneNumber = phoneNumber.takeIf { it.isNotBlank() }
        )
        if (createdProfile is Resource.Error) {
            Log.w("AuthViewModel", "Google profile create failed: ${createdProfile.error}")
        }
        return (createdProfile as? Resource.Success)?.data?.data
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            val result = repository.resetPassword(email)
            result.observeForever {
                _resetPasswordResult.postValue(it)
            }
        }
    }

    fun saveUserLoginSession(uid: String, token: String, email: String, fullName: String, username: String, phoneNumber: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            persistUserLoginSession(uid, token, email, fullName, username, phoneNumber)
            onComplete() // Notify that the operation is complete
        }
    }

    private suspend fun persistUserLoginSession(
        uid: String,
        token: String,
        email: String,
        fullName: String,
        username: String,
        phoneNumber: String
    ) {
        preferences.saveIsLoginSetting(true)
        preferences.saveUidSetting(uid)
        preferences.saveTokenSetting(token)
        preferences.saveEmailSetting(email)
        preferences.saveFullNameSetting(fullName)
        preferences.saveUsernameSetting(username)
        preferences.savePhoneNumberSetting(phoneNumber)
        _isLoggedIn.value = true
    }

    fun deleteLoginSession() {
        viewModelScope.launch {
            FirebaseAuth.getInstance().signOut()
            // auth session
            preferences.deleteIsLoginSetting()
            preferences.deleteUidSetting()
            preferences.deleteTokenSetting()
            preferences.deleteEmailSetting()
            preferences.deletePhoneNumberSetting()
            // profile session
            preferences.deleteUpdatedAtSetting()
            preferences.deleteCreatedAtSetting()
            preferences.deleteBirthDateSetting()
            preferences.deleteGenderSetting()
            preferences.deleteUsernameSetting()
            preferences.deleteProfilePicSetting()
            preferences.deleteLocationSetting()
            preferences.deleteFullNameSetting()
            //
            preferences.deleteIsHaveProfileSetting()
            _isLoggedIn.value = false
        }
    }

}
