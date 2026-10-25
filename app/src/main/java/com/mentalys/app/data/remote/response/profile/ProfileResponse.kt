package com.mentalys.app.data.remote.response.profile

import com.google.gson.annotations.SerializedName

data class ProfileResponse(
    val status: String?,
    val message: String?,
    val data: ProfileData?,
    val errors: List<ProfileError?>?
)

data class ProfileData(
    val username: String?,
    @SerializedName("full_name")
    val fullName: String?,
    val firstName: String?,
    val lastName: String?,
    val phoneNumber: String?,
    @SerializedName("birth_date")
    val birthDate: String?,
    val location: String?,
    val gender: String?,
    @SerializedName("profile_pic")
    val profilePic: String?,
    val uid: String?,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("updated_at")
    val updatedAt: String?
)

data class ProfileError(
    val field: String?,
    val message: String?,
)
