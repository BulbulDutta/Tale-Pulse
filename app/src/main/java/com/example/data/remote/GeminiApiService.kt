package com.example.data.remote

import android.graphics.BitmapFactory
import android.util.Base64
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

object GeminiClient {

    private val generativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-3.8-flash")
    }

    suspend fun generateResponse(
        prompt: String,
        imageBase64: String? = null,
        mimeType: String = "image/jpeg"
    ): String = withContext(Dispatchers.IO) {
        try {
            val response = withTimeout(60_000L) {
                if (!imageBase64.isNullOrBlank()) {
                    val decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    if (bitmap != null) {
                        val inputContent = content {
                            image(bitmap)
                            text(prompt)
                        }
                        generativeModel.generateContent(inputContent)
                    } else {
                        generativeModel.generateContent(prompt)
                    }
                } else {
                    generativeModel.generateContent(prompt)
                }
            }
            val replyText = response.text
            if (!replyText.isNullOrBlank()) {
                replyText
            } else {
                "Gemini did not return text for this prompt."
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            "Gemini AI Assistant Error: ${e.localizedMessage ?: "Failed to reach Gemini API."}"
        }
    }
}
