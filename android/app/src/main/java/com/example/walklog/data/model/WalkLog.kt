package com.example.walklog.data.model

/**
 * 산책 로그 하나. Firestore logs/{logId} 문서와 같은 모양이다.
 * 내 로그와 남의 로그(탐색 피드)를 한 컬렉션에 두고, 공개 범위와 작성자로 나눈다. 시각은 모두 epoch 밀리초.
 */
data class WalkLog(
    val id: String = "",
    val authorId: String,
    val title: String,
    val text: String = "",
    val startedAt: Long,
    val endedAt: Long,
    val minutes: Int,
    val distanceKm: Double,
    val place: String,
    val visibility: Visibility = Visibility.PUBLIC,
    val tags: List<String> = emptyList(),
    val likeCount: Int = 0,
    val saveCount: Int = 0,
    val route: List<RoutePoint> = emptyList(),
    val photos: List<LogPhoto> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

/** 걷는 동안 몇 초마다 찍힌 위치. segment는 GPS가 오래 끊겼다가 다시 이어질 때 늘어난다. */
data class RoutePoint(
    val lat: Double,
    val lng: Double,
    val recordedAt: Long,
    val segment: Int = 0,
)

/** 로그에 붙은 사진. 위치 · 시각은 원본 사진에서 읽은 값이고, 업로드한 파일에서는 위치 정보를 지운다. */
data class LogPhoto(
    val url: String,
    val lat: Double? = null,
    val lng: Double? = null,
    val takenAt: Long? = null,
    val note: String? = null,
)

enum class FeedSort { RECENT, POPULAR }
