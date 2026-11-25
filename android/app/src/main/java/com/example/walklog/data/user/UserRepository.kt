package com.example.walklog.data.user

import com.example.walklog.data.HandleTakenException
import com.example.walklog.data.InvalidHandleException
import com.example.walklog.data.NetworkException
import com.example.walklog.data.model.User
import kotlinx.coroutines.flow.Flow

/** 네트워크가 안 되면 suspend 함수들은 [NetworkException]으로 실패한다. */
interface UserRepository {
    /** 프로필이 없으면 null. uid 가 [UserIds] 규칙에 맞지 않으면 수집할 때 [IllegalArgumentException]으로 실패한다. */
    fun observeUser(uid: String): Flow<User?>

    /**
     * 아무도 안 쓰는 아이디면 true. 이미 있는 아이디라도 로그인한 사용자 본인의 것이면 true 를 돌려준다
     * (온보딩을 다시 거칠 때 지금 내 아이디를 그대로 고를 수 있게).
     * 아이디가 [HandleRules]에 맞지 않으면 Firestore 를 보기 전에 [InvalidHandleException]으로 실패한다.
     */
    suspend fun isHandleAvailable(handle: String): Result<Boolean>

    /**
     * 아이디(handle)를 차지하면서 프로필을 만든다(온보딩).
     * users/{uid} 문서가 아직 없으면 user 값으로 새로 만든다. joinedAt 이 0 이면 지금 시각으로 채운다.
     * 이미 있으면 name 과 handle 만 바꾸고, 나머지(bio · area · photoUrl · headerColor · defaultVisibility · language · joinedAt)는 그대로 둔다.
     * 아이디를 바꿨으면 예전 아이디(handles 문서)는, 아직 이 사용자의 것일 때만 같은 트랜잭션에서 풀어 준다.
     * 아이디가 [HandleRules]에 맞지 않으면 [InvalidHandleException], 이미 누가 쓰는 아이디면 [HandleTakenException]으로 실패한다.
     * user.uid 가 [UserIds] 규칙에 맞지 않으면 [IllegalArgumentException],
     * 로그인한 사용자가 아니면(로그인 안 함 포함) [IllegalStateException]으로 실패한다.
     */
    suspend fun createProfile(user: User): Result<Unit>

    /**
     * 아이디(handle)는 바꾸지 않는다. 아이디를 뺀 나머지 프로필 값만 고친다.
     * 쓰기는 연결이 있어야 한다. [NetworkException]은 서버가 고쳤는데 응답만 못 받았을 때도 올 수 있으며, 같은 내용으로 다시 시도하면 된다.
     * 프로필이 아직 없으면 [NoSuchElementException]으로 실패한다(처음 만들 때는 [createProfile]).
     * user.uid 가 [UserIds] 규칙에 맞지 않으면 [IllegalArgumentException],
     * 로그인한 사용자가 아니면(로그인 안 함 포함) [IllegalStateException]으로 실패한다.
     */
    suspend fun updateProfile(user: User): Result<Unit>
}
