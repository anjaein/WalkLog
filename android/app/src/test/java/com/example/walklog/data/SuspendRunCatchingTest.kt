package com.example.walklog.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
    fun cancellation_isRethrown() {
        assertThrows(CancellationException::class.java) {
            runBlocking { suspendRunCatching<Int> { throw CancellationException("cancelled") } }
        }
    }

    @Test
    fun unknownHost_becomesNetworkException() = runBlocking {
        val error = UnknownHostException("api.cloudinary.com")
        val failure = suspendRunCatching<Int> { throw error }.exceptionOrNull()
        assertTrue(failure is NetworkException)
        assertSame(error, failure?.cause)
    }
}
