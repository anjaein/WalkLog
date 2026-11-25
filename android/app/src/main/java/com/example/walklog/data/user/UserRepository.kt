package com.example.walklog.data.user

import com.example.walklog.data.HandleTakenException
import com.example.walklog.data.InvalidHandleException
import com.example.walklog.data.NetworkException
import com.example.walklog.data.model.User
import kotlinx.coroutines.flow.Flow

/** 네트워크가 안 되면 suspend 함수들은 [NetworkException]으로 실패한다. */
interface UserRepository {
    fun observeUser(uid: String): Flow<User?>

    /** 아이디가 [HandleRules]에 맞지 않으면 Firestore 를 보기 전에 [InvalidHandleException]으로 실패한다. */
    suspend fun isHandleAvailable(handle: String): Result<Boolean>

    /**
     * 아이디(handle)를 차지하면서 프로필을 처음 만든다.
     * 아이디가 [HandleRules]에 맞지 않으면 [InvalidHandleException], 이미 누가 쓰는 아이디면 [HandleTakenException]으로 실패한다.
     */
    suspend fun createProfile(user: User): Result<Unit>

    /** 아이디(handle)는 바꾸지 않는다. 아이디를 뺀 나머지 프로필 값만 고친다. */
    suspend fun updateProfile(user: User): Result<Unit>
}
