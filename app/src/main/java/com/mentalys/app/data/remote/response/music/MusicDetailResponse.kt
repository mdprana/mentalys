package com.mentalys.app.data.remote.response.music

import com.mentalys.app.data.local.entity.ImagesEntity
import com.mentalys.app.data.local.entity.MusicDetailEntity
import com.mentalys.app.data.local.entity.PreviewsEntity
import com.google.gson.annotations.SerializedName

data class MusicDetailResponse(
    val id: Int,
    val url: String? = null,
    val name: String? = null,
    val tags: List<String>? = null,
    val description: String?,
    val geotag: String?,
    val created: String? = null,
    val license: String? = null,
    val type: String? = null,
    val channels: Int? = null,
    val filesize: Int? = null,
    val bitrate: Int? = null,
    val bitdepth: Int? = null,
    val duration: Double? = null,
    val samplerate: Double? = null,
    val username: String? = null,
    val pack: String?,
    val pack_name: String?,
    val download: String? = null,
    val bookmark: String? = null,
    val previews: PreviewsResponse? = null,
    val images: ImagesResponse? = null,
    val num_downloads: Int? = null,
    val avg_rating: Double? = null,
    val num_ratings: Int? = null,
    val rate: String? = null,
    val comments: String? = null,
    val num_comments: Int? = null,
    val comment: String? = null,
    val similar_sounds: String? = null,
    val analysis: String? = null,
    val analysis_frames: String? = null,
    val analysis_stats: String? = null,
    val is_explicit: Boolean? = null
) {
    fun toEntity(): MusicDetailEntity {
        val previewEntity = previews?.toEntity() ?: PreviewsEntity("", "", "", "")
        val imageEntity = images?.toEntity() ?: ImagesEntity("", "", "", "", "", "", "", "")
        return MusicDetailEntity(
            id = id,
            url = url.orEmpty(),
            name = name.orEmpty(),
            tags = tags.orEmpty(),
            description = description,
            geotag = geotag,
            created = created.orEmpty(),
            license = license.orEmpty(),
            type = type.orEmpty(),
            channels = channels ?: 0,
            filesize = filesize ?: 0,
            bitrate = bitrate ?: 0,
            bitdepth = bitdepth ?: 0,
            duration = duration ?: 0.0,
            samplerate = samplerate ?: 0.0,
            username = username.orEmpty(),
            pack = pack,
            pack_name = pack_name,
            download = download.orEmpty(),
            bookmark = bookmark.orEmpty(),
            previews = previewEntity,
            images = imageEntity,
            num_downloads = num_downloads ?: 0,
            avg_rating = avg_rating ?: 0.0,
            num_ratings = num_ratings ?: 0,
            is_explicit = is_explicit ?: false,
            rate = rate.orEmpty(),
            comments = comments.orEmpty(),
            num_comments = num_comments ?: 0,
            comment = comment.orEmpty(),
            similar_sounds = similar_sounds.orEmpty(),
            analysis = analysis.orEmpty(),
            analysis_frames = analysis_frames.orEmpty(),
            analysis_stats = analysis_stats.orEmpty()
        )
    }
}

data class PreviewsResponse(
    @SerializedName("preview-hq-mp3") val previewHqMp3: String? = null,
    @SerializedName("preview-hq-ogg") val previewHqOgg: String? = null,
    @SerializedName("preview-lq-mp3") val previewLqMp3: String? = null,
    @SerializedName("preview-lq-ogg") val previewLqOgg: String? = null
) {
    fun toEntity(): PreviewsEntity {
        return PreviewsEntity(
            previewHqMp3 = previewHqMp3.orEmpty(),
            previewHqOgg = previewHqOgg.orEmpty(),
            previewLqMp3 = previewLqMp3.orEmpty(),
            previewLqOgg = previewLqOgg.orEmpty()
        )
    }
}

data class ImagesResponse(
    @SerializedName("waveform_m") val waveformM: String? = null,
    @SerializedName("waveform_l") val waveformL: String? = null,
    @SerializedName("spectral_m") val spectralM: String? = null,
    @SerializedName("spectral_l") val spectralL: String? = null,
    @SerializedName("waveform_bw_m") val waveformBwM: String? = null,
    @SerializedName("waveform_bw_l") val waveformBwL: String? = null,
    @SerializedName("spectral_bw_m") val spectralBwM: String? = null,
    @SerializedName("spectral_bw_l") val spectralBwL: String? = null
) {
    fun toEntity(): ImagesEntity {
        return ImagesEntity(
            waveformM = waveformM.orEmpty(),
            waveformL = waveformL.orEmpty(),
            spectralM = spectralM.orEmpty(),
            spectralL = spectralL.orEmpty(),
            waveformBwM = waveformBwM.orEmpty(),
            waveformBwL = waveformBwL.orEmpty(),
            spectralBwM = spectralBwM.orEmpty(),
            spectralBwL = spectralBwL.orEmpty()
        )
    }
}
