package com.example.walklog.domain

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPoint(val latitude: Double, val longitude: Double) {
    init {
        require(latitude in -90.0..90.0 && longitude in -180.0..180.0)
    }
}

private const val EARTH_RADIUS_METERS = 6_371_008.8
private const val RADIANS_PER_DEGREE = kotlin.math.PI / 180.0
private const val PROJECTION_TIE_METERS = 0.5

internal fun distanceMeters(from: GeoPoint, to: GeoPoint): Double {
    val latitudeDelta = (to.latitude - from.latitude) * RADIANS_PER_DEGREE
    val longitudeDelta = longitudeDelta(from, to) * RADIANS_PER_DEGREE
    val haversine = sin(latitudeDelta / 2.0) * sin(latitudeDelta / 2.0) +
        cos(from.latitude * RADIANS_PER_DEGREE) * cos(to.latitude * RADIANS_PER_DEGREE) *
        sin(longitudeDelta / 2.0) * sin(longitudeDelta / 2.0)
    return 2.0 * EARTH_RADIUS_METERS * asin(sqrt(haversine.coerceIn(0.0, 1.0)))
}

internal fun interpolate(from: GeoPoint, to: GeoPoint, fraction: Double): GeoPoint = GeoPoint(
    latitude = from.latitude + (to.latitude - from.latitude) * fraction,
    longitude = wrapLongitude(from.longitude + longitudeDelta(from, to) * fraction),
)

private fun longitudeDelta(from: GeoPoint, to: GeoPoint): Double =
    wrapLongitude(to.longitude - from.longitude)

private fun wrapLongitude(longitude: Double): Double = (longitude + 540.0) % 360.0 - 180.0

internal data class RouteProjection(
    val position: GeoPoint,
    val distanceToRouteMeters: Double,
    val distanceAlongRouteMeters: Double,
)

internal class RouteGeometry(segments: List<List<GeoPoint>>) {
    private data class Leg(
        val start: GeoPoint,
        val end: GeoPoint,
        val offsetMeters: Double,
        val lengthMeters: Double,
    ) {
        fun project(position: GeoPoint): RouteProjection {
            // Walking segments are short enough to project onto a local plane.
            val longitudeScale = cos((start.latitude + end.latitude) / 2.0 * RADIANS_PER_DEGREE)
            val dx = longitudeDelta(start, end) * longitudeScale
            val dy = end.latitude - start.latitude
            val px = longitudeDelta(start, position) * longitudeScale
            val py = position.latitude - start.latitude
            val squaredLength = dx * dx + dy * dy
            val fraction = if (squaredLength == 0.0) {
                0.0
            } else {
                ((px * dx + py * dy) / squaredLength).coerceIn(0.0, 1.0)
            }
            val projected = when (fraction) {
                0.0 -> start
                1.0 -> end
                else -> interpolate(start, end, fraction)
            }
            return RouteProjection(
                position = projected,
                distanceToRouteMeters = distanceMeters(position, projected),
                distanceAlongRouteMeters = offsetMeters + lengthMeters * fraction,
            )
        }
    }

    private val legs = buildList {
        var offsetMeters = 0.0
        for (segment in segments) {
            segment.singleOrNull()?.let { add(Leg(it, it, offsetMeters, 0.0)) }
            for ((start, end) in segment.zipWithNext()) {
                val lengthMeters = distanceMeters(start, end)
                add(Leg(start, end, offsetMeters, lengthMeters))
                offsetMeters += lengthMeters
            }
        }
    }

    val distanceMeters: Double = legs.lastOrNull()?.let { it.offsetMeters + it.lengthMeters } ?: 0.0

    fun project(position: GeoPoint, previousDistanceMeters: Double? = null): RouteProjection? {
        val candidates = legs.map { it.project(position) }
        val nearest = candidates.minByOrNull { it.distanceToRouteMeters } ?: return null
        if (previousDistanceMeters == null) return nearest

        // At a crossing, use the previous progress to distinguish nearly identical matches.
        val match = candidates.asSequence()
            .filter { it.distanceToRouteMeters <= nearest.distanceToRouteMeters + PROJECTION_TIE_METERS }
            .minWith(compareBy<RouteProjection> { abs(it.distanceAlongRouteMeters - previousDistanceMeters) }
                .thenByDescending { it.distanceAlongRouteMeters })
        return match.copy(distanceToRouteMeters = nearest.distanceToRouteMeters)
    }
}
