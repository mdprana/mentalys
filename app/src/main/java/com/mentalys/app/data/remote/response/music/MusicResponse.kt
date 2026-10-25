package com.mentalys.app.data.remote.response.music

import com.google.gson.annotations.SerializedName
import com.mentalys.app.data.local.entity.MusicItemEntity

data class MusicResponse(
    val count: Int,
    val previous: String?,
    val next: String?,
    val results: List<MusicItemResponse>
)

data class MusicItemResponse(
    val id: Int,
    val name: String?,
    val tags: List<String?>,
    val license: String?,
    val username: String?,
    val created: String? = null,
    val url: String? = null,
    val previews: MusicItemPreviewsResponse? = null
) {
    fun toEntity(): MusicItemEntity {
        return MusicItemEntity(
            id = id,
            name = name,
            tags = tags,
            license = license,
            username = username,
        )
    }
}

data class MusicItemPreviewsResponse(
    @SerializedName("preview-hq-mp3") val previewHqMp3: String? = null
)
