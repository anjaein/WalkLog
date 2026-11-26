package com.example.walklog.domain

data class WalkPhoto(
    val id: String,
    val capturedAtMillis: Long? = null,
    val position: GeoPoint? = null,
    val title: String? = null,
    val description: String? = null,
) {
    init {
        require(id.isNotBlank())
        require(capturedAtMillis == null || capturedAtMillis >= 0)
    }
}

data class PhotoPlacementSettings(
    val snapDistanceMeters: Double = 50.0,
    val groupingDistanceMeters: Double = 25.0,
) {
    init {
        require(snapDistanceMeters.isFinite() && snapDistanceMeters >= 0.0)
        require(groupingDistanceMeters.isFinite() && groupingDistanceMeters >= 0.0)
    }
}

@ConsistentCopyVisibility
data class PhotoPin internal constructor(
    val position: GeoPoint,
    val photos: List<WalkPhoto>,
) {
    val photoCount: Int get() = photos.size
}

data class PhotoPlacement(val pins: List<PhotoPin>, val unplacedPhotos: List<WalkPhoto>)

fun placePhotos(
    route: WalkRoute,
    photos: List<WalkPhoto>,
    settings: PhotoPlacementSettings = PhotoPlacementSettings(),
): PhotoPlacement {
    val start = route.startMillis ?: return PhotoPlacement(emptyList(), photos.toList())
    val end = checkNotNull(route.endMillis)
    val groups = mutableListOf<PhotoGroup>()
    val unplacedPhotos = mutableListOf<WalkPhoto>()
    val orderedPhotos = photos.sortedWith(compareBy<WalkPhoto> { it.capturedAtMillis ?: Long.MAX_VALUE }.thenBy { it.id })

    for (photo in orderedPhotos) {
        val timestamp = photo.capturedAtMillis
        val position = if (timestamp == null || timestamp in start..end) {
            matchPhoto(route, photo, settings.snapDistanceMeters)
        } else {
            null
        }
        if (position == null) {
            unplacedPhotos.add(photo)
            continue
        }

        val group = groups.asSequence()
            .map { it to distanceMeters(it.position, position) }
            .filter { (_, distance) -> distance <= settings.groupingDistanceMeters }
            .minByOrNull { (_, distance) -> distance }
            ?.first
        if (group == null) {
            groups.add(PhotoGroup(position, mutableListOf(photo)))
        } else {
            group.photos.add(photo)
        }
    }

    return PhotoPlacement(
        pins = groups.map { PhotoPin(it.position, it.photos.toList()) },
        unplacedPhotos = unplacedPhotos.toList(),
    )
}

private data class PhotoGroup(val position: GeoPoint, val photos: MutableList<WalkPhoto>)

private fun matchPhoto(route: WalkRoute, photo: WalkPhoto, snapDistanceMeters: Double): GeoPoint? {
    val position = photo.position ?: return photo.capturedAtMillis?.let(route::positionAt)
    return route.geometry.project(position)
        ?.takeIf { it.distanceToRouteMeters <= snapDistanceMeters }
        ?.position
}
