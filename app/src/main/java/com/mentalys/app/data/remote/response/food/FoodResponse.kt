package com.mentalys.app.data.remote.response.food

import com.google.gson.annotations.SerializedName
import com.mentalys.app.data.local.entity.FoodEntity

data class FoodResponse(
    @SerializedName("data")
    val data: List<FoodDataResponse>? = null
)

data class FoodDataResponse(

    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("category")
    val category: String? = null,

    @SerializedName("imageUrl")
    val imageUrl: String? = null

) {
    fun toEntity(): FoodEntity {
        return FoodEntity(
            id = id,
            name = name.orEmpty(),
            description = description.orEmpty(),
            category = category.orEmpty(),
            imageUrl = imageUrl.orEmpty()
        )
    }
}
