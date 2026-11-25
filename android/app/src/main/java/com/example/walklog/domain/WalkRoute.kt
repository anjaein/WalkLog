package com.example.walklog.domain

data class RoutePoint(
    val position: GeoPoint,
    val timestampMillis: Long,
    val accuracyMeters: Double? = null,
) {
    init {
        require(timestampMillis >= 0)
    }
}

data class GpsFilterSettings(
    val maxAccuracyMeters: Double = 30.0,
    val maxSpeedMetersPerSecond: Double = 8.0,
    val maxGapMillis: Long = 30_000,
) {
    init {
        require(maxAccuracyMeters.isFinite() && maxAccuracyMeters > 0.0)
        require(maxSpeedMetersPerSecond.isFinite() && maxSpeedMetersPerSecond > 0.0)
        require(maxGapMillis > 0)
    }
}

class WalkRoute internal constructor(val segments: List<List<RoutePoint>>) {
    internal val geometry = RouteGeometry(segments.map { segment -> segment.map(RoutePoint::position) })

    val distanceMeters: Double get() = geometry.distanceMeters
    val startMillis: Long? get() = segments.firstOrNull()?.first()?.timestampMillis
    val endMillis: Long? get() = segments.lastOrNull()?.last()?.timestampMillis

    val elapsedMillis: Long get() = startMillis?.let { checkNotNull(endMillis) - it } ?: 0L
    val recordedMillis: Long = segments.sumOf { it.last().timestampMillis - it.first().timestampMillis }

    fun cut(startMillis: Long, endMillis: Long): WalkRoute {
        require(startMillis >= 0 && startMillis <= endMillis)
        return WalkRoute(segments.mapNotNull { segment ->
            val start = maxOf(startMillis, segment.first().timestampMillis)
            val end = minOf(endMillis, segment.last().timestampMillis)
            if (start > end) return@mapNotNull null

            buildList {
                add(segment.pointAt(start))
                if (start < end) {
                    addAll(segment.filter { it.timestampMillis > start && it.timestampMillis < end })
                    add(segment.pointAt(end))
                }
            }
        })
    }

    fun positionAt(timestampMillis: Long): GeoPoint? = segments
        .firstOrNull { timestampMillis in it.first().timestampMillis..it.last().timestampMillis }
        ?.pointAt(timestampMillis)
        ?.position
}

fun cleanGpsRoute(points: List<RoutePoint>, settings: GpsFilterSettings = GpsFilterSettings()): WalkRoute {
    val fixes = points.asSequence()
        .filter { point -> point.accuracyMeters?.let { it in 0.0..settings.maxAccuracyMeters } ?: true }
        .sortedWith(compareBy<RoutePoint> { it.timestampMillis }.thenBy { it.accuracyMeters ?: Double.POSITIVE_INFINITY })
        .distinctBy(RoutePoint::timestampMillis)
        .toList()

    val segments = mutableListOf<MutableList<RoutePoint>>()
    for ((index, point) in fixes.withIndex()) {
        val segment = segments.lastOrNull()
        val previous = segment?.last()
        if (previous != null && point.timestampMillis - previous.timestampMillis <= settings.maxGapMillis) {
            if (canWalkBetween(previous, point, settings)) segment.add(point)
            continue
        }

        // Reject an isolated first fix when the next two continuous fixes support each other.
        val next = fixes.getOrNull(index + 1)
        val following = fixes.getOrNull(index + 2)
        if (next != null && following != null &&
            next.timestampMillis - point.timestampMillis <= settings.maxGapMillis &&
            following.timestampMillis - next.timestampMillis <= settings.maxGapMillis &&
            !canWalkBetween(point, next, settings) && canWalkBetween(next, following, settings)
        ) {
            continue
        }
        segments.add(mutableListOf(point))
    }
    return WalkRoute(segments.map { it.toList() })
}

private fun canWalkBetween(from: RoutePoint, to: RoutePoint, settings: GpsFilterSettings): Boolean {
    val seconds = (to.timestampMillis - from.timestampMillis) / 1_000.0
    return distanceMeters(from.position, to.position) <= settings.maxSpeedMetersPerSecond * seconds
}

private fun List<RoutePoint>.pointAt(timestampMillis: Long): RoutePoint {
    val index = binarySearchBy(timestampMillis, selector = RoutePoint::timestampMillis)
    if (index >= 0) return this[index]

    val successor = -index - 1
    val start = this[successor - 1]
    val end = this[successor]
    val fraction = (timestampMillis - start.timestampMillis).toDouble() / (end.timestampMillis - start.timestampMillis)
    return RoutePoint(interpolate(start.position, end.position, fraction), timestampMillis)
}
