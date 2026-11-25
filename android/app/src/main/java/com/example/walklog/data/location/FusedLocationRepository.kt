package com.example.walklog.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.example.walklog.data.model.RoutePoint
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// 위치 권한은 화면에서 먼저 요청한다. 권한 없이 부르면 SecurityException 으로 실패한다.
@SuppressLint("MissingPermission")
class FusedLocationRepository(context: Context) : LocationRepository {

    private val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)

    override fun locationUpdates(intervalMs: Long): Flow<RoutePoint> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs).build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { trySend(it.toRoutePoint()) }
            }
        }

        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                .addOnFailureListener { close(it) }
        } catch (e: SecurityException) {
            close(e)
        }

        awaitClose { client.removeLocationUpdates(callback) }
    }

    override suspend fun currentLocation(): Result<RoutePoint?> = runCatching {
        val cancellation = CancellationTokenSource()
        try {
            val location: Location? = client
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token)
                .await()
            location?.toRoutePoint()
        } finally {
            cancellation.cancel()
        }
    }

    private fun Location.toRoutePoint() = RoutePoint(lat = latitude, lng = longitude, recordedAt = time)
}
