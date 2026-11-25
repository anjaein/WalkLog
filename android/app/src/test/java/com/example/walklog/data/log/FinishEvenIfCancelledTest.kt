package com.example.walklog.data.log

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
                    throw IllegalStateException("not author")
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
