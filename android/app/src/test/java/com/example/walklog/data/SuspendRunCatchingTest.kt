package com.example.walklog.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

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
    fun unknownHost_becomesNetworkException() = runBlocking {
        val error = UnknownHostException("api.cloudinary.com")
        val failure = suspendRunCatching<Int> { throw error }.exceptionOrNull()
        assertTrue(failure is NetworkException)
        assertSame(error, failure?.cause)
    }
}
