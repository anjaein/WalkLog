package com.example.walklog.seed

import com.example.walklog.data.model.RoutePoint
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/** 꺾임점을 따라 일정한 빠르기로 걸었다고 보고, 시각마다의 위치를 만든다. */
internal class SeedRoute(
    private val waypoints: List<Pair<Double, Double>>,
    private val startedAt: Long,
    private val endedAt: Long,
    seed: Int,
) {
    init {
        require(waypoints.size >= 2) { "Route needs at least two waypoints" }
        require(endedAt > startedAt) { "Route must end after it starts" }
    }

    private val cumulative: DoubleArray = DoubleArray(waypoints.size).also { acc ->
        for (i in 1 until waypoints.size) acc[i] = acc[i - 1] + distanceMeters(waypoints[i - 1], waypoints[i])
    }
    private val total = cumulative.last()

    // 손으로 찍은 직선 경로는 너무 반듯해서, GPS 처럼 몇 미터 안에서 천천히 흔들리게 한다.
    // 점마다 따로 흔들면 거리가 부풀어서 느린 사인파를 쓴다
    private val phases = DoubleArray(4) { i -> ((seed * 31 + i * 17) % 360) * PI / 180 }

    fun points(): List<RoutePoint> {
        val times = generateSequence(startedAt) { it + INTERVAL_MS }.takeWhile { it < endedAt }.toList() + endedAt
        return times.map { t ->
            val (lat, lng) = positionAt(t)
            RoutePoint(lat = lat, lng = lng, recordedAt = t)
        }
    }

    fun positionAt(time: Long): Pair<Double, Double> {
        val t = time.coerceIn(startedAt, endedAt)
        val walked = total * (t - startedAt) / (endedAt - startedAt)
        var i = 1
        while (i < cumulative.lastIndex && cumulative[i] < walked) i++
        val segment = cumulative[i] - cumulative[i - 1]
        val f = if (segment == 0.0) 0.0 else (walked - cumulative[i - 1]) / segment
        val (lat1, lng1) = waypoints[i - 1]
        val (lat2, lng2) = waypoints[i]
        val seconds = (t - startedAt) / 1000.0
        val north = 1.5 * sin(2 * PI * seconds / 97 + phases[0]) + 0.7 * sin(2 * PI * seconds / 41 + phases[1])
        val east = 1.5 * sin(2 * PI * seconds / 113 + phases[2]) + 0.7 * sin(2 * PI * seconds / 37 + phases[3])
        val lat = lat1 + (lat2 - lat1) * f + north / METERS_PER_DEGREE
        val lng = lng1 + (lng2 - lng1) * f + east / (METERS_PER_DEGREE * cos(lat * PI / 180))
        return round6(lat) to round6(lng)
    }

    companion object {
        const val INTERVAL_MS = 5_000L
        private const val METERS_PER_DEGREE = 111_320.0
        private const val EARTH_RADIUS_M = 6_371_008.8

        fun distanceKm(points: List<RoutePoint>): Double {
            val meters = points.zipWithNext { a, b -> distanceMeters(a.lat to a.lng, b.lat to b.lng) }.sum()
            return round(meters / 10) / 100
        }

        fun distanceMeters(a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
            val lat1 = a.first * PI / 180
            val lat2 = b.first * PI / 180
            val dLat = lat2 - lat1
            val dLng = (b.second - a.second) * PI / 180
            val h = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLng / 2) * sin(dLng / 2)
            return 2 * EARTH_RADIUS_M * asin(sqrt(h))
        }

        /** "2026-11-18" + "18:12" → 서울 시각 epoch 밀리초. */
        fun epochMillis(date: String, time: String): Long {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
                isLenient = false
                timeZone = TimeZone.getTimeZone(SeedData.TIME_ZONE)
            }
            return checkNotNull(format.parse("$date $time")) { "Bad seed time: $date $time" }.time
        }

        private fun round6(value: Double): Double = round(value * 1_000_000) / 1_000_000
    }
}
