package com.example.walklog.data.auth

import com.example.walklog.data.mapDataErrors
import com.example.walklog.data.suspendRunCatching
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(private val auth: FirebaseAuth) : AuthRepository {

    override val currentUserId: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged().mapDataErrors()

    override fun currentUserIdNow(): String? = auth.currentUser?.uid

    override suspend fun signUpWithEmail(email: String, password: String): Result<String> = suspendRunCatching {
        signUpInputFailure(email, password)?.let { throw it }
        auth.createUserWithEmailAndPassword(email, password).await().uid()
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<String> = suspendRunCatching {
        signInInputFailure(email, password)?.let { throw it }
        auth.signInWithEmailAndPassword(email, password).await().uid()
    }

    override suspend fun signInWithGoogle(idToken: String): Result<String> = suspendRunCatching {
        googleTokenInputFailure(idToken)?.let { throw it }
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await().uid()
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = suspendRunCatching {
        passwordResetInputFailure(email)?.let { throw it }
        auth.sendPasswordResetEmail(email).await()
    }

    override fun signOut() {
        auth.signOut()
    }

    private fun AuthResult.uid(): String = checkNotNull(user?.uid) { "Auth result has no user" }
}
