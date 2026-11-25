package com.example.walklog.data.like

import com.example.walklog.data.NetworkException
import com.example.walklog.data.log.LogIds
import kotlinx.coroutines.flow.Flow

interface LikeRepository {
    /** logId 가 [LogIds] 규칙에 맞지 않으면 수집할 때 [IllegalArgumentException]으로 실패한다. */
    fun observeIsLiked(uid: String, logId: String): Flow<Boolean>

    /**
     * 내 좋아요 표시와 로그의 좋아요 수를 한 번에(트랜잭션) 바꾼다. 이미 그 상태면 아무것도 안 한다.
     * logId 가 [LogIds] 규칙에 맞지 않으면 [IllegalArgumentException], uid 가 로그인한 사용자가 아니면(로그인 안 함 포함) [IllegalStateException]으로 실패한다.
     * 쓰기는 연결이 있어야 한다. [NetworkException]은 서버가 바꿨는데 응답만 못 받았을 때도 올 수 있다.
     * 같은 값으로 다시 시도해도 이미 그 상태면 아무것도 안 하므로, 좋아요 수가 두 번 바뀌지 않는다.
     * 없는(지워진) 로그에 liked = true 를 주면 표시를 만들지 않고 성공으로 돌려준다(오류 처리 PR에서 '로그 없음' 실패로 바꿀 예정).
     * liked = false 는 로그가 지워졌어도 내 표시를 지운다.
     */
    suspend fun setLiked(uid: String, logId: String, liked: Boolean): Result<Unit>
}
