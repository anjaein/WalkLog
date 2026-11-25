package com.example.walklog.data.firebase

import com.example.walklog.data.model.HeaderColor
import com.example.walklog.data.model.LogPhoto
import com.example.walklog.data.model.RoutePoint
import com.example.walklog.data.model.User
import com.example.walklog.data.model.Visibility
import com.example.walklog.data.model.WalkLog

// Firestore 문서(Map) <-> 모델 변환. Firebase SDK 없이 돌아가서 단위 테스트로 확인한다.
// Firestore는 정수를 Long, 실수를 Double로 돌려주므로 숫자는 Number로 받아서 바꾼다.

fun User.toFirestore(): Map<String, Any?> = mapOf(
    "name" to name,
    "handle" to handle,
    "photoUrl" to photoUrl,
    "bio" to bio,
    "area" to area,
    "headerColor" to headerColor.name.lowercase(),
    "defaultVisibility" to defaultVisibility.name.lowercase(),
    "language" to language,
    "joinedAt" to joinedAt,
)

fun userFromFirestore(uid: String, data: Map<String, Any?>): User? {
    val name = data["name"] as? String ?: return null
    val handle = data["handle"] as? String ?: return null
    return User(
        uid = uid,
        name = name,
        handle = handle,
        photoUrl = data["photoUrl"] as? String,
        bio = data.string("bio"),
        area = data.string("area"),
        headerColor = enumOrDefault(data["headerColor"], HeaderColor.TOMATO),
        defaultVisibility = enumOrDefault(data["defaultVisibility"], Visibility.PUBLIC),
        language = data["language"] as? String ?: "ko",
        joinedAt = data.long("joinedAt"),
    )
}

fun WalkLog.toFirestore(): Map<String, Any?> = mapOf(
    "authorId" to authorId,
    "title" to title,
    "text" to text,
    "startedAt" to startedAt,
    "endedAt" to endedAt,
    "minutes" to minutes,
    "distanceKm" to distanceKm,
    "place" to place,
    "visibility" to visibility.name.lowercase(),
    "tags" to tags,
    "likeCount" to likeCount,
    "saveCount" to saveCount,
    "route" to route.map { it.toFirestore() },
    "photos" to photos.map { it.toFirestore() },
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
)

fun walkLogFromFirestore(id: String, data: Map<String, Any?>): WalkLog? {
    val authorId = data["authorId"] as? String ?: return null
    return WalkLog(
        id = id,
        authorId = authorId,
        title = data.string("title"),
        text = data.string("text"),
        startedAt = data.long("startedAt"),
        endedAt = data.long("endedAt"),
        minutes = data.long("minutes").toInt(),
        distanceKm = (data["distanceKm"] as? Number)?.toDouble() ?: 0.0,
        place = data.string("place"),
        visibility = visibilityFromFirestore(data["visibility"]),
        tags = (data["tags"] as? List<*>)?.filterIsInstance<String>().orEmpty(),
        likeCount = data.long("likeCount").toInt(),
        saveCount = data.long("saveCount").toInt(),
        route = data.mapList("route").mapNotNull(::routePointFromFirestore),
        photos = data.mapList("photos").mapNotNull(::logPhotoFromFirestore),
        createdAt = data.long("createdAt"),
        updatedAt = data.long("updatedAt"),
    )
}

/** 로그의 공개 범위. 값이 없거나 모르는 값이면 나만 보기로 읽어, 잘못된 문서가 공개로 새지 않게 한다. */
fun visibilityFromFirestore(value: Any?): Visibility = enumOrDefault(value, Visibility.PRIVATE)

fun RoutePoint.toFirestore(): Map<String, Any?> = mapOf(
    "lat" to lat,
    "lng" to lng,
    "recordedAt" to recordedAt,
    "segment" to segment,
)

fun routePointFromFirestore(data: Map<String, Any?>): RoutePoint? {
    val lat = (data["lat"] as? Number)?.toDouble() ?: return null
    val lng = (data["lng"] as? Number)?.toDouble() ?: return null
    return RoutePoint(lat, lng, data.long("recordedAt"), data.long("segment").toInt())
}

fun LogPhoto.toFirestore(): Map<String, Any?> = mapOf(
    "url" to url,
    "lat" to lat,
    "lng" to lng,
    "takenAt" to takenAt,
    "note" to note,
)

fun logPhotoFromFirestore(data: Map<String, Any?>): LogPhoto? {
    val url = data["url"] as? String ?: return null
    return LogPhoto(
        url = url,
        lat = (data["lat"] as? Number)?.toDouble(),
        lng = (data["lng"] as? Number)?.toDouble(),
        takenAt = (data["takenAt"] as? Number)?.toLong(),
        note = data["note"] as? String,
    )
}

private fun Map<String, Any?>.string(key: String): String = this[key] as? String ?: ""

private fun Map<String, Any?>.long(key: String): Long = (this[key] as? Number)?.toLong() ?: 0L

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any?>.mapList(key: String): List<Map<String, Any?>> =
    (this[key] as? List<*>)?.filterIsInstance<Map<*, *>>()?.map { it as Map<String, Any?> }.orEmpty()

private inline fun <reified E : Enum<E>> enumOrDefault(value: Any?, default: E): E =
    enumValues<E>().firstOrNull { it.name.equals(value as? String, ignoreCase = true) } ?: default
