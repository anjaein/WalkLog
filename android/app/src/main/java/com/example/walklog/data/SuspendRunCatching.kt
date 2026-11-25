package com.example.walklog.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive

/**
 * suspend 함수용 runCatching. 실패는 모두 [toDataException]으로 바꿔 [DataException]으로 돌려준다.
 * 기본 runCatching 은 CancellationException 까지 실패로 삼켜서 화면을 나가 취소된 코루틴이 멈추지 않으므로,
 * 이 코루틴 자체가 취소됐으면 CancellationException 을 다시 던진다. 취소된 뒤 block 이 일반 예외로 끝나도 마찬가지다.
 * 그 밖의 CancellationException(취소된 Task, block 안의 withTimeout 등)은 원인(cause)이 있으면 그 원인을, 없으면 그 자체를
 * [toDataException]으로 바꿔 돌려준다(예: 네트워크 원인으로 취소된 Task 는 [NetworkException]).
 * Error(OutOfMemoryError 등)는 잡지 않고 그대로 올려 보낸다.
 */
internal suspend fun <T> suspendRunCatching(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        // Play services · Firebase 가 Task 를 스스로 취소해도 CancellationException 이 온다. 이 코루틴이 살아 있으면 취소가 아니라 실패다.
        // 그대로 돌려주면 getOrThrow() 로 다시 던진 쪽이 아무 표시 없이 취소된 것처럼 끝나서, 취소가 아닌 예외로 감싼다
        if (!currentCoroutineContext().isActive) throw e
        Result.failure(e.cancellationCause().toDataException())
    } catch (e: Exception) {
        // 취소 때 연결을 끊으면 block 은 IOException("Socket closed") 같은 일반 예외로 끝난다. 이것도 실패가 아니라 취소다
        currentCoroutineContext().ensureActive()
        Result.failure(e.toDataException())
    }
