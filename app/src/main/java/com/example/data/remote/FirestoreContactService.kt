package com.example.data.remote

import android.util.Log
import com.example.data.local.entity.ContactEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FirestoreContactService {
    private const val TAG = "FirestoreContactService"
    private const val COLLECTION_CONTACTS = "user_contacts"

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (_: Throwable) {
            null
        }
    }

    private var activeSyncListener: ListenerRegistration? = null

    /**
     * Upload or update a single contact to Firebase Firestore.
     */
    fun uploadContact(contact: ContactEntity) {
        val db = getFirestore() ?: return
        try {
            val contactData = mapOf(
                "id" to contact.id,
                "userEmail" to contact.userEmail.lowercase().trim(),
                "contactUserId" to contact.contactUserId,
                "contactEmail" to contact.contactEmail,
                "contactDisplayName" to contact.contactDisplayName,
                "contactUsername" to contact.contactUsername,
                "contactStatus" to contact.contactStatus,
                "contactAvatarUri" to contact.contactAvatarUri,
                "updatedAt" to System.currentTimeMillis()
            )

            // Doc ID format: ownerEmail_contactId
            val docId = "${contact.userEmail.lowercase().trim()}_${contact.id}"
            db.collection(COLLECTION_CONTACTS)
                .document(docId)
                .set(contactData)
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully synced contact ${contact.contactDisplayName} to Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error syncing contact to Firestore: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during uploadContact: ${e.message}")
        }
    }

    /**
     * Upload a batch of contacts to Firestore (e.g., initial local contacts).
     */
    fun uploadContactsBatch(contacts: List<ContactEntity>) {
        contacts.forEach { uploadContact(it) }
    }

    /**
     * Fetch existing contacts from Firestore for the user and save them locally.
     */
    fun fetchAndSyncContacts(
        ownerEmail: String,
        scope: CoroutineScope,
        onContactsReceived: suspend (List<ContactEntity>) -> Unit
    ) {
        val db = getFirestore() ?: return
        val normalizedEmail = ownerEmail.lowercase().trim()
        if (normalizedEmail.isBlank()) return

        try {
            db.collection(COLLECTION_CONTACTS)
                .whereEqualTo("userEmail", normalizedEmail)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot != null && !snapshot.isEmpty) {
                        val contacts = snapshot.documents.mapNotNull { doc ->
                            val id = doc.getString("id") ?: return@mapNotNull null
                            val userEmail = doc.getString("userEmail") ?: normalizedEmail
                            val contactUserId = doc.getString("contactUserId") ?: ""
                            val contactEmail = doc.getString("contactEmail") ?: ""
                            val contactDisplayName = doc.getString("contactDisplayName") ?: ""
                            val contactUsername = doc.getString("contactUsername") ?: ""
                            val contactStatus = doc.getString("contactStatus") ?: ""
                            val contactAvatarUri = doc.getString("contactAvatarUri")

                            ContactEntity(
                                id = id,
                                userEmail = userEmail,
                                contactUserId = contactUserId,
                                contactEmail = contactEmail,
                                contactDisplayName = contactDisplayName,
                                contactUsername = contactUsername,
                                contactStatus = contactStatus,
                                contactAvatarUri = contactAvatarUri
                            )
                        }

                        scope.launch(Dispatchers.IO) {
                            onContactsReceived(contacts)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to fetch contacts from Firestore: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during fetchAndSyncContacts: ${e.message}")
        }
    }

    /**
     * Start real-time Firestore listener to keep contacts synced across devices.
     */
    fun startRealtimeContactSync(
        ownerEmail: String,
        scope: CoroutineScope,
        onContactsReceived: suspend (List<ContactEntity>) -> Unit
    ) {
        activeSyncListener?.remove()

        val db = getFirestore() ?: return
        val normalizedEmail = ownerEmail.lowercase().trim()
        if (normalizedEmail.isBlank()) return

        try {
            activeSyncListener = db.collection(COLLECTION_CONTACTS)
                .whereEqualTo("userEmail", normalizedEmail)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Firestore contact listener error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val contacts = snapshot.documents.mapNotNull { doc ->
                            val id = doc.getString("id") ?: return@mapNotNull null
                            val userEmail = doc.getString("userEmail") ?: normalizedEmail
                            val contactUserId = doc.getString("contactUserId") ?: ""
                            val contactEmail = doc.getString("contactEmail") ?: ""
                            val contactDisplayName = doc.getString("contactDisplayName") ?: ""
                            val contactUsername = doc.getString("contactUsername") ?: ""
                            val contactStatus = doc.getString("contactStatus") ?: ""
                            val contactAvatarUri = doc.getString("contactAvatarUri")

                            ContactEntity(
                                id = id,
                                userEmail = userEmail,
                                contactUserId = contactUserId,
                                contactEmail = contactEmail,
                                contactDisplayName = contactDisplayName,
                                contactUsername = contactUsername,
                                contactStatus = contactStatus,
                                contactAvatarUri = contactAvatarUri
                            )
                        }

                        scope.launch(Dispatchers.IO) {
                            onContactsReceived(contacts)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting realtime contact sync: ${e.message}")
        }
    }

    fun stopRealtimeSync() {
        activeSyncListener?.remove()
        activeSyncListener = null
    }
}
