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
     * user.uid 가 로그인한 사용자가 아니면(로그인 안 함 포함) [IllegalStateException]으로 실패한다.
     */
    suspend fun createProfile(user: User): Result<Unit>

    /**
     * 아이디(handle)는 바꾸지 않는다. 아이디를 뺀 나머지 프로필 값만 고친다.
     * 쓰기는 연결이 있어야 한다. [NetworkException]은 서버가 고쳤는데 응답만 못 받았을 때도 올 수 있으며, 같은 내용으로 다시 시도하면 된다.
     * 프로필이 아직 없으면 [NoSuchElementException]으로 실패한다(처음 만들 때는 [createProfile]).
     * user.uid 가 로그인한 사용자가 아니면(로그인 안 함 포함) [IllegalStateException]으로 실패한다.
     */
    suspend fun updateProfile(user: User): Result<Unit>
}
