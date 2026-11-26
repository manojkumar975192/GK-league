package com.example.data

import android.app.Activity
import android.content.Context
import android.util.Log
import android.util.Patterns
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

fun isValidEmailAddress(email: String): Boolean {
    val trimmed = email.trim()
    return trimmed.length in 5..120 && Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()
}

private fun mapFirebaseAuthError(error: Exception): String {
    val code = (error as? FirebaseAuthException)?.errorCode.orEmpty()
    val rawMessage = error.message.orEmpty()
    return when {
        code == "ERROR_OPERATION_NOT_ALLOWED" ||
            rawMessage.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ||
            rawMessage.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ->
            "Firebase Console -> Authentication -> Sign-in method பகுதியில் Email/Password வசதியை இயக்கவும். அல்லது கீழே உள்ள Google உள்நுழைவைப் பயன்படுத்தவும்."

        code == "ERROR_INVALID_EMAIL" ->
            "சரியான மின்னஞ்சல் முகவரியை உள்ளிடவும்."

        code == "ERROR_USER_NOT_FOUND" ||
            code == "ERROR_WRONG_PASSWORD" ||
            code == "ERROR_INVALID_CREDENTIAL" ->
            "மின்னஞ்சல் அல்லது கடவுச்சொல் தவறானது. மீண்டும் சரிபார்க்கவும்."

        code == "ERROR_EMAIL_ALREADY_IN_USE" ->
            "இந்த மின்னஞ்சல் ஏற்கனவே பதிவு செய்யப்பட்டுள்ளது. உள்நுழைவுப் பக்கத்திற்குச் செல்லவும்."

        code == "ERROR_WEAK_PASSWORD" ->
            "கடவுச்சொல் குறைந்தது 6 எழுத்துகள் கொண்டதாக இருக்க வேண்டும்."

        code == "ERROR_USER_DISABLED" ->
            "இந்த மாணவர் கணக்கு தற்காலிகமாக முடக்கப்பட்டுள்ளது."

        code == "ERROR_TOO_MANY_REQUESTS" ->
            "அதிக முயற்சிகள் செய்யப்பட்டுள்ளன. சற்று நேரம் கழித்து மீண்டும் முயற்சிக்கவும்."

        code == "ERROR_NETWORK_REQUEST_FAILED" ->
            "இணைய இணைப்பைச் சரிபார்த்து மீண்டும் முயற்சிக்கவும்."

        else -> error.localizedMessage ?: "அங்கீகாரப் பிழை ஏற்பட்டது. மீண்டும் முயற்சிக்கவும்."
    }
}

fun signInWithEmailPassword(
    email: String,
    password: String,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope
) {
    val cleanEmail = email.trim()
    if (!isValidEmailAddress(cleanEmail)) {
        onAuthError("சரியான மின்னஞ்சல் முகவரியை உள்ளிடவும்.")
        return
    }
    if (password.length < 6) {
        onAuthError("கடவுச்சொல் குறைந்தது 6 எழுத்துகள் இருக்க வேண்டும்.")
        return
    }

    scope.launch {
        try {
            Firebase.auth.signInWithEmailAndPassword(cleanEmail, password).await()
            onAuthSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("Auth", "Email sign-in failed: ${(e as? FirebaseAuthException)?.errorCode ?: e.javaClass.simpleName}")
            onAuthError(mapFirebaseAuthError(e))
        }
    }
}

fun registerWithEmailPassword(
    fullName: String,
    email: String,
    password: String,
    confirmPassword: String,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope
) {
    val cleanName = fullName.trim().take(80)
    val cleanEmail = email.trim()

    if (cleanName.length < 2) {
        onAuthError("மாணவர் முழுப் பெயரை உள்ளிடவும் (குறைந்தது 2 எழுத்துகள்).")
        return
    }
    if (!isValidEmailAddress(cleanEmail)) {
        onAuthError("சரியான மின்னஞ்சல் முகவரியை உள்ளிடவும்.")
        return
    }
    if (password.length < 6) {
        onAuthError("கடவுச்சொல் குறைந்தது 6 எழுத்துகள் கொண்டதாக இருக்க வேண்டும்.")
        return
    }
    if (password != confirmPassword) {
        onAuthError("கடவுச்சொல் மற்றும் உறுதிப்படுத்தல் கடவுச்சொல் பொருந்தவில்லை.")
        return
    }

    scope.launch {
        try {
            val result = Firebase.auth.createUserWithEmailAndPassword(cleanEmail, password).await()
            val user = result.user
            if (user != null) {
                try {
                    val profileUpdates = userProfileChangeRequest {
                        displayName = cleanName
                    }
                    user.updateProfile(profileUpdates).await()
                } catch (profileErr: Exception) {
                    Log.w("Auth", "Profile displayName update skipped: ${profileErr.javaClass.simpleName}")
                }
            }
            onAuthSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("Auth", "Email registration failed: ${(e as? FirebaseAuthException)?.errorCode ?: e.javaClass.simpleName}")
            onAuthError(mapFirebaseAuthError(e))
        }
    }
}

fun sendPasswordReset(
    email: String,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit,
    scope: CoroutineScope
) {
    val cleanEmail = email.trim()
    if (!isValidEmailAddress(cleanEmail)) {
        onError("கடவுச்சொல் மீட்டமைப்பிற்கு சரியான மின்னஞ்சலை முதலில் உள்ளிடவும்.")
        return
    }

    scope.launch {
        try {
            Firebase.auth.sendPasswordResetEmail(cleanEmail).await()
            onSuccess("கடவுச்சொல் மீட்டமைப்பு இணைப்பு $cleanEmail முகவரிக்கு அனுப்பப்பட்டது.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("Auth", "Password reset failed: ${(e as? FirebaseAuthException)?.errorCode ?: e.javaClass.simpleName}")
            onError(mapFirebaseAuthError(e))
        }
    }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val activity = context as? Activity
            val cred = credentialManager.getCredential(activity ?: context, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(
                    GoogleAuthProvider.getCredential(token, null)
                ).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google உள்நுழைவு கட்டமைப்பு கிடைக்கவில்லை.")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val activity = context as? Activity
                ?: throw IllegalStateException("Google உள்நுழைவிற்கு Activity சூழல் தேவை.")
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(
                    GoogleAuthProvider.getCredential(token, null)
                ).await()
                onAuthSuccess()
            } else {
                onAuthError("எதிர்பாராத கணக்குத் தகவல் பெறப்பட்டது. மீண்டும் முயற்சிக்கவும்.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "உள்நுழைவு தோல்வியடைந்தது. மீண்டும் முயற்சிக்கவும்.")
        }
    }
}

fun signOutUser(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}
