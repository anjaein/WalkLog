package com.example.walklog.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** 외부 SDK 예외를 화면이 다룰 [DataException]으로 바꾼다. 취소([CancellationException])는 여기 넘기지 말고 다시 던진다. */
internal fun Throwable.toDataException(): DataException {
    if (this is DataException) return this
    if (isNetworkProblem()) return NetworkException(this)
    val mapped = when (this) {
        is FirebaseTooManyRequestsException -> TooManyRequestsException(this)
        is FirebaseAuthException -> authFailureFor(errorCode, this) ?: authFailureByType(this)
        is FirebaseFirestoreException -> firestoreFailureFor(code.name, this)
        is StorageException -> storageFailureFor(errorCode, this)
        else -> null
    }
    return mapped ?: UnknownDataException(this)
}

/**
 * Flow 오류는 Result 를 거치지 않고 collect 하는 화면으로 바로 가므로, 여기서도 DataException 으로 바꿔야 화면이 SDK 예외를 안 본다.
 * 수집하는 쪽이 취소됐으면 무슨 예외로 끝났든 취소로 올려 보내고, Error(OutOfMemoryError 등)는 [suspendRunCatching]처럼 바꾸지 않는다.
 */
internal fun <T> Flow<T>.mapDataErrors(): Flow<T> = catch { e ->
    // 취소 때 연결을 끊으면 위쪽 흐름은 IOException 같은 일반 예외로 끝난다. 이것도 실패가 아니라 취소다
    currentCoroutineContext().ensureActive()
    if (e !is Exception) throw e
    if (e is CancellationException) {
        // Firestore snapshots() 는 리스너 오류를 cancel(cause = 진짜 오류)로 알려서, 수집하는 쪽은 살아 있는데 CancellationException 을 받는다.
        // 그대로 올리면 화면은 아무 표시 없이 멈추므로, 원인으로 실패를 알린다
        val cause = e.cancellationCause()
        if (cause !is Exception) throw cause
        throw cause.toDataException()
    }
    throw e.toDataException()
}

/**
 * 취소가 아닌데 CancellationException 으로 온 실패의 진짜 원인. 원인이 없으면 자기 자신.
 * 코루틴 디버그 모드(테스트의 -ea 등)는 스택 복구용으로 CancellationException 을 한 겹 더 감싸므로, 사슬을 따라가 취소가 아닌 첫 예외를 찾는다.
 */
internal fun CancellationException.cancellationCause(): Throwable =
    generateSequence<Throwable>(this) { it.cause }.take(MAX_CAUSE_DEPTH).firstOrNull { it !is CancellationException } ?: this

// Firebase 예외 객체는 JVM 단위 테스트에서 만들 수 없어서, 종류 판단은 오류 코드만 받는 함수로 떼어 테스트한다
internal fun authFailureFor(errorCode: String, cause: Throwable): DataException? = when (errorCode) {
    "ERROR_INVALID_EMAIL", "ERROR_MISSING_EMAIL" -> InvalidEmailException(cause)
    // 없는 계정인지 비밀번호가 틀린 건지 구분해 알려 주면 가입된 이메일을 알아낼 수 있으므로 같은 종류로 묶는다
    "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND", "ERROR_INVALID_CREDENTIAL",
    "ERROR_USER_DISABLED", "ERROR_MISSING_PASSWORD",
    -> InvalidCredentialsException(cause)
    "ERROR_EMAIL_ALREADY_IN_USE", "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL", "ERROR_CREDENTIAL_ALREADY_IN_USE",
    -> EmailAlreadyInUseException(cause)
    "ERROR_WEAK_PASSWORD", "ERROR_PASSWORD_DOES_NOT_MEET_REQUIREMENTS" -> WeakPasswordException(cause)
    "ERROR_USER_TOKEN_EXPIRED", "ERROR_INVALID_USER_TOKEN" -> PermissionDeniedException("Sign-in expired", cause)
    "ERROR_TOO_MANY_REQUESTS" -> TooManyRequestsException(cause)
    else -> null
}

// 코드 문자열은 SDK 버전마다 늘어나서, 모르는 코드라도 예외 클래스로 종류를 한 번 더 본다
private fun authFailureByType(e: FirebaseAuthException): DataException? = when (e) {
    // WeakPassword 는 InvalidCredentials 의 하위 클래스라서 먼저 봐야 한다
    is FirebaseAuthWeakPasswordException -> WeakPasswordException(e)
    is FirebaseAuthInvalidCredentialsException -> InvalidCredentialsException(e)
    is FirebaseAuthInvalidUserException -> InvalidCredentialsException(e)
    is FirebaseAuthUserCollisionException -> EmailAlreadyInUseException(e)
    else -> null
}

/** [codeName]은 FirebaseFirestoreException.Code 의 이름. */
internal fun firestoreFailureFor(codeName: String, cause: Throwable): DataException? = when (codeName) {
    "PERMISSION_DENIED", "UNAUTHENTICATED" -> PermissionDeniedException(cause = cause)
    "NOT_FOUND" -> NotFoundException(cause = cause)
    "RESOURCE_EXHAUSTED" -> TooManyRequestsException(cause)
    else -> null
}

internal fun storageFailureFor(errorCode: Int, cause: Throwable): DataException? = when (errorCode) {
    StorageException.ERROR_NOT_AUTHORIZED, StorageException.ERROR_NOT_AUTHENTICATED -> PermissionDeniedException(cause = cause)
    StorageException.ERROR_OBJECT_NOT_FOUND -> NotFoundException(cause = cause)
    else -> null
}

// Firestore · Storage 는 연결이 끊기면 자기 예외(UNAVAILABLE, 재시도 한도 초과)로 감싸서 주므로, 원인 사슬까지 따라가며 본다
private fun Throwable.isNetworkProblem(): Boolean =
    generateSequence(this) { it.cause }.take(MAX_CAUSE_DEPTH).any { it.isNetworkProblemItself() }

private fun Throwable.isNetworkProblemItself(): Boolean =
    this is FirebaseNetworkException ||
        this is UnknownHostException ||
        this is SocketTimeoutException ||
        this is ConnectException ||
        (this is FirebaseFirestoreException && (code == FirebaseFirestoreException.Code.UNAVAILABLE || code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED)) ||
        (this is StorageException && errorCode == StorageException.ERROR_RETRY_LIMIT_EXCEEDED)

private const val MAX_CAUSE_DEPTH = 8
