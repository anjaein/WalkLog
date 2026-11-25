package com.example.walklog.data.log

import com.example.walklog.data.model.FeedSort
import com.example.walklog.data.model.WalkLog
import kotlinx.coroutines.flow.Flow

interface LogRepository {
    fun observeLog(logId: String): Flow<WalkLog?>

    /** 한 사용자의 로그, 최근 순. 내 프로필이면 includePrivate = true 로 나만 보기 로그까지 받는다. */
    fun observeLogsByUser(uid: String, includePrivate: Boolean): Flow<List<WalkLog>>

    /** 탐색 피드: 공개 로그만. tag 를 주면 그 태그가 붙은 로그만 거른다. */
    fun observeFeed(tag: String? = null, sort: FeedSort = FeedSort.RECENT, limit: Long = 50): Flow<List<WalkLog>>

    /** 새 로그를 저장하고 만들어진 logId 를 돌려준다. id · 좋아요 수 · 저장 수 · 작성 시각은 무시하고 새로 채운다. */
    suspend fun createLog(log: WalkLog): Result<String>

    /** 좋아요 수 · 저장 수 · 작성자 · 작성 시각은 바꾸지 않는다. */
    suspend fun updateLog(log: WalkLog): Result<Unit>

    /** 로그와 그 로그의 사진 파일을 함께 지운다. */
    suspend fun deleteLog(logId: String): Result<Unit>
}
