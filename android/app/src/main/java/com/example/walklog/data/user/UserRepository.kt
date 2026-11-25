package com.example.walklog.data.user

import com.example.walklog.data.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeUser(uid: String): Flow<User?>

    suspend fun isHandleAvailable(handle: String): Result<Boolean>

    /** 아이디(handle)를 차지하면서 프로필을 처음 만든다. 이미 누가 쓰는 아이디면 [HandleTakenException]으로 실패한다. */
    suspend fun createProfile(user: User): Result<Unit>

    /** 아이디(handle)는 바꾸지 않는다. 아이디를 뺀 나머지 프로필 값만 고친다. */
    suspend fun updateProfile(user: User): Result<Unit>
}

class HandleTakenException(handle: String) : Exception("이미 쓰는 아이디예요: $handle")
