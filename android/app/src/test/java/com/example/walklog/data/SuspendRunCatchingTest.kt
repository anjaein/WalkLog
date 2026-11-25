package com.example.walklog.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SuspendRunCatchingTest {

    @Test
    fun success_returnsValue() = runBlocking {
        assertEquals(Result.success(42), suspendRunCatching { 42 })
    }

    @Test
    fun exception_returnsSameFailure() = runBlocking {
        val error = IllegalStateException("boom")
        val result = suspendRunCatching<Int> { throw error }
        assertSame(error, result.exceptionOrNull())
    }

    @Test
    fun error_propagates() {
        assertThrows(OutOfMemoryError::class.java) {
            runBlocking { suspendRunCatching<Int> { throw OutOfMemoryError("test") } }
        }
    }

    @Test
    fun cancellationWhileCoroutineActive_becomesNonCancellationFailure() = runBlocking {
        val error = CancellationException("task cancelled")
        val failure = suspendRunCatching<Int> { throw error }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
        assertFalse(failure is CancellationException)
        assertSame(error, failure?.cause)
    }

    @Test
    fun cancelledCoroutine_rethrowsCancellation() = runBlocking {
        var resumedAfterCatch = false
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            suspendRunCatching<Int> { awaitCancellation() }
            resumedAfterCatch = true
        }
        job.cancel()
        job.join()
        assertTrue(job.isCancelled)
        assertFalse(resumedAfterCatch)
    }

    @Test
    fun cancelledCoroutine_thenOrdinaryException_rethrowsCancellation() = runBlocking {
        var blocked: Continuation<Unit>? = null
        var result: Result<Int>? = null
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            result = suspendRunCatching {
                // 취소를 듣지 않고 막혀 있다가, 연결이 끊긴 것처럼 일반 예외로 끝나는 호출을 흉내 낸다
                suspendCoroutine<Unit> { blocked = it }
                throw IOException("Socket closed")
            }
        }
        job.cancel()
        blocked!!.resume(Unit)
        job.join()
        assertTrue(job.isCancelled)
        assertNull(result)
    }

    @Test
    fun unknownHost_becomesNetworkException() = runBlocking {
        val error = UnknownHostException("api.cloudinary.com")
        val failure = suspendRunCatching<Int> { throw error }.exceptionOrNull()
        assertTrue(failure is NetworkException)
        assertSame(error, failure?.cause)
    }
}
