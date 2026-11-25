package com.example.walklog.data.log

import com.example.walklog.data.PermissionDeniedException
import com.example.walklog.data.suspendRunCatching
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// deleteLog 는 suspendRunCatching { finishEvenIfCancelled { ... } } 꼴이라, 같은 조합으로 확인한다
class FinishEvenIfCancelledTest {

    @Test
    fun notCancelled_returnsBlockResult() = runBlocking {
        assertEquals(Result.success(42), suspendRunCatching { finishEvenIfCancelled { 42 } })
    }

    @Test
    fun notCancelled_blockFails_keepsDataExceptionType() = runBlocking {
        val result = suspendRunCatching<Unit> { finishEvenIfCancelled { throw PermissionDeniedException("not author") } }
        // 코루틴 디버그 모드는 withContext 를 지난 예외를 스택 복구용 사본으로 바꿀 수 있어서, 같은 객체가 아니라 종류만 본다
        assertTrue(result.exceptionOrNull() is PermissionDeniedException)
    }

    @Test
    fun cancelledWhileRunning_blockFinishes_callerGetsNoResult() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        var finished = false
        var result: Result<Unit>? = null
        val caller = launch {
            result = suspendRunCatching {
                finishEvenIfCancelled {
                    gate.await()
                    finished = true
                }
            }
        }
        yield()
        caller.cancel()
        gate.complete(Unit)
        caller.join()

        assertTrue(finished)
        assertTrue(caller.isCancelled)
        assertNull(result)
    }

    @Test
    fun cancelledWhileRunning_blockFails_callerGetsNoResult() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        var result: Result<Unit>? = null
        val caller = launch {
            result = suspendRunCatching {
                finishEvenIfCancelled {
                    gate.await()
                    throw PermissionDeniedException("not author")
                }
            }
        }
        yield()
        caller.cancel()
        gate.complete(Unit)
        caller.join()

        assertTrue(caller.isCancelled)
        assertNull(result)
    }
}
