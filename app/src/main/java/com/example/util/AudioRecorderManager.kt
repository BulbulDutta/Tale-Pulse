package com.example.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class AudioRecorderManager(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null

    fun startRecording(): File? {
        stopRecording()
        return try {
            val audioDir = File(context.cacheDir, "linko_audio")
            if (!audioDir.exists()) {
                audioDir.mkdirs()
            }

            val file = File(audioDir, "voice_note_${System.currentTimeMillis()}.m4a")
            currentOutputFile = file

            @Suppress("DEPRECATION")
            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }

            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            Log.d("AudioRecorderManager", "Recording started successfully: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Error starting media recorder", e)
            try {
                recorder?.release()
            } catch (_: Exception) {}
            recorder = null
            currentOutputFile = null
            null
        }
    }

    fun stopRecording(): File? {
        return try {
            recorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    Log.e("AudioRecorderManager", "Stop called on recorder error", e)
                }
                release()
            }
            recorder = null
            val recordedFile = currentOutputFile
            currentOutputFile = null
            recordedFile
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Error stopping recorder", e)
            try {
                recorder?.release()
            } catch (_: Exception) {}
            recorder = null
            null
        }
    }
}
