package com.example.walklog.data

import com.example.walklog.data.location.toLocationDataException
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
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
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// Firebase 예외 객체는 JVM 단위 테스트에서 만들 수 없어서(android.text.TextUtils · Code 초기화 실패), 오류 코드를 받는 함수로 종류를 확인한다
class DataErrorsTest {

    private val cause = RuntimeException("sdk")

    @Test
    fun dataException_passesThrough() {
        val error = HandleTakenException("walker")
        assertSame(error, error.toDataException())
    }

    @Test
    fun runtimeException_becomesUnknownWithCause() {
        val error = IllegalStateException("boom")
        val mapped = error.toDataException()
        assertTrue(mapped is UnknownDataException)
        assertSame(error, mapped.cause)
    }

    @Test
    fun unknownHost_becomesNetworkException() {
        val error = UnknownHostException("firestore.googleapis.com")
        val mapped = error.toDataException()
        assertTrue(mapped is NetworkException)
        assertSame(error, mapped.cause)
    }

    @Test
    fun socketAndSslProblems_becomeNetworkException() {
        listOf(
            SocketException("Connection reset"),
            NoRouteToHostException("no route"),
            ConnectException("refused"),
            SSLException("connection closed"),
        ).forEach { assertTrue(it.toString(), it.toDataException() is NetworkException) }
    }

    @Test
    fun networkProblemDeepInCauseChain_becomesNetworkException() {
        val error = RuntimeException("wrapped", IllegalStateException("inner", UnknownHostException("x")))
        assertTrue(error.toDataException() is NetworkException)
    }

    @Test
    fun authCodes_invalidEmail() {
        assertTrue(authFailureFor("ERROR_INVALID_EMAIL", cause) is InvalidEmailException)
        assertTrue(authFailureFor("ERROR_MISSING_EMAIL", cause) is InvalidEmailException)
    }

    @Test
    fun authCodes_wrongPasswordAndMissingUser_areSameKind() {
        listOf("ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND", "ERROR_INVALID_CREDENTIAL", "ERROR_USER_DISABLED").forEach {
            assertTrue(it, authFailureFor(it, cause) is InvalidCredentialsException)
        }
    }

    @Test
    fun authCodes_emailInUse() {
        listOf("ERROR_EMAIL_ALREADY_IN_USE", "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL", "ERROR_CREDENTIAL_ALREADY_IN_USE").forEach {
            assertTrue(it, authFailureFor(it, cause) is EmailAlreadyInUseException)
        }
    }

    @Test
    fun authCodes_weakPassword() {
        assertTrue(authFailureFor("ERROR_WEAK_PASSWORD", cause) is WeakPasswordException)
        assertTrue(authFailureFor("ERROR_PASSWORD_DOES_NOT_MEET_REQUIREMENTS", cause) is WeakPasswordException)
    }

    @Test
    fun authCodes_expiredSession_isPermissionDenied() {
        assertTrue(authFailureFor("ERROR_USER_TOKEN_EXPIRED", cause) is PermissionDeniedException)
        assertTrue(authFailureFor("ERROR_INVALID_USER_TOKEN", cause) is PermissionDeniedException)
    }

    @Test
    fun authCodes_keepCauseAndAreAuthKind() {
        val mapped = authFailureFor("ERROR_WRONG_PASSWORD", cause)
        assertTrue(mapped is AuthException)
        assertSame(cause, mapped?.cause)
    }

    @Test
    fun authCodes_tooManyRequests() {
        assertTrue(authFailureFor("ERROR_TOO_MANY_REQUESTS", cause) is TooManyRequestsException)
    }

    @Test
    fun authCodes_unknownCode_isLeftToFallback() {
        assertNull(authFailureFor("ERROR_SOMETHING_NEW", cause))
    }

    @Test
    fun firestoreCodes() {
        assertTrue(firestoreFailureFor("PERMISSION_DENIED", cause) is PermissionDeniedException)
        assertTrue(firestoreFailureFor("UNAUTHENTICATED", cause) is PermissionDeniedException)
        val notFound = firestoreFailureFor("NOT_FOUND", cause)
        assertTrue(notFound is NotFoundException)
        assertSame(cause, notFound?.cause)
        assertTrue(firestoreFailureFor("RESOURCE_EXHAUSTED", cause) is TooManyRequestsException)
        assertNull(firestoreFailureFor("ABORTED", cause))
    }

    @Test
    fun storageCodes() {
        assertTrue(storageFailureFor(StorageException.ERROR_NOT_AUTHORIZED, cause) is PermissionDeniedException)
        assertTrue(storageFailureFor(StorageException.ERROR_NOT_AUTHENTICATED, cause) is PermissionDeniedException)
        assertTrue(storageFailureFor(StorageException.ERROR_OBJECT_NOT_FOUND, cause) is NotFoundException)
        assertTrue(storageFailureFor(StorageException.ERROR_QUOTA_EXCEEDED, cause) is TooManyRequestsException)
        assertNull(storageFailureFor(StorageException.ERROR_UNKNOWN, cause))
    }

    @Test
    fun securityException_becomesLocationPermission() {
        val error = SecurityException("no ACCESS_FINE_LOCATION")
        val mapped = error.toLocationDataException()
        assertTrue(mapped is LocationPermissionException)
        assertSame(error, mapped.cause)
    }

    @Test
    fun otherLocationFailure_usesCommonMapping() {
        assertTrue(IllegalStateException("x").toLocationDataException() is UnknownDataException)
        assertTrue(UnknownHostException("x").toLocationDataException() is NetworkException)
    }

    @Test
    fun flowError_becomesUnknownDataException() = runBlocking {
        val error = RuntimeException("listener failed")
        val failure = runCatching { flow<Int> { throw error }.mapDataErrors().toList() }.exceptionOrNull()
        assertTrue(failure is UnknownDataException)
        assertSame(error, failure?.cause)
    }

    @Test
    fun flowDataException_passesThrough() = runBlocking {
        val error = LocationPermissionException(SecurityException())
        val failure = runCatching { flow<Int> { throw error }.mapDataErrors().toList() }.exceptionOrNull()
        assertSame(error, failure)
    }

    @Test
    fun flowValues_passThrough() = runBlocking {
        val values = flow { emit(1); emit(2) }.mapDataErrors().toList()
        assertEquals(listOf(1, 2), values)
    }

    // Firestore snapshots() 가 리스너 오류를 알리는 방식(cancel(cause = 오류))을 흉내 낸다
    @Test
    fun upstreamCancelWithCause_collectorActive_isMappedByCause() = runBlocking {
        val error = IllegalStateException("listener failed")
        val failure = runCatching {
            callbackFlow<Int> {
                cancel("Error getting snapshot", error)
                awaitClose()
            }.mapDataErrors().toList()
        }.exceptionOrNull()
        assertTrue(failure is UnknownDataException)
        assertSame(error, failure?.cause)
    }

    @Test
    fun upstreamCancelWithNetworkCause_collectorActive_isNetwork() = runBlocking {
        val failure = runCatching {
            callbackFlow<Int> {
                cancel("Error getting snapshot", UnknownHostException("firestore.googleapis.com"))
                awaitClose()
            }.mapDataErrors().toList()
        }.exceptionOrNull()
        assertTrue(failure is NetworkException)
    }

    @Test
    fun cancellationCause_skipsNestedCancellations() {
        val root = IllegalStateException("root")
        val nested = CancellationException("copy", CancellationException("original", root))
        assertSame(root, nested.cancellationCause())
        val bare = CancellationException("bare")
        assertSame(bare, bare.cancellationCause())
    }

    @Test
    fun flowError_isRethrownAsIs() {
        assertThrows(StackOverflowError::class.java) {
            runBlocking { flow<Int> { throw StackOverflowError() }.mapDataErrors().toList() }
        }
    }

    @Test
    fun upstreamCancelWithErrorCause_rethrowsError() {
        assertThrows(OutOfMemoryError::class.java) {
            runBlocking {
                callbackFlow<Int> {
                    cancel("Error getting snapshot", OutOfMemoryError("test"))
                    awaitClose()
                }.mapDataErrors().toList()
            }
        }
    }

    @Test
    fun collectorCancelled_thenUpstreamOrdinaryException_isCancellation() = runBlocking {
        var blocked: Continuation<Unit>? = null
        var caught: Throwable? = null
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                flow<Int> {
                    // 취소를 듣지 않고 막혀 있다가, 연결이 끊긴 것처럼 일반 예외로 끝나는 위쪽 흐름을 흉내 낸다
                    suspendCoroutine<Unit> { blocked = it }
                    throw IOException("Socket closed")
                }.mapDataErrors().collect()
            } catch (e: Throwable) {
                caught = e
                throw e
            }
        }
        job.cancel()
        blocked!!.resume(Unit)
        job.join()
        assertTrue(job.isCancelled)
        assertTrue(caught is CancellationException)
        assertFalse(caught is DataException)
    }

    @Test
    fun collectorCancelled_thenUpstreamError_rethrowsError() = runBlocking {
        var blocked: Continuation<Unit>? = null
        var caught: Throwable? = null
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                flow<Int> {
                    suspendCoroutine<Unit> { blocked = it }
                    throw StackOverflowError()
                }.mapDataErrors().collect()
            } catch (e: Throwable) {
                // 다시 던지면 Error 가 부모(runBlocking)까지 올라가 테스트가 깨지므로, 여기서 받아 두기만 한다
                caught = e
            }
        }
        job.cancel()
        blocked!!.resume(Unit)
        job.join()
        assertTrue(caught is StackOverflowError)
    }

    @Test
    fun collectorCancelled_isNotMapped() = runBlocking {
        var caught: Throwable? = null
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                flow<Int> { awaitCancellation() }.mapDataErrors().collect()
            } catch (e: Throwable) {
                caught = e
                throw e
            }
        }
        job.cancel()
        job.join()
        assertTrue(job.isCancelled)
        assertTrue(caught is CancellationException)
        assertFalse(caught is DataException)
    }

    @Test
    fun collectorStoppingEarly_isNotMapped() = runBlocking {
        // first() 는 값을 받으면 내부적으로 CancellationException 계열로 위쪽 흐름을 멈춘다.
        // catch 연산자는 아래쪽에서 온 이런 중단을 바꾸지 않고 다시 던져서 통과한다. 다른 연산자로 바꿀 때 깨지지 않는지 지키는 테스트다
        val value = flow { emit(7); awaitCancellation() }.mapDataErrors().first()
        assertEquals(7, value)
    }
}
