package com.example.walklog.data.like

import com.example.walklog.data.firebase.FirestorePaths
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirebaseLikeRepository(private val firestore: FirebaseFirestore) : LikeRepository {

    private fun likeRef(uid: String, logId: String) =
        firestore.collection(FirestorePaths.USERS).document(uid)
            .collection(FirestorePaths.LIKES).document(logId)

    override fun observeIsLiked(uid: String, logId: String): Flow<Boolean> =
        likeRef(uid, logId).snapshots().map { it.exists() }

    override suspend fun setLiked(uid: String, logId: String, liked: Boolean): Result<Unit> = runCatching {
        val markerRef = likeRef(uid, logId)
        val logRef = firestore.collection(FirestorePaths.LOGS).document(logId)
        firestore.runTransaction<Unit> { transaction ->
            // 표시 문서가 이미 원하는 상태면 수를 건드리지 않아야, 두 번 눌려도 좋아요 수가 어긋나지 않는다
            val markerExists = transaction.get(markerRef).exists()
            val logExists = transaction.get(logRef).exists()
            if (markerExists != liked) {
                if (liked) {
                    transaction.set(markerRef, mapOf("likedAt" to System.currentTimeMillis()))
                } else {
                    transaction.delete(markerRef)
                }
                // 지워진 로그는 수를 고칠 곳이 없다. 표시만 지워서 지워진 로그도 해제할 수 있게 한다
                if (logExists) {
                    transaction.update(logRef, "likeCount", FieldValue.increment(if (liked) 1L else -1L))
                }
            }
        }.await()
        Unit
    }
}
