package com.example.walklog.data.location

import com.example.walklog.data.DataException
import com.example.walklog.data.LocationPermissionException
import com.example.walklog.data.UnknownDataException
import com.example.walklog.data.model.RoutePoint
import kotlinx.coroutines.flow.Flow

/**
 * 위치 권한은 화면에서 먼저 받아야 한다. 모든 실패(Result · Flow 오류)는 [DataException]이다.
 * 권한이 없으면 [LocationPermissionException], 그 밖에 위치를 못 받으면 [UnknownDataException](원래 예외는 cause)으로 실패한다.
 */
interface LocationRepository {
    /** 걷는 동안 intervalMs 마다 위치를 보낸다. GPS 튐 거르기는 domain 에서 한다. */
    fun locationUpdates(intervalMs: Long = 4_000L): Flow<RoutePoint>

    suspend fun currentLocation(): Result<RoutePoint?>
}
