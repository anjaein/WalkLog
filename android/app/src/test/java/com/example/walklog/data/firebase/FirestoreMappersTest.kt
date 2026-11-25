package com.example.walklog.data.firebase

import com.example.walklog.data.model.HeaderColor
import com.example.walklog.data.model.LogPhoto
import com.example.walklog.data.model.RoutePoint
import com.example.walklog.data.model.User
import com.example.walklog.data.model.Visibility
import com.example.walklog.data.model.WalkLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FirestoreMappersTest {
    private val user = User(
        uid = "u1",
        name = "석두",
        handle = "seokdu.walks",
        photoUrl = "https://example.com/a.jpg",
        bio = "걷는 사람",
        area = "망원동",
        headerColor = HeaderColor.COBALT,
        defaultVisibility = Visibility.PRIVATE,
        joinedAt = 1_760_000_000_000L,
    )

    private val log = WalkLog(
        id = "l1",
        authorId = "u1",
        title = "망원 한강공원",
        text = "노을",
        startedAt = 1_763_500_000_000L,
        endedAt = 1_763_502_820_000L,
        minutes = 47,
        distanceKm = 2.4,
        place = "망원동",
        visibility = Visibility.PUBLIC,
        tags = listOf("hangang", "night"),
        likeCount = 3,
        saveCount = 1,
        route = listOf(RoutePoint(37.555, 126.895, 1_763_500_000_000L), RoutePoint(37.556, 126.896, 1_763_500_004_000L, segment = 1)),
        photos = listOf(LogPhoto("https://example.com/p.jpg", 37.555, 126.895, 1_763_500_100_000L, "노을"), LogPhoto("https://example.com/q.jpg")),
        createdAt = 1_763_502_900_000L,
        updatedAt = 1_763_502_900_000L,
    )

    @Test
    fun user_roundTrip() {
        assertEquals(user, userFromFirestore("u1", user.toFirestore()))
    }

    @Test
    fun walkLog_roundTrip() {
        assertEquals(log, walkLogFromFirestore("l1", log.toFirestore()))
    }

    @Test
    fun walkLog_readsFirestoreNumberTypes() {
        // Firestore는 정수를 Long으로 돌려준다
        val data = log.toFirestore() + mapOf("minutes" to 47L, "likeCount" to 3L, "distanceKm" to 2L)
        val read = walkLogFromFirestore("l1", data)!!
        assertEquals(47, read.minutes)
        assertEquals(3, read.likeCount)
        assertEquals(2.0, read.distanceKm, 0.0)
    }

    @Test
    fun missingRequiredFields_returnNull() {
        assertNull(userFromFirestore("u1", mapOf("name" to "석두")))
        assertNull(walkLogFromFirestore("l1", mapOf("title" to "제목만")))
    }

    @Test
    fun unknownOrMissingValues_useDefaults() {
        val read = userFromFirestore("u1", mapOf("name" to "석두", "handle" to "seokdu", "headerColor" to "purple"))!!
        assertEquals(HeaderColor.TOMATO, read.headerColor)
        assertEquals(Visibility.PUBLIC, read.defaultVisibility)
        assertEquals("", read.bio)
    }

    @Test
    fun brokenRouteAndPhotoEntries_areSkipped() {
        val data = log.toFirestore() + mapOf(
            "route" to listOf(mapOf("lat" to 37.5), mapOf("lat" to 37.5, "lng" to 126.9)),
            "photos" to listOf(mapOf("note" to "주소 없음"), mapOf("url" to "https://example.com/p.jpg")),
        )
        val read = walkLogFromFirestore("l1", data)!!
        assertEquals(1, read.route.size)
        assertEquals(1, read.photos.size)
    }
}
