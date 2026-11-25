package com.example.walklog.data.auth

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** 로그인한 사용자 uid. 로그아웃 상태면 null. 로그인 · 로그아웃 때마다 새 값이 온다. */
    val currentUserId: Flow<String?>

    fun currentUserIdNow(): String?

    suspend fun signUpWithEmail(email: String, password: String): Result<String>

    suspend fun signInWithEmail(email: String, password: String): Result<String>

    /** Credential Manager로 받은 Google ID 토큰으로 로그인한다. 토큰을 받는 화면 코드는 온보딩 담당이 만든다. */
    suspend fun signInWithGoogle(idToken: String): Result<String>

    suspend fun sendPasswordReset(email: String): Result<Unit>

    fun signOut()
}
