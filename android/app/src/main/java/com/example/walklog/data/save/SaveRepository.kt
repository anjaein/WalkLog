package com.example.walklog.data.save

import kotlinx.coroutines.flow.Flow

interface SaveRepository {
    /** 보관함: 저장한 로그 id, 최근 저장 순. */
    fun observeSavedLogIds(uid: String): Flow<List<String>>

    fun observeIsSaved(uid: String, logId: String): Flow<Boolean>

    /** 내 저장 표시와 로그의 저장 수를 한 번에(트랜잭션) 바꾼다. 이미 그 상태면 아무것도 안 한다. */
    suspend fun setSaved(uid: String, logId: String, saved: Boolean): Result<Unit>
}
