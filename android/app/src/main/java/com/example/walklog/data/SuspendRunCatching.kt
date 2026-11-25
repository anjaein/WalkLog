package com.example.walklog.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * suspend 함수용 runCatching. 네트워크 문제는 [NetworkException]으로 바꿔 실패로 돌려준다.
 * 기본 runCatching 은 CancellationException 까지 실패로 삼켜서 화면을 나가 취소된 코루틴이 멈추지 않으므로,
 * 이 코루틴 자체가 취소됐을 때만 CancellationException 을 다시 던진다.
 * 그 밖의 CancellationException(취소된 Task, block 안의 withTimeout 등)은 [IllegalStateException] 실패로 바꿔 돌려준다.
 * Error(OutOfMemoryError 등)는 잡지 않고 그대로 올려 보낸다.
 */
internal suspend fun <T> suspendRunCatching(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        // Play services · Firebase 가 Task 를 스스로 취소해도 CancellationException 이 온다. 이 코루틴이 살아 있으면 취소가 아니라 실패다.
        // 그대로 돌려주면 getOrThrow() 로 다시 던진 쪽이 아무 표시 없이 취소된 것처럼 끝나서, 일반 예외로 감싼다
        if (!currentCoroutineContext().isActive) throw e
        Result.failure(IllegalStateException("Task was cancelled", e))
    } catch (e: Exception) {
        Result.failure(if (e.isNetworkProblem()) NetworkException(e) else e)
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
