package com.mentalys.app.data.remote.response.psychiatrist

data class PsychiatristContactResponse(
    val placeId: String? = null,
    val name: String? = null,
    val address: String? = null,
    val rating: Any? = null,
    val userRatingsTotal: Any? = null,
    val location: PsychiatristLocationResponse? = null,
    val phoneNumber: String? = null,
    val website: String? = null,
    val openingHours: Any? = null,
    val photoUrl: String? = null
)

data class PsychiatristLocationResponse(
    val lat: Double? = null,
    val lng: Double? = null
)
