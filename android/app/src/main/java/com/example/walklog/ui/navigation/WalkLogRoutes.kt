package com.example.walklog.ui.navigation

import com.example.walklog.ui.components.NavTab

/**
 * 앱의 모든 화면 주소. 웹 프로토타입(js/router.js)의 경로와 같다.
 * 주석의 폴더는 그 화면을 만들 feature 폴더다.
 */
object WalkLogRoutes {
    const val ARG_STEP = "step"
    const val ARG_LOG_ID = "logId"
    const val ARG_POST_ID = "postId"
    const val ARG_COURSE_ID = "courseId"
    const val ARG_USER_ID = "userId"
    const val ARG_PHOTO = "photo"

    const val ONBOARDING_FIRST_STEP = 1
    const val ONBOARDING_LAST_STEP = 7

    // 온보딩: 로그인 → 이메일 → 프로필 → 위치 → 항상 허용 → 나머지 권한 → 준비 완료 (feature/auth)
    const val ONBOARDING = "onboarding/{$ARG_STEP}"
    fun onboarding(step: Int) = "onboarding/$step"

    // 홈 탭 (feature/mypage)
    const val HOME = "home"
    const val MY_LOGS = "mylogs"
    const val ARCHIVE = "archive"
    const val SETTINGS = "settings"
    const val PROFILE_EDIT = "settings/profile"

    // 로그 상세 · 로그 남기기 · 로그 수정 (feature/record)
    // 로그 상세는 사진을 인스타 스토리처럼 한 장씩 넘겨 본다. photo는 몇 번째 사진(0부터)부터 열지다.
    const val LOG = "log/{$ARG_LOG_ID}?$ARG_PHOTO={$ARG_PHOTO}"
    fun log(logId: String, photo: Int = 0) = "log/$logId?$ARG_PHOTO=$photo"
    const val RECORD_TIME = "record/time"

    // edit에 로그 id가 있으면 로그 수정, 없으면 새 로그 남기기
    const val RECORD_COURSE = "record/course?edit={$ARG_LOG_ID}"
    fun recordCourse(editLogId: String? = null) =
        if (editLogId == null) "record/course" else "record/course?edit=$editLogId"
    const val RECORD_DONE = "record/done"

    // 지도 탭 (feature/photomap)
    const val MAP = "map"

    // 로그 · 남의 로그의 '지도에서 보기'로 오면 log 또는 post가 붙는다
    const val MAP_VIEW = "map/view?log={$ARG_LOG_ID}&post={$ARG_POST_ID}"
    fun mapView(logId: String? = null, postId: String? = null) = when {
        logId != null -> "map/view?log=$logId"
        postId != null -> "map/view?post=$postId"
        else -> "map/view"
    }
    const val FOLLOW = "follow/{$ARG_COURSE_ID}"
    fun follow(courseId: String) = "follow/$courseId"

    // 탐색 탭 (feature/discover)
    const val EXPLORE = "explore"
    const val SEARCH = "explore/search"
    // 남의 로그도 내 로그 상세와 같이 사진별 스토리로 본다
    const val POST = "post/{$ARG_POST_ID}?$ARG_PHOTO={$ARG_PHOTO}"
    fun post(postId: String, photo: Int = 0) = "post/$postId?$ARG_PHOTO=$photo"
    const val USER = "user/{$ARG_USER_ID}"
    fun user(userId: String) = "user/$userId"

    const val NOT_FOUND = "404"
}

/**
 * 지금 화면에서 하단 냅바의 어느 탭이 켜지는지. null이면 냅바를 숨긴다.
 * (온보딩, 상세 화면, 설정, 로그 남기기, 검색은 냅바 없이 전체 화면)
 */
fun navTabFor(route: String?): NavTab? = when (route) {
    WalkLogRoutes.HOME, WalkLogRoutes.MY_LOGS, WalkLogRoutes.ARCHIVE -> NavTab.HOME
    WalkLogRoutes.MAP, WalkLogRoutes.MAP_VIEW, WalkLogRoutes.FOLLOW -> NavTab.MAP
    WalkLogRoutes.EXPLORE -> NavTab.EXPLORE
    else -> null
}

/** 지도 탭 첫 화면은 배경이 어두워서 밝은 냅바를 쓴다. */
fun usesLightNavBar(route: String?): Boolean = route == WalkLogRoutes.MAP

fun NavTab.route(): String = when (this) {
    NavTab.HOME -> WalkLogRoutes.HOME
    NavTab.MAP -> WalkLogRoutes.MAP
    NavTab.EXPLORE -> WalkLogRoutes.EXPLORE
}
