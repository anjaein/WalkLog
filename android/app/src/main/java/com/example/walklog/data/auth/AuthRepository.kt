package com.example.walklog.data.auth

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** 로그인한 사용자 uid. 로그아웃 상태면 null. 로그인 · 로그아웃 때마다 새 값이 온다. */
    val currentUserId: Flow<String?>

    fun currentUserIdNow(): String?

    suspend fun signUpWithEmail(email: String, password: String): Result<String>

    suspend fun signInWithEmail(email: String, password: String): Result<String>

    /**
     * Google ID 토큰으로 로그인한다. 토큰을 받는 Credential Manager 단계도 화면에서 직접 부르지 않고
     * data 쪽에 감싸야 한다(AGENTS.md). 그 부분은 온보딩 작업에서 이어서 만든다.
     */
    suspend fun signInWithGoogle(idToken: String): Result<String>

    suspend fun sendPasswordReset(email: String): Result<Unit>

    fun signOut()
}
