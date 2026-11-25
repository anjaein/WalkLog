package com.example.walklog.data.user

import com.example.walklog.data.firebase.FirestorePaths
import com.example.walklog.data.firebase.toFirestore
import com.example.walklog.data.firebase.userFromFirestore
import com.example.walklog.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirebaseUserRepository(private val firestore: FirebaseFirestore) : UserRepository {

    private fun userDoc(uid: String) = firestore.collection(FirestorePaths.USERS).document(uid)

    private fun handleDoc(handle: String) = firestore.collection(FirestorePaths.HANDLES).document(handle)

    override fun observeUser(uid: String): Flow<User?> =
        userDoc(uid).snapshots().map { snapshot ->
            snapshot.data?.let { userFromFirestore(uid, it) }
        }

    override suspend fun isHandleAvailable(handle: String): Result<Boolean> = runCatching {
        !handleDoc(normalize(handle)).get().await().exists()
    }

    override suspend fun createProfile(user: User): Result<Unit> = runCatching {
        val handle = normalize(user.handle)
        val handleRef = handleDoc(handle)
        val profile = user.copy(
            handle = handle,
            joinedAt = if (user.joinedAt == 0L) System.currentTimeMillis() else user.joinedAt,
        )
        // 트랜잭션 안에서 던진 예외는 SDK가 감쌀 수 있어서, 결과만 돌려받고 예외는 밖에서 던진다.
        val claimed = firestore.runTransaction { tx ->
            val existing = tx.get(handleRef)
            val oldHandle = tx.get(userDoc(user.uid)).getString("handle")
            if (existing.exists() && existing.getString("uid") != user.uid) {
                false
            } else {
                // 온보딩에서 아이디를 바꿔 다시 만들면 예전 아이디를 풀어 줘야 다른 사람이 쓸 수 있다
                if (oldHandle != null && oldHandle != handle) tx.delete(handleDoc(oldHandle))
                tx.set(handleRef, mapOf("uid" to user.uid))
                tx.set(userDoc(user.uid), profile.toFirestore())
                true
            }
        }.await()
        if (!claimed) throw HandleTakenException(handle)
    }

    override suspend fun updateProfile(user: User): Result<Unit> = runCatching {
        // handles 문서와 어긋나지 않도록 아이디와 가입 시각은 덮어쓰지 않는다.
        val fields = user.toFirestore() - "handle" - "joinedAt"
        userDoc(user.uid).update(fields).await()
        Unit
    }

    private fun normalize(handle: String) = handle.trim().lowercase()
}
