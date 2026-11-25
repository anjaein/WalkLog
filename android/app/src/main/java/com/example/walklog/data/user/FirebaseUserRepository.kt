package com.example.walklog.data.user

import com.example.walklog.data.HandleTakenException
import com.example.walklog.data.InvalidHandleException
import com.example.walklog.data.NotFoundException
import com.example.walklog.data.PermissionDeniedException
import com.example.walklog.data.firebase.FirestorePaths
import com.example.walklog.data.firebase.toFirestore
import com.example.walklog.data.firebase.userFromFirestore
import com.example.walklog.data.mapDataErrors
import com.example.walklog.data.model.User
import com.example.walklog.data.suspendRunCatching
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirebaseUserRepository(
    private val firestore: FirebaseFirestore,
    private val currentUid: () -> String?,
) : UserRepository {

    private fun userDoc(uid: String) = firestore.collection(FirestorePaths.USERS).document(uid)

    private fun handleDoc(handle: String) = firestore.collection(FirestorePaths.HANDLES).document(handle)

    override fun observeUser(uid: String): Flow<User?> {
        // 잘못된 uid 를 부른 자리에서 바로 죽이지 않고, 수집할 때 실패로 알린다
        if (!UserIds.isValid(uid)) return flow<User?> { UserIds.requireValid(uid) }.mapDataErrors()
        return userDoc(uid).snapshots().map { snapshot ->
            snapshot.data?.let { userFromFirestore(uid, it) }
        }.mapDataErrors()
    }

    override suspend fun isHandleAvailable(handle: String): Result<Boolean> = suspendRunCatching {
        val snapshot = handleDoc(validHandle(handle)).get().await()
        val uid = currentUid()
        !snapshot.exists() || (uid != null && snapshot.getString("uid") == uid)
    }

    override suspend fun createProfile(user: User): Result<Unit> = suspendRunCatching {
        UserIds.requireValid(user.uid)
        requireSignedInAs(user.uid)
        val handle = validHandle(user.handle)
        val handleRef = handleDoc(handle)
        val userRef = userDoc(user.uid)
        val newJoinedAt = if (user.joinedAt == 0L) System.currentTimeMillis() else user.joinedAt
        // 트랜잭션 안에서 던진 예외는 SDK가 감쌀 수 있어서, 결과만 돌려받고 예외는 밖에서 던진다.
        val claimed = firestore.runTransaction { tx ->
            // 트랜잭션은 모든 읽기를 쓰기보다 먼저 해야 한다
            val existing = tx.get(handleRef)
            val existingUser = tx.get(userRef)
            // 저장된 값이 규칙에 안 맞으면 문서 경로를 만들 수 없으니 풀어 줄 대상에서 뺀다
            val oldHandleRef = existingUser.getString("handle")
                ?.takeIf { it != handle && HandleRules.isValid(it) }
                ?.let { handleDoc(it) }
            val oldHandleOwner = oldHandleRef?.let { tx.get(it).getString("uid") }
            if (existing.exists() && existing.getString("uid") != user.uid) {
                false
            } else {
                // 온보딩에서 아이디를 바꿔 다시 만들면 예전 아이디를 풀어 줘야 다른 사람이 쓸 수 있다.
                // 그사이 다른 사람이 그 아이디를 차지했을 수 있어서, 아직 내 것일 때만 지운다
                if (oldHandleRef != null && oldHandleOwner == user.uid) tx.delete(oldHandleRef)
                tx.set(handleRef, mapOf("uid" to user.uid))
                if (existingUser.exists()) {
                    // 온보딩을 다시 거쳐도 이미 고친 프로필 값과 처음 가입한 시각은 지킨다
                    tx.update(userRef, mapOf("name" to user.name, "handle" to handle))
                } else {
                    tx.set(userRef, user.copy(handle = handle, joinedAt = newJoinedAt).toFirestore())
                }
                true
            }
        }.await()
        if (!claimed) throw HandleTakenException(handle)
    }

    override suspend fun updateProfile(user: User): Result<Unit> = suspendRunCatching {
        UserIds.requireValid(user.uid)
        requireSignedInAs(user.uid)
        // handles 문서와 어긋나지 않도록 아이디와 가입 시각은 덮어쓰지 않는다.
        val fields = user.toFirestore() - "handle" - "joinedAt"
        val ref = userDoc(user.uid)
        val updated = firestore.runTransaction { tx ->
            if (tx.get(ref).exists()) {
                tx.update(ref, fields)
                true
            } else {
                false
            }
        }.await()
        if (!updated) throw NotFoundException("User not found: ${user.uid}")
    }

    private fun requireSignedInAs(uid: String) {
        if (uid != currentUid()) throw PermissionDeniedException("Not the signed-in user")
    }

    // '/' 가 들어간 아이디는 Firestore 문서 경로를 깨뜨리므로, 문서를 건드리기 전에 규칙부터 확인한다
    private fun validHandle(handle: String): String {
        val normalized = HandleRules.normalize(handle)
        if (!HandleRules.isValid(normalized)) throw InvalidHandleException(normalized)
        return normalized
    }
}
