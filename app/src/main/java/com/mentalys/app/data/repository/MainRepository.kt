package com.mentalys.app.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.delay
import com.google.gson.Gson
import com.mentalys.app.data.local.room.ArticleDao
import com.mentalys.app.data.remote.retrofit.MainApiService
import com.mentalys.app.utils.Resource
import com.mentalys.app.BuildConfig
import com.mentalys.app.data.remote.request.auth.LoginRequest
import com.mentalys.app.data.remote.request.auth.RegisterRequest
import com.mentalys.app.data.remote.request.auth.ResetPasswordRequest
import com.mentalys.app.data.remote.request.payment.PaymentChargeRequest
import com.mentalys.app.data.remote.response.auth.LoginResponse
import com.mentalys.app.data.remote.response.auth.RegisterResponse
import com.mentalys.app.data.remote.response.auth.ResetPasswordResponse
import com.mentalys.app.data.remote.response.payment.PaymentChargeResponse
import com.mentalys.app.data.remote.response.profile.ProfileResponse
import com.mentalys.app.utils.getErrorMessage
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

class MainRepository(
    private val mainApiService: MainApiService,
    private val articleDao: ArticleDao,
) {

    private val primaryGenerativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )
    private val fallbackGenerativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash-lite",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    fun getMentalHealthTips(prompt: String): LiveData<Resource<String>> = liveData {
        emit(Resource.Loading)
        val attempts = listOf(
            primaryGenerativeModel to 0L,
            primaryGenerativeModel to 800L,
            primaryGenerativeModel to 1800L,
            fallbackGenerativeModel to 0L,
            fallbackGenerativeModel to 1500L,
        )
        var lastError: Exception? = null
        for ((model, backoffMs) in attempts) {
            if (backoffMs > 0) delay(backoffMs)
            try {
                val response = model.generateContent(prompt)
                val text = response.text
                if (!text.isNullOrBlank()) {
                    emit(Resource.Success(text))
                    return@liveData
                }
            } catch (e: Exception) {
                lastError = e
                Log.w("MainRepository", "Gemini attempt failed: ${e.message}")
            }
        }
        val errorMessage = lastError?.let { getErrorMessage(it) } ?: "AI unavailable"
        Log.e("MainRepository", "All Gemini attempts failed", lastError)
        emit(Resource.Error(errorMessage))
    }

    fun paymentCharge(
        token: String,
        psychiatristId: String,
        phoneNumber: String? = null
    ): LiveData<Resource<PaymentChargeResponse>> = liveData {
        emit(Resource.Loading)
        val request = PaymentChargeRequest(
            psychiatristId = psychiatristId,
            phoneNumber = phoneNumber?.takeIf { it.isNotBlank() },
        )
        try {
            val response = mainApiService.paymentCharge("Bearer $token", request)
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    emit(Resource.Success(responseBody))
                } else {
                    emit(Resource.Error("Response body is null"))
                }
            } else {
                val errorJson = response.errorBody()?.string()
                val errorMessage = try {
                    val errorResponse = Gson().fromJson(errorJson, PaymentChargeResponse::class.java)
                    errorResponse.message
                } catch (e: Exception) {
                    null
                } ?: "Payment failed (${response.code()})"
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = getErrorMessage(e)
            Log.d("MainRepository", "Payment error: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }
    }

    // AUTHENTICATION
    fun registerUser(
        firstName: String,
        lastName: String,
        username: String,
        email: String,
        phoneNumber: String,
        password: String,
        confirmPassword: String,
        birthDate: String = "",
        location: String = "",
        gender: String = ""
    ): LiveData<Resource<RegisterResponse>> = liveData {
        emit(Resource.Loading)
        val request = RegisterRequest(
            firstName = firstName,
            lastName = lastName,
            username = username,
            email = email,
            phoneNumber = phoneNumber,
            password = password,
            confirmPassword = confirmPassword,
        )
        try {
            val response = mainApiService.registerUser(request)
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    emit(Resource.Success(responseBody))
                } else {
                    emit(Resource.Error("Response body is null"))
                }
            } else {
                // Parse error response into a structured format
                val errorJson = response.errorBody()?.string()
                val errorMessage = try {
                    val errorResponse = Gson().fromJson(errorJson, RegisterResponse::class.java)
                    errorResponse.message
                } catch (e: Exception) {
                    "Unknown error occurred"
                }
                errorMessage?.let { Resource.Error(it) }?.let { emit(it) }
            }
        } catch (e: Exception) {
            val errorMessage = getErrorMessage(e)
            Log.d("MainRepository", "Error: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }
    }

    fun loginUser(email: String, password: String): LiveData<Resource<LoginResponse>> = liveData {
        emit(Resource.Loading)
        val request = LoginRequest(email, password)
        try {
            val response = mainApiService.loginUser(request)
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    emit(Resource.Success(responseBody))
                } else {
                    emit(Resource.Error("Response body is null"))
                }
            } else {
                // Parse error response into a structured format
                val errorJson = response.errorBody()?.string()
                val errorMessage = try {
                    val errorResponse = Gson().fromJson(errorJson, LoginResponse::class.java)
                    errorResponse.message
                } catch (e: Exception) {
                    "Unknown error occurred"
                }
                errorMessage?.let { Resource.Error(it) }?.let { emit(it) }
            }
        } catch (e: Exception) {
            val errorMessage = getErrorMessage(e)
            Log.d("MainRepository", "Error: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }
    }

    fun resetPassword(email: String): LiveData<Resource<ResetPasswordResponse>> = liveData {
        emit(Resource.Loading)
        val request = ResetPasswordRequest(email)
        try {
            val response = mainApiService.resetPassword(request)
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    emit(Resource.Success(responseBody))
                } else {
                    emit(Resource.Error("Response body is null"))
                }
            } else {
                // Parse error response into a structured format
                val errorJson = response.errorBody()?.string()
                val errorMessage = try {
                    val errorResponse =
                        Gson().fromJson(errorJson, ResetPasswordResponse::class.java)
                    errorResponse.message
                } catch (e: Exception) {
                    "Unknown error occurred"
                }
                errorMessage?.let { Resource.Error(it) }?.let { emit(it) }
            }
        } catch (e: Exception) {
            val errorMessage = getErrorMessage(e)
            Log.d("MainRepository", "Error: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }
    }

    // Check have profile or not
    fun getProfile(token: String): LiveData<Resource<ProfileResponse>> = liveData {
        emit(Resource.Loading)
        try {
            val response = mainApiService.getProfile("Bearer $token")
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    emit(Resource.Success(responseBody))
                } else {
                    emit(Resource.Error("Response body is null"))
                }
            } else {
                // Handle specific HTTP error codes
                when (response.code()) {
                    404 -> emit(Resource.Error("Profile not found (404)."))
                    else -> emit(Resource.Error("Error ${response.code()}: ${response.message()}"))
                }
            }
        } catch (e: Exception) {
            val errorMessage = getErrorMessage(e)
            Log.d("MainRepository", "Error: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }
    }

    suspend fun getProfileOnce(token: String): Resource<ProfileResponse> {
        return try {
            val response = mainApiService.getProfile("Bearer $token")
            if (response.isSuccessful) {
                response.body()?.let { Resource.Success(it) }
                    ?: Resource.Error("Response body is null")
            } else {
                when (response.code()) {
                    404 -> Resource.Error("Profile not found (404).")
                    else -> Resource.Error("Error ${response.code()}: ${response.message()}")
                }
            }
        } catch (e: Exception) {
            val errorMessage = getErrorMessage(e)
            Log.d("MainRepository", "Error: $errorMessage", e)
            Resource.Error(errorMessage)
        }
    }

    // Profile Update Method
    fun updateProfile(
        token: String,
        username: String?,
        fullName: String?,
        birthDate: String?,
        location: String?,
        gender: String?,
        profilePicFile: File?, // Optional profile picture file
        phoneNumber: String? = null
    ): LiveData<Resource<ProfileResponse>> = liveData {
        emit(Resource.Loading)  // Emitting loading state

        // Prepare request body for the text fields only if they are non-null and non-empty
        val usernameRequestBody = username?.takeIf { it.isNotEmpty() }?.let {
            RequestBody.create("text/plain".toMediaTypeOrNull(), it)
        }

        val fullNameRequestBody = fullName?.takeIf { it.isNotEmpty() }?.let {
            RequestBody.create("text/plain".toMediaTypeOrNull(), it)
        }

        val birthDateRequestBody = birthDate?.takeIf { it.isNotEmpty() }?.let {
            RequestBody.create("text/plain".toMediaTypeOrNull(), it)
        }

        val locationRequestBody = location?.takeIf { it.isNotEmpty() }?.let {
            RequestBody.create("text/plain".toMediaTypeOrNull(), it)
        }

        val genderRequestBody = gender?.takeIf { it.isNotEmpty() }?.let {
            RequestBody.create("text/plain".toMediaTypeOrNull(), it)
        }

        val phoneNumberRequestBody = phoneNumber?.takeIf { it.isNotEmpty() }?.let {
            RequestBody.create("text/plain".toMediaTypeOrNull(), it)
        }

        // Prepare the profile picture if it exists and is not null
        val profilePicPart = profilePicFile?.takeIf { it.exists() }?.let {
            val profilePicRequestBody = RequestBody.create("image/*".toMediaTypeOrNull(), it)
            MultipartBody.Part.createFormData("profile_pic", it.name, profilePicRequestBody)
        }

        try {
            // Call the updateProfile API only with the valid (non-null) parts
            val response = mainApiService.updateProfile(
                "Bearer $token",
                usernameRequestBody,
                fullNameRequestBody,
                birthDateRequestBody,
                locationRequestBody,
                genderRequestBody,
                phoneNumberRequestBody,
                profilePicPart
            )

            if (response.isSuccessful) {
                response.body()?.let {
                    emit(Resource.Success(it)) // Emit the success result
                } ?: run {
                    emit(Resource.Error("Empty response body"))
                }
            } else {
                val errorJson = response.errorBody()?.string()
                val errorMessage =
                    "Failed to update profile" // You can parse the error further if needed
                emit(Resource.Error(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = getErrorMessage(e)
            Log.d("MainRepository", "Error: $errorMessage", e)
            emit(Resource.Error(errorMessage))
        }
    }

    suspend fun updateProfileOnce(
        token: String,
        username: String?,
        fullName: String?,
        birthDate: String?,
        location: String?,
        gender: String?,
        profilePicFile: File?,
        phoneNumber: String? = null
    ): Resource<ProfileResponse> {
        val usernameRequestBody = username?.takeIf { it.isNotEmpty() }?.let {
            RequestBody.create("text/plain".toMediaTypeOrNull(), it)
        }
        val fullNameRequestBody = fullName?.takeIf { it.isNotEmpty() }?.let {
            RequestBody.create("text/plain".toMediaTypeOrNull(), it)
        }
        val birthDateRequestBody = birthDate?.takeIf { it.isNotEmpty() }?.let {
            RequestBody.create("text/plain".toMediaTypeOrNull(), it)
        }
        val locationRequestBody = location?.takeIf { it.isNotEmpty() }?.let {
            RequestBody.create("text/plain".toMediaTypeOrNull(), it)
        }
        val genderRequestBody = gender?.takeIf { it.isNotEmpty() }?.let {
            RequestBody.create("text/plain".toMediaTypeOrNull(), it)
        }
        val phoneNumberRequestBody = phoneNumber?.takeIf { it.isNotEmpty() }?.let {
            RequestBody.create("text/plain".toMediaTypeOrNull(), it)
        }
        val profilePicPart = profilePicFile?.takeIf { it.exists() }?.let {
            val profilePicRequestBody = RequestBody.create("image/*".toMediaTypeOrNull(), it)
            MultipartBody.Part.createFormData("profile_pic", it.name, profilePicRequestBody)
        }

        return try {
            val response = mainApiService.updateProfile(
                "Bearer $token",
                usernameRequestBody,
                fullNameRequestBody,
                birthDateRequestBody,
                locationRequestBody,
                genderRequestBody,
                phoneNumberRequestBody,
                profilePicPart
            )
            if (response.isSuccessful) {
                response.body()?.let { Resource.Success(it) }
                    ?: Resource.Error("Empty response body")
            } else {
                Resource.Error("Failed to update profile")
            }
        } catch (e: Exception) {
            val errorMessage = getErrorMessage(e)
            Log.d("MainRepository", "Error: $errorMessage", e)
            Resource.Error(errorMessage)
        }
    }

    companion object {
        @Volatile
        private var instance: MainRepository? = null
        fun getInstance(
            mainApiService: MainApiService,
            articleDao: ArticleDao,
        ): MainRepository = instance ?: synchronized(this) {
            instance ?: MainRepository(
                mainApiService,
                articleDao,
            )
        }.also { instance = it }
    }

}
