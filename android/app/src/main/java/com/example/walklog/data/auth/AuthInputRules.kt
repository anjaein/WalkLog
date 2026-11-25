package com.example.walklog.data.auth

import com.example.walklog.data.DataException
import com.example.walklog.data.InvalidCredentialsException
import com.example.walklog.data.InvalidEmailException
import com.example.walklog.data.WeakPasswordException

// firebase-auth 는 빈 이메일 · 비밀번호 · 토큰을 서버에 묻기 전에 IllegalArgumentException 으로 던져서 종류를 알 수 없게 되므로, 부르기 전에 거른다

internal fun signUpInputFailure(email: String, password: String): DataException? = when {
    email.isBlank() -> InvalidEmailException()
    password.isBlank() -> WeakPasswordException()
    else -> null
}

internal fun signInInputFailure(email: String, password: String): DataException? = when {
    email.isBlank() -> InvalidEmailException()
    password.isBlank() -> InvalidCredentialsException()
    else -> null
}

internal fun passwordResetInputFailure(email: String): DataException? =
    if (email.isBlank()) InvalidEmailException() else null

internal fun googleTokenInputFailure(idToken: String): DataException? =
    if (idToken.isBlank()) InvalidCredentialsException() else null
