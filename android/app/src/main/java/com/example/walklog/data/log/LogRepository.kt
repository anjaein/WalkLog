package com.example.walklog.data.log

import com.example.walklog.data.NetworkException
import com.example.walklog.data.model.FeedSort
import com.example.walklog.data.model.WalkLog
import kotlinx.coroutines.flow.Flow

interface LogRepository {
    /** 로그 하나를 지켜본다. 지워졌거나 없으면 null. logId 가 [LogIds] 규칙에 맞지 않으면 수집할 때 [IllegalArgumentException]으로 실패한다. */
    fun observeLog(logId: String): Flow<WalkLog?>

    /** 한 사용자의 로그, 최근 순. 내 프로필이면 includePrivate = true 로 나만 보기 로그까지 받는다. */
    fun observeLogsByUser(uid: String, includePrivate: Boolean): Flow<List<WalkLog>>

    /** 탐색 피드: 공개 로그만. tag 를 주면 그 태그가 붙은 로그만 거른다. */
    fun observeFeed(tag: String? = null, sort: FeedSort = FeedSort.RECENT, limit: Long = 50): Flow<List<WalkLog>>

    /**
     * 지금 로그인한 사용자의 새 로그로 저장하고 logId 를 돌려준다.
     * 작성자는 log.authorId 와 상관없이 로그인한 uid 로 채우고, 좋아요 수 · 저장 수 · 작성 시각은 무시하고 새로 채운다.
     * 로그인하지 않았으면 [IllegalStateException]으로 실패한다.
     *
     * log.id 가 비어 있으면 새 id 를 만들고, 있으면 그 id 로 저장한다([LogIds] 규칙에 맞지 않으면 [IllegalArgumentException]).
     * 쓰기는 연결이 있어야 한다. [NetworkException]은 서버가 저장했는데 응답만 못 받았을 때도 올 수 있으므로,
     * 화면은 초안마다 id 를 한 번 만들어(예: UUID) 재시도할 때도 같은 id 를 넘겨야 한다. 새 id 로 다시 시도하면 로그가 두 개 생길 수 있다.
     *
     * 재시도는 이미 있는 로그를 덮어쓰지 않는다: 같은 작성자의 로그가 이미 있으면 그대로 두고 성공으로 그 id 를 돌려주고,
     * 다른 작성자의 로그가 그 id 를 쓰고 있으면 [IllegalStateException]으로 실패한다.
     * 그래서 첫 시도가 실제로는 저장됐고 그 뒤 사용자가 초안을 고쳤다면, 재시도는 성공해도 예전 내용이 남는다.
     * 화면은 재시도가 성공한 뒤 고친 초안으로 [updateLog]를 불러야 한다.
     */
    suspend fun createLog(log: WalkLog): Result<String>

    /**
     * 좋아요 수 · 저장 수 · 작성자 · 작성 시각은 바꾸지 않는다.
     * 로그가 없으면 [NoSuchElementException], 로그인하지 않았거나 저장된 작성자가 로그인한 사용자가 아니면 [IllegalStateException],
     * log.id 가 [LogIds] 규칙에 맞지 않으면 [IllegalArgumentException]으로 실패한다.
     * 쓰기는 연결이 있어야 한다. [NetworkException]은 서버가 고쳤는데 응답만 못 받았을 때도 올 수 있으며, 같은 내용으로 다시 시도하면 된다.
     * 이번 수정을 위해 새로 올린 사진은, 수정이 확실히 안 됐을 때(로그 없음 · 작성자 아님 · 잘못된 입력 · 로그인 안 함)만 화면이 지운다.
     * [NetworkException]처럼 결과를 모르는 실패면 이미 저장됐을 수 있으니 사진을 지우지 말고 같은 내용으로 다시 시도한다.
     * 수정이 성공하면 목록에서 뺀 사진 파일은 화면이 지운다(Repository 는 지우지 않는다).
     */
    suspend fun updateLog(log: WalkLog): Result<Unit>

    /**
     * 로그와 그 로그의 사진 파일을 함께 지운다. 이미 없는 로그면 지울 것 없이 성공한다.
     * 로그인하지 않았거나 저장된 작성자가 로그인한 사용자가 아니면 [IllegalStateException],
     * logId 가 [LogIds] 규칙에 맞지 않으면 [IllegalArgumentException]으로 실패한다.
     * 쓰기는 연결이 있어야 한다. [NetworkException]은 서버가 지웠는데 응답만 못 받았을 때도 올 수 있다.
     * 사진 파일은 로그 문서가 지워진 뒤에 지우며, 사진을 못 지워도 실패로 돌리지 않는다.
     * 알려진 문제: 서버가 로그를 지운 뒤 응답을 못 받으면, 재시도는 지울 로그를 못 찾아 성공하고 사진 파일은 남을 수 있다(서버 쪽 정리는 없다).
     */
    suspend fun deleteLog(logId: String): Result<Unit>
}
