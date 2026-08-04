package com.example.util

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * 100% Production-Grade Native Google Authentication Manager for Linko.
 *
 * Uses official Android CredentialManager and Google Identity Services SDK
 * to trigger the native OS Account Picker bottom sheet for accounts on the user's device.
 *
 * OAuth Client ID: 419727588321-vpr8ij7unfkrlpi10fb8ceb5q1g0tfj.apps.googleusercontent.com
 */
object GoogleAuthManager {

    const val GOOGLE_WEB_CLIENT_ID = "419727588321-vpr8ij7unfkrlpi10fb8ceb5q1g0tfj.apps.googleusercontent.com"
    private const val TAG = "GoogleAuthManager"
    private const val PREF_AUTH = "linko_auth_prefs"

    data class GoogleUserData(
        val email: String,
        val displayName: String,
        val username: String,
        val idToken: String?,
        val profilePictureUri: String?
    )

    /**
     * Triggers the native Android Credential Manager bottom sheet account picker.
     * Requests the physical device's logged-in Google Accounts.
     */
    suspend fun signInWithGoogle(
        context: Context,
        clientId: String = GOOGLE_WEB_CLIENT_ID
    ): Result<GoogleUserData> {
        return try {
            val credentialManager = CredentialManager.create(context)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(request = request, context = context)
            val credential = result.credential

            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

                val email = googleIdTokenCredential.id
                val name = googleIdTokenCredential.displayName
                    ?: googleIdTokenCredential.givenName
                    ?: email.substringBefore("@").replace(".", " ").capitalizeWords()
                val idToken = googleIdTokenCredential.idToken
                val profilePicture = googleIdTokenCredential.profilePictureUri?.toString()

                val baseUsername = email.substringBefore("@").replace(".", "_")
                val username = "${baseUsername}_google"

                val userData = GoogleUserData(
                    email = email,
                    displayName = name,
                    username = username,
                    idToken = idToken,
                    profilePictureUri = profilePicture
                )

                // Save session in SharedPreferences
                saveGoogleSession(context, userData)

                Result.success(userData)
            } else {
                Result.failure(Exception("Unsupported credential type returned: ${credential.type}"))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "User cancelled Google Account picker")
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Google Credential Manager error: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Google Sign-In: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun saveGoogleSession(context: Context, userData: GoogleUserData) {
        val prefs = context.getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("auth_provider", "google")
            .putString("google_email", userData.email)
            .putString("google_display_name", userData.displayName)
            .putString("google_username", userData.username)
            .putString("google_id_token", userData.idToken)
            .putString("google_avatar_uri", userData.profilePictureUri)
            .putLong("google_auth_timestamp", System.currentTimeMillis())
            .apply()
    }

    fun getSavedGoogleUser(context: Context): GoogleUserData? {
        val prefs = context.getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE)
        val email = prefs.getString("google_email", null) ?: return null
        val displayName = prefs.getString("google_display_name", email.substringBefore("@")) ?: email.substringBefore("@")
        val username = prefs.getString("google_username", email.substringBefore("@")) ?: email.substringBefore("@")
        val idToken = prefs.getString("google_id_token", null)
        val avatar = prefs.getString("google_avatar_uri", null)

        return GoogleUserData(
            email = email,
            displayName = displayName,
            username = username,
            idToken = idToken,
            profilePictureUri = avatar
        )
    }

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
}
