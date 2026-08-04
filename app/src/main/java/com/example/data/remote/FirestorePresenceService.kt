package com.example.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FirestorePresenceService {
    private const val TAG = "FirestorePresence"
    private const val COLLECTION_PRESENCE = "user_presence"

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (_: Throwable) {
            null
        }
    }

    private var activePresenceListener: ListenerRegistration? = null

    fun updatePresence(email: String, userId: String, isOnline: Boolean) {
        val db = getFirestore() ?: return
        val normalizedEmail = email.lowercase().trim()
        if (normalizedEmail.isBlank()) return

        try {
            val data = mapOf(
                "email" to normalizedEmail,
                "userId" to userId,
                "isOnline" to isOnline,
                "lastSeen" to System.currentTimeMillis()
            )
            db.collection(COLLECTION_PRESENCE)
                .document(normalizedEmail)
                .set(data)
                .addOnSuccessListener {
                    Log.d(TAG, "Presence updated for $normalizedEmail: isOnline=$isOnline")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed updating presence for $normalizedEmail: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in updatePresence: ${e.message}")
        }
    }

    fun startListeningToPresence(
        scope: CoroutineScope,
        onPresenceUpdate: (Set<String>) -> Unit
    ) {
        activePresenceListener?.remove()
        val db = getFirestore() ?: return

        try {
            activePresenceListener = db.collection(COLLECTION_PRESENCE)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Presence listener error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val onlineEmails = mutableSetOf<String>()
                        // Gemini AI is always online
                        onlineEmails.add("gemini_ai@google.com")
                        onlineEmails.add("gemini.ai@google.com")

                        val now = System.currentTimeMillis()
                        for (doc in snapshot.documents) {
                            val email = doc.getString("email")?.lowercase()?.trim() ?: continue
                            val isOnline = doc.getBoolean("isOnline") ?: false
                            val lastSeen = doc.getLong("lastSeen") ?: 0L

                            // Online ONLY if isOnline is true and lastSeen within last 5 minutes
                            if (isOnline && (now - lastSeen) < 5 * 60 * 1000L) {
                                onlineEmails.add(email)
                            }
                        }

                        scope.launch(Dispatchers.Main) {
                            onPresenceUpdate(onlineEmails)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in startListeningToPresence: ${e.message}")
        }
    }

    fun stopListening() {
        activePresenceListener?.remove()
        activePresenceListener = null
    }
}
