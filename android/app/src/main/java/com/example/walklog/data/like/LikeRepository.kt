package com.example.walklog.data.like

import kotlinx.coroutines.flow.Flow

interface LikeRepository {
    fun observeIsLiked(uid: String, logId: String): Flow<Boolean>

    /** 내 좋아요 표시와 로그의 좋아요 수를 한 번에(트랜잭션) 바꾼다. 이미 그 상태면 아무것도 안 한다. */
    suspend fun setLiked(uid: String, logId: String, liked: Boolean): Result<Unit>
}
