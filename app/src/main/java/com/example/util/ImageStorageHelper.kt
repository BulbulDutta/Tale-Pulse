package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageStorageHelper {
    private const val TAG = "ImageStorageHelper"

    /**
     * Saves a captured camera Bitmap directly into Linko's persistent local media storage (Linko/Media/Photos).
     * Returns a local file Uri (file:///...) that can be accessed and rendered permanently.
     */
    suspend fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
        val savedInfo = MediaDirectoryManager.saveBitmapClean(context, bitmap)
        savedInfo?.uri
    }

    /**
     * Copies a gallery content Uri (content://...) into Linko's persistent local media storage.
     * Automatically categorizes into Linko/Media/Photos, Videos, or Documents.
     */
    suspend fun saveUriToInternalStorage(context: Context, contentUri: Uri): Uri? = withContext(Dispatchers.IO) {
        try {
            if (contentUri.scheme == "file" && contentUri.path?.contains("Linko/Media") == true) {
                return@withContext contentUri
            }

            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(contentUri) ?: "application/octet-stream"
            val rawPath = contentUri.path ?: ""
            val fileName = rawPath.substringAfterLast("/").ifBlank { "Linko_Media_${System.currentTimeMillis()}" }
            val category = MediaCategory.fromMimeType(mimeType, fileName)

            val inputStream: InputStream? = contentResolver.openInputStream(contentUri)
            if (inputStream != null) {
                val savedInfo = MediaDirectoryManager.saveMediaFileClean(
                    context = context,
                    inputStream = inputStream,
                    originalFileName = if (fileName.contains(".")) fileName else "$fileName.${if (category == MediaCategory.PHOTOS) "jpg" else if (category == MediaCategory.VIDEOS) "mp4" else "pdf"}",
                    category = category
                )
                inputStream.close()
                savedInfo?.uri
            } else {
                Log.e(TAG, "InputStream was null for Uri: $contentUri")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving gallery Uri to internal storage", e)
            null
        }
    }

    /**
     * Simulates a secure cloud upload endpoint (e.g. Firebase Storage, AWS S3, Cloudinary).
     * Uploads the local file and yields the persistent public/local URI payload.
     */
    suspend fun uploadImagePayload(context: Context, inputUri: Uri): String = withContext(Dispatchers.IO) {
        val savedFileUri = saveUriToInternalStorage(context, inputUri)
        savedFileUri?.toString() ?: inputUri.toString()
    }

    /**
     * Downloads/saves attached documents and media files directly into the device Downloads directory
     * as well as Linko/Media storage folders.
     */
    suspend fun downloadMediaFile(context: Context, mediaUri: String, suggestedFileName: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val fileExt = when {
                mediaUri.contains(".pdf", ignoreCase = true) -> "pdf"
                mediaUri.contains(".m4a", ignoreCase = true) || mediaUri.contains(".mp3", ignoreCase = true) -> "m4a"
                mediaUri.contains(".mp4", ignoreCase = true) -> "mp4"
                else -> "jpg"
            }

            val fileName = suggestedFileName?.takeIf { it.isNotBlank() } ?: "Linko_Download_${System.currentTimeMillis()}.$fileExt"
            val targetFile = File(downloadsDir, fileName)

            val uri = Uri.parse(mediaUri)
            if (uri.scheme == "content" || uri.scheme == "file") {
                val inputStream = context.contentResolver.openInputStream(uri) ?: File(uri.path ?: "").inputStream()
                FileOutputStream(targetFile).use { out ->
                    inputStream.copyTo(out)
                    out.flush()
                }
                inputStream.close()
            } else {
                val connection = java.net.URL(mediaUri).openConnection()
                connection.inputStream.use { input ->
                    FileOutputStream(targetFile).use { out ->
                        input.copyTo(out)
                        out.flush()
                    }
                }
            }

            val category = when (fileExt) {
                "jpg", "png", "webp" -> MediaCategory.PHOTOS
                "mp4", "mkv" -> MediaCategory.VIDEOS
                else -> MediaCategory.DOCUMENTS
            }
            if (targetFile.exists()) {
                targetFile.inputStream().use { input ->
                    MediaDirectoryManager.saveMediaFileClean(context, input, fileName, category)
                }
            }

            Log.d(TAG, "File successfully downloaded to: ${targetFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading media file", e)
            false
        }
    }
}
