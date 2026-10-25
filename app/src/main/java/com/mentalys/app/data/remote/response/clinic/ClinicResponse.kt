package com.mentalys.app.data.remote.response.clinic

import com.google.gson.annotations.SerializedName
import com.mentalys.app.data.local.entity.ClinicEntity

data class ClinicResponse(
    val status: String? = null,
    val total: Int? = null,
    val data: List<PlaceDetails>? = null
)

data class ClinicDetailResponse(
    val status: String? = null,
    val data: PlaceDetails? = null
)

data class PlaceDetails(
    @SerializedName("business_status")
    val businessStatus: String? = null,
    val geometry: Geometry? = null,
    val icon: String? = null,
    @SerializedName("icon_background_color")
    val iconBackgroundColor: String? = null,
    @SerializedName("icon_mask_base_uri")
    val iconMaskBaseUri: String? = null,
    val name: String,

    @SerializedName("opening_hours")
    val openingHours: OpeningHours? = null,


    val photos: List<Photo>? = null,
    @SerializedName("place_id")
    val placeId: String? = null,
    @SerializedName("plus_code")
    val plusCode: PlusCode? = null,
    val rating: Double? = null,
    val reference: String? = null,
    val scope: String? = null,
    val types: List<String>? = null,
    @SerializedName("user_ratings_total")
    val userRatingsTotal: Int? = null,
    val vicinity: String? = null,
    @SerializedName("formatted_address")
    val formattedAddress: String? = null,
    @SerializedName("formatted_phone_number")
    val formattedPhoneNumber: String? = null,
    val website: String? = null,
    val photoUrl: String? = null
) {
    fun toClinicEntity(): ClinicEntity {
        return ClinicEntity(
            name = this.name,
            vicinity = this.vicinity ?: this.formattedAddress.orEmpty(),
            openNow = this.openingHours?.openNow ?: false,
            reference = this.placeId ?: this.reference.orEmpty(),
            rating = this.rating ?: 0.0,
            photoUrl = this.photoUrl.orEmpty()
        )
    }
}

data class Geometry(
    val location: Location? = null,
    val viewport: Viewport? = null
)

data class Location(
    val lat: Double? = null,
    val lng: Double? = null
)

data class Viewport(
    val northeast: Location? = null,
    val southwest: Location? = null
)

data class OpeningHours(
    @SerializedName("open_now")
    val openNow: Boolean? = null,
    @SerializedName("weekday_text")
    val weekdayText: List<String>? = null
)

data class PlusCode(
    val compoundCode: String? = null,
    val globalCode: String? = null
)

data class Photo(
    val height: Int? = null,
    @SerializedName("html_attributions")
    val htmlAttributions: List<String>? = null,
    @SerializedName("photo_reference")
    val photoReference: String? = null,
    val width: Int? = null
)
