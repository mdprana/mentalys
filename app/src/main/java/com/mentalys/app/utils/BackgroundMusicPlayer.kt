package com.mentalys.app.utils

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

object BackgroundMusicPlayer {
    private var player: ExoPlayer? = null
    private var currentUrl: String? = null
    private var lastPositionMs: Long = 0L
    private var pausedForPreview: Boolean = false
    private var pausedForRecording: Boolean = false
    private var onTrackEnded: (() -> Unit)? = null

    fun play(context: Context, previewUrl: String, onEnded: (() -> Unit)? = null) {
        if (previewUrl.isBlank()) return

        val appContext = context.applicationContext
        onTrackEnded = onEnded
        val existing = player
        if (existing != null && currentUrl == previewUrl) {
            if (!existing.isPlaying) existing.play()
            return
        }

        val previousUrl = currentUrl
        stop()
        if (previousUrl != previewUrl) {
            lastPositionMs = 0L
        }
        currentUrl = previewUrl
        player = ExoPlayer.Builder(appContext).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            volume = 1f
            setMediaItem(MediaItem.fromUri(previewUrl))
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        onTrackEnded?.invoke()
                    }
                }
            })
            prepare()
            if (lastPositionMs > 0L) {
                seekTo(lastPositionMs)
            }
            playWhenReady = true
        }
    }

    fun stop() {
        player?.let {
            lastPositionMs = it.currentPosition
            it.release()
        }
        player = null
        pausedForPreview = false
        pausedForRecording = false
    }

    fun currentPreviewUrl(): String? = currentUrl

    /**
     * True when there is an active player (playing OR paused for preview/recording).
     * Used by callers to avoid replacing a track that is already loaded just because
     * the host Activity was recreated or a new screen was opened.
     */
    fun hasActiveTrack(): Boolean = player != null && currentUrl != null

    fun reset() {
        stop()
        currentUrl = null
        lastPositionMs = 0L
    }

    fun pauseForPreview() {
        val existing = player ?: return
        if (existing.isPlaying) {
            pausedForPreview = true
            existing.pause()
        }
    }

    fun resumeAfterPreview() {
        val existing = player ?: return
        if (pausedForPreview) {
            pausedForPreview = false
            existing.play()
        }
    }

    fun pauseForRecording() {
        val existing = player ?: return
        if (existing.isPlaying) {
            pausedForRecording = true
            existing.pause()
        }
    }

    fun resumeAfterRecording() {
        val existing = player ?: return
        if (pausedForRecording) {
            pausedForRecording = false
            existing.play()
        }
    }
}
