package com.example.util

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log

object AudioPlayerManager {
    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlayingUri: String? = null

    fun playAudio(
        context: Context,
        audioUri: String,
        onCompletion: () -> Unit
    ) {
        try {
            if (currentlyPlayingUri == audioUri && mediaPlayer?.isPlaying == true) {
                pauseAudio()
                return
            }

            stopAudio()

            val player = MediaPlayer()
            val uri = Uri.parse(audioUri)
            if (audioUri.startsWith("content://") || audioUri.startsWith("file://")) {
                player.setDataSource(context, uri)
            } else if (audioUri.startsWith("/")) {
                player.setDataSource(audioUri)
            } else {
                player.setDataSource(context, uri)
            }

            player.setOnCompletionListener {
                currentlyPlayingUri = null
                onCompletion()
            }

            player.prepare()
            player.start()
            mediaPlayer = player
            currentlyPlayingUri = audioUri
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error playing audio file: $audioUri", e)
            stopAudio()
            onCompletion()
        }
    }

    fun pauseAudio() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (_: Exception) {}
    }

    fun isPlaying(audioUri: String): Boolean {
        return currentlyPlayingUri == audioUri && mediaPlayer?.isPlaying == true
    }

    fun stopAudio() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
        currentlyPlayingUri = null
    }
}
