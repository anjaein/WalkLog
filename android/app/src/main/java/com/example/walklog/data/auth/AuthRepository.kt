package com.example.walklog.data.auth

import com.example.walklog.data.DataException
import com.example.walklog.data.EmailAlreadyInUseException
import com.example.walklog.data.InvalidCredentialsException
import com.example.walklog.data.InvalidEmailException
import com.example.walklog.data.NetworkException
import com.example.walklog.data.TooManyRequestsException
import com.example.walklog.data.UnknownDataException
import com.example.walklog.data.WeakPasswordException
import kotlinx.coroutines.flow.Flow

/**
 * 모든 실패(Result · Flow 오류)는 [DataException]이다. 아래 함수마다 적은 것 말고도
 * 네트워크가 안 되면 [NetworkException], 너무 자주 시도해 막히면 [TooManyRequestsException],
 * 그 밖은 [UnknownDataException](원래 예외는 cause)으로 실패한다.
 */
interface AuthRepository {
    /** 로그인한 사용자 uid. 로그아웃 상태면 null. 로그인 · 로그아웃 때마다 새 값이 온다. */
    val currentUserId: Flow<String?>

    fun currentUserIdNow(): String?

    /**
     * 이메일 형식이 틀렸거나 비었으면 [InvalidEmailException], 이미 가입된 이메일이면 [EmailAlreadyInUseException],
     * 비밀번호가 약하거나 비었으면 [WeakPasswordException].
     */
    suspend fun signUpWithEmail(email: String, password: String): Result<String>

    /** 이메일 형식이 틀렸거나 비었으면 [InvalidEmailException], 비밀번호가 틀렸거나 비었거나 없는 · 막힌 계정이면 [InvalidCredentialsException]. */
    suspend fun signInWithEmail(email: String, password: String): Result<String>

    /**
     * Google ID 토큰으로 로그인한다. 토큰을 받는 Credential Manager 단계도 화면에서 직접 부르지 않고
     * data 쪽에 감싸야 한다(AGENTS.md). 그 부분은 온보딩 작업에서 이어서 만든다.
     * 토큰이 비었거나 잘못됐거나 막힌 계정이면 [InvalidCredentialsException], 같은 이메일이 다른 방식으로 가입돼 있으면 [EmailAlreadyInUseException].
     */
    suspend fun signInWithGoogle(idToken: String): Result<String>

    /**
     * 이메일 형식이 틀렸거나 비었으면 [InvalidEmailException].
     * 없는 계정이면 [InvalidCredentialsException]이 올 수 있다(이메일 열거 보호 설정에 따라 성공으로 끝나기도 한다).
     */
    suspend fun sendPasswordReset(email: String): Result<Unit>

    fun signOut()
}
