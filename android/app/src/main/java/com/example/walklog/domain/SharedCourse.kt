package com.example.walklog.domain

data class SharedCoursePhoto(val id: String, val title: String?, val description: String?)

data class SharedCoursePin(val position: GeoPoint, val photos: List<SharedCoursePhoto>) {
    val photoCount: Int get() = photos.size
}

class SharedCourse internal constructor(
    val sourceLogId: String,
    val title: String,
    val routeSegments: List<List<GeoPoint>>,
    val pins: List<SharedCoursePin>,
) {
    internal val geometry = RouteGeometry(routeSegments)
    val distanceMeters: Double get() = geometry.distanceMeters
}

fun buildSharedCourse(
    sourceLogId: String,
    title: String,
    route: WalkRoute,
    photos: List<WalkPhoto> = emptyList(),
    photoSettings: PhotoPlacementSettings = PhotoPlacementSettings(),
): SharedCourse? {
    require(sourceLogId.isNotBlank())
    require(title.isNotBlank())
    if (route.distanceMeters <= 0.0) return null

    val pins = placePhotos(route, photos, photoSettings).pins.map { pin ->
        SharedCoursePin(
            position = pin.position,
            photos = pin.photos.map { SharedCoursePhoto(it.id, it.title, it.description) },
        )
    }
    return SharedCourse(
        sourceLogId = sourceLogId,
        title = title,
        routeSegments = route.segments.map { segment -> segment.map(RoutePoint::position) },
        pins = pins,
    )
}

data class CourseFollowSettings(
    val leaveDistanceMeters: Double = 35.0,
    val rejoinDistanceMeters: Double = 25.0,
) {
    init {
        require(leaveDistanceMeters.isFinite() && leaveDistanceMeters > 0.0)
        require(rejoinDistanceMeters.isFinite() && rejoinDistanceMeters in 0.0..leaveDistanceMeters)
    }
}

@ConsistentCopyVisibility
data class CourseProgress internal constructor(
    val fraction: Double,
    val coveredDistanceMeters: Double,
    val remainingDistanceMeters: Double,
    val distanceToCourseMeters: Double,
    val isOffCourse: Boolean,
)

fun updateCourseProgress(
    course: SharedCourse,
    currentPosition: GeoPoint,
    previous: CourseProgress? = null,
    settings: CourseFollowSettings = CourseFollowSettings(),
): CourseProgress {
    val match = checkNotNull(course.geometry.project(currentPosition, previous?.coveredDistanceMeters))
    val limit = if (previous?.isOffCourse == true) settings.rejoinDistanceMeters else settings.leaveDistanceMeters
    val isOffCourse = match.distanceToRouteMeters > limit
    val distance = if (isOffCourse) previous?.coveredDistanceMeters ?: 0.0 else match.distanceAlongRouteMeters
    val covered = distance.coerceIn(0.0, course.distanceMeters)
    return CourseProgress(
        fraction = covered / course.distanceMeters,
        coveredDistanceMeters = covered,
        remainingDistanceMeters = course.distanceMeters - covered,
        distanceToCourseMeters = match.distanceToRouteMeters,
        isOffCourse = isOffCourse,
    )
}
