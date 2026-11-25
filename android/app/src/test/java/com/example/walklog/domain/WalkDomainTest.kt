package com.example.walklog.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WalkDomainTest {
    @Test
    fun gpsFixesAreSortedAndDuplicateTimestampsKeepTheMoreAccurateFix() {
        val start = fix(0.0, 0)
        val middle = fix(10.0, 10, accuracyMeters = 4.0)
        val end = fix(20.0, 20)
        val route = cleanGpsRoute(listOf(
            end, fix(500.0, 15), fix(70.0, 10, accuracyMeters = 12.0),
            fix(40.0, 25, accuracyMeters = 50.0), middle, start,
        ))

        assertEquals(listOf(listOf(start, middle, end)), route.segments)
        assertEquals(20.0, route.distanceMeters, 0.01)
        assertEquals(20_000L, route.recordedMillis)
    }

    @Test
    fun isolatedFirstFixDoesNotHideTheWalkingRoute() {
        val middle = fix(10.0, 10)
        val end = fix(20.0, 20)
        val route = cleanGpsRoute(listOf(fix(700.0, 0), middle, end))

        assertEquals(listOf(listOf(middle, end)), route.segments)
    }

    @Test
    fun cuttingUsesPointTimesEvenWhenPaceChanges() {
        val route = cleanGpsRoute(listOf(fix(0.0, 0), fix(20.0, 10), fix(80.0, 20)))
        val cut = route.cut(5_000, 15_000)

        assertEquals(listOf(5_000L, 10_000L, 15_000L), cut.segments.single().map(RoutePoint::timestampMillis))
        assertEquals(40.0, cut.distanceMeters, 0.01)
        assertEquals(10_000L, cut.elapsedMillis)
        assertEquals(10_000L, cut.recordedMillis)
    }

    @Test
    fun gpsGapsSplitTheRouteAndAreExcludedFromRecordedTime() {
        val route = cleanGpsRoute(listOf(fix(0.0, 0), fix(10.0, 10), fix(200.0, 65), fix(210.0, 75)))
        val cut = route.cut(5_000, 70_000)

        assertEquals(2, cut.segments.size)
        assertEquals(10.0, cut.distanceMeters, 0.01)
        assertEquals(65_000L, cut.elapsedMillis)
        assertEquals(10_000L, cut.recordedMillis)
        assertNull(cut.positionAt(30_000))
        assertTrue(cut.cut(20_000, 40_000).segments.isEmpty())
    }

    @Test
    fun emptyAndSinglePointRangesHaveZeroDistanceAndTime() {
        val fix = fix(10.0, 10)
        val route = cleanGpsRoute(listOf(fix))
        val instant = route.cut(10_000, 10_000)

        assertEquals(listOf(listOf(fix)), instant.segments)
        assertEquals(0.0, instant.distanceMeters, 0.0)
        assertEquals(0L, instant.elapsedMillis)
        assertTrue(route.cut(0, 9_999).segments.isEmpty())
        assertNull(cleanGpsRoute(emptyList()).startMillis)
    }

    @Test
    fun routeCrossingDateLineUsesTheShortLongitudeArc() {
        val route = cleanGpsRoute(listOf(
            RoutePoint(GeoPoint(0.0, 179.9997), 0),
            RoutePoint(GeoPoint(0.0, -179.9997), 20_000),
        ))

        assertEquals(66.72, route.distanceMeters, 0.02)
        assertEquals(180.0, kotlin.math.abs(checkNotNull(route.positionAt(10_000)).longitude), 0.000001)
    }

    @Test
    fun photosSnapByLocationOrTimeAndNearbyPinsKeepAllTheirPhotos() {
        val route = straightRoute()
        val first = WalkPhoto("first", 2_000, atMeters(20.0, 10.0), title = "Bridge")
        val second = WalkPhoto("second", 5_000, description = "River")
        val third = WalkPhoto("third", 16_000)
        val placement = placePhotos(route, listOf(third, second, first))

        assertEquals(listOf(2, 1), placement.pins.map(PhotoPin::photoCount))
        assertEquals(listOf(first, second), placement.pins.first().photos)
        assertEquals(0.0, distanceMeters(atMeters(20.0), placement.pins.first().position), 0.01)
        assertTrue(placement.unplacedPhotos.isEmpty())
    }

    @Test
    fun photoGroupingDoesNotChainAcrossTheWholeRoute() {
        val placement = placePhotos(straightRoute(), listOf(
            WalkPhoto("a", position = atMeters(0.0)),
            WalkPhoto("b", position = atMeters(20.0)),
            WalkPhoto("c", position = atMeters(40.0)),
        ))

        assertEquals(listOf(2, 1), placement.pins.map(PhotoPin::photoCount))
    }

    @Test
    fun photosOutsideTheSelectionFarFromTheRouteOrInGpsGapsStayUnplaced() {
        val route = cleanGpsRoute(listOf(fix(0.0, 0), fix(10.0, 10), fix(200.0, 50), fix(210.0, 60)))
            .cut(5_000, 55_000)
        val photos = listOf(
            WalkPhoto("outside selection", 3_000, atMeters(5.0)),
            WalkPhoto("far from route", 7_000, atMeters(7.0, 60.0)),
            WalkPhoto("gap", 20_000),
            WalkPhoto("unknown"),
        )
        val placement = placePhotos(route, photos)

        assertTrue(placement.pins.isEmpty())
        assertEquals(photos, placement.unplacedPhotos)
        assertEquals(photos, placePhotos(cleanGpsRoute(emptyList()), photos).unplacedPhotos)
    }

    @Test
    fun shareableCoursePreservesSegmentsAndPhotoLabels() {
        val route = cleanGpsRoute(listOf(fix(0.0, 0), fix(100.0, 20), fix(300.0, 60), fix(400.0, 80)))
        val photo = WalkPhoto("bridge", 10_000, title = "Bridge", description = "River")
        val course = checkNotNull(buildSharedCourse("log-1", "Riverside", route, listOf(photo, WalkPhoto("unknown"))))

        assertEquals("log-1", course.sourceLogId)
        assertEquals("Riverside", course.title)
        assertEquals(2, course.routeSegments.size)
        assertEquals(200.0, course.distanceMeters, 0.01)
        assertEquals(1, course.pins.single().photoCount)
        assertEquals(SharedCoursePhoto("bridge", "Bridge", "River"), course.pins.single().photos.single())
    }

    @Test
    fun routeWithNoDistanceCannotBeFollowedAsACourse() {
        val stationary = cleanGpsRoute(listOf(fix(0.0, 0), fix(0.0, 10)))

        assertNull(buildSharedCourse("log", "No movement", stationary))
        assertNull(buildSharedCourse("log", "No points", cleanGpsRoute(emptyList())))
    }

    @Test
    fun followReportsProgressAlongTheCourseAndRemainingDistance() {
        val course = checkNotNull(buildSharedCourse("log", "Walk", straightRoute()))
        val start = updateCourseProgress(course, atMeters(0.0))
        val midpoint = updateCourseProgress(course, atMeters(50.0, 4.0), previous = start)
        val end = updateCourseProgress(course, atMeters(100.0), previous = midpoint)

        assertEquals(0.0, start.fraction, 0.0001)
        assertEquals(0.5, midpoint.fraction, 0.0001)
        assertEquals(50.0, midpoint.remainingDistanceMeters, 0.01)
        assertEquals(4.0, midpoint.distanceToCourseMeters, 0.01)
        assertEquals(1.0, end.fraction, 0.0001)
        assertFalse(midpoint.isOffCourse)
    }

    @Test
    fun offCourseFreezesProgressUntilTheWalkerRejoins() {
        val course = checkNotNull(buildSharedCourse("log", "Walk", straightRoute()))
        val lastOnCourse = updateCourseProgress(course, atMeters(30.0))
        val offCourse = updateCourseProgress(course, atMeters(70.0, 40.0), previous = lastOnCourse)
        val stillOffCourse = updateCourseProgress(course, atMeters(70.0, 30.0), previous = offCourse)
        val rejoined = updateCourseProgress(course, atMeters(70.0, 20.0), previous = stillOffCourse)

        assertTrue(offCourse.isOffCourse)
        assertEquals(lastOnCourse.fraction, offCourse.fraction, 0.0)
        assertTrue(stillOffCourse.isOffCourse)
        assertFalse(rejoined.isOffCourse)
        assertEquals(0.7, rejoined.fraction, 0.0001)
        assertEquals(0.0, updateCourseProgress(course, atMeters(70.0, 40.0)).fraction, 0.0)
    }

    @Test
    fun followingAnOutAndBackCourseContinuesAfterTheTurn() {
        val route = cleanGpsRoute(listOf(fix(0.0, 0), fix(100.0, 20), fix(0.0, 40)))
        val course = checkNotNull(buildSharedCourse("log", "Out and back", route))
        val turn = updateCourseProgress(course, atMeters(100.0))
        val returning = updateCourseProgress(course, atMeters(60.0), previous = turn)

        assertEquals(0.5, turn.fraction, 0.0001)
        assertEquals(0.7, returning.fraction, 0.0001)
    }

    @Test
    fun followingDoesNotTreatAGpsGapAsAWalkableSegment() {
        val route = cleanGpsRoute(listOf(fix(0.0, 0), fix(100.0, 20), fix(300.0, 60), fix(400.0, 80)))
        val course = checkNotNull(buildSharedCourse("log", "Two segments", route))
        val gap = updateCourseProgress(course, atMeters(200.0))

        assertNotNull(course.routeSegments.lastOrNull())
        assertTrue(gap.isOffCourse)
        assertEquals(100.0, gap.distanceToCourseMeters, 0.01)
    }

    private fun straightRoute(): WalkRoute = cleanGpsRoute(listOf(fix(0.0, 0), fix(100.0, 20)))

    private fun fix(east: Double, seconds: Long, accuracyMeters: Double? = null): RoutePoint =
        RoutePoint(atMeters(east), seconds * 1_000, accuracyMeters)

    private fun atMeters(east: Double, north: Double = 0.0): GeoPoint =
        GeoPoint(north / 111_195.08, east / 111_195.08)
}
