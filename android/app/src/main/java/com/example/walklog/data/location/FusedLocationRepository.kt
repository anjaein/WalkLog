package com.example.walklog.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.example.walklog.data.DataException
import com.example.walklog.data.LocationPermissionException
import com.example.walklog.data.mapDataErrors
import com.example.walklog.data.model.RoutePoint
import com.example.walklog.data.suspendRunCatching
import com.example.walklog.data.toDataException
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// 위치 권한은 화면에서 먼저 요청한다. 권한 없이 부르면 SecurityException 이 오고, LocationPermissionException 으로 바꿔 실패한다.
@SuppressLint("MissingPermission")
class FusedLocationRepository(context: Context) : LocationRepository {

    private val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)

    // 기본 버퍼(64)는 수집이 밀리면 trySend 가 실패해 경로 점이 조용히 빠진다. 기록 중 점은 하나도 버리지 않는다
    override fun locationUpdates(intervalMs: Long): Flow<RoutePoint> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs).build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { trySend(it.toRoutePoint()) }
            }
        }

        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                .addOnFailureListener { close(it.toLocationDataException()) }
        } catch (e: SecurityException) {
            close(e.toLocationDataException())
        }

        awaitClose { client.removeLocationUpdates(callback) }
    }.buffer(Channel.UNLIMITED).mapDataErrors()

    override suspend fun currentLocation(): Result<RoutePoint?> = suspendRunCatching {
        val cancellation = CancellationTokenSource()
        try {
            val location: Location? = client
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token)
                .await()
            location?.toRoutePoint()
        } catch (e: SecurityException) {
            throw e.toLocationDataException()
        } finally {
            cancellation.cancel()
        }
    }

    private fun Location.toRoutePoint() = RoutePoint(lat = latitude, lng = longitude, recordedAt = time)
}

/** 위치 권한이 없을 때의 SecurityException 만 [LocationPermissionException]으로, 나머지는 공통 규칙으로 바꾼다. */
internal fun Throwable.toLocationDataException(): DataException =
    if (this is SecurityException) LocationPermissionException(this) else toDataException()
