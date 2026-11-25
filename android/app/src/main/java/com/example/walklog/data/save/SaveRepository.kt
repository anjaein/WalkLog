package com.example.walklog.data.save

import com.example.walklog.data.NetworkException
import com.example.walklog.data.log.LogIds
import com.example.walklog.data.user.UserIds
import kotlinx.coroutines.flow.Flow

interface SaveRepository {
    /** 보관함: 저장한 로그 id, 최근 저장 순. uid 가 [UserIds] 규칙에 맞지 않으면 수집할 때 [IllegalArgumentException]으로 실패한다. */
    fun observeSavedLogIds(uid: String): Flow<List<String>>

    /** uid 가 [UserIds], logId 가 [LogIds] 규칙에 맞지 않으면 수집할 때 [IllegalArgumentException]으로 실패한다. */
    fun observeIsSaved(uid: String, logId: String): Flow<Boolean>

    /**
     * 내 저장 표시와 로그의 저장 수를 한 번에(트랜잭션) 바꾼다. 이미 그 상태면 아무것도 안 한다.
     * uid 가 [UserIds], logId 가 [LogIds] 규칙에 맞지 않으면 [IllegalArgumentException], uid 가 로그인한 사용자가 아니면(로그인 안 함 포함) [IllegalStateException]으로 실패한다.
     * 쓰기는 연결이 있어야 한다. [NetworkException]은 서버가 바꿨는데 응답만 못 받았을 때도 올 수 있다.
     * 같은 값으로 다시 시도해도 이미 그 상태면 아무것도 안 하므로, 저장 수가 두 번 바뀌지 않는다.
     * 남의 나만 보기 로그에 saved = true 를 주면 없는 로그처럼 표시를 만들지 않고 성공으로 돌려준다.
     * saved = false 는 공개 범위와 상관없이 내 표시를 지우고, 표시가 있었고 로그가 남아 있으면 수도 줄인다.
     * 없는(지워진) 로그에 saved = true 를 주면 표시를 만들지 않고 성공으로 돌려준다(오류 처리 PR에서 '로그 없음' 실패로 바꿀 예정).
     * saved = false 는 로그가 지워졌어도 내 표시를 지운다.
     */
    suspend fun setSaved(uid: String, logId: String, saved: Boolean): Result<Unit>
}
