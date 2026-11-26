package com.example.walklog.data.save

import com.example.walklog.data.NotFoundException
import com.example.walklog.data.PermissionDeniedException
import com.example.walklog.data.firebase.FirestorePaths
import com.example.walklog.data.firebase.visibilityFromFirestore
import com.example.walklog.data.log.LogIds
import com.example.walklog.data.log.MarkDecision
import com.example.walklog.data.log.decideMark
import com.example.walklog.data.log.isVisibleTo
import com.example.walklog.data.mapDataErrors
import com.example.walklog.data.suspendRunCatching
import com.example.walklog.data.user.UserIds
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirebaseSaveRepository(
    private val firestore: FirebaseFirestore,
    private val currentUid: () -> String?,
) : SaveRepository {

    private fun savesOf(uid: String) =
        firestore.collection(FirestorePaths.USERS).document(uid).collection(FirestorePaths.SAVES)

    override fun observeSavedLogIds(uid: String): Flow<List<String>> {
        // 잘못된 uid 를 부른 자리에서 바로 죽이지 않고, 수집할 때 실패로 알린다
        if (!UserIds.isValid(uid)) return flow<List<String>> { UserIds.requireValid(uid) }.mapDataErrors()
        return savesOf(uid).orderBy("savedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot -> snapshot.documents.map { it.id } }
            .mapDataErrors()
    }

    override fun observeIsSaved(uid: String, logId: String): Flow<Boolean> {
        // 잘못된 id 를 부른 자리에서 바로 죽이지 않고, 수집할 때 실패로 알린다
        if (!UserIds.isValid(uid) || !LogIds.isValid(logId)) {
            return flow<Boolean> {
                UserIds.requireValid(uid)
                LogIds.requireValid(logId)
            }.mapDataErrors()
        }
        return savesOf(uid).document(logId).snapshots().map { it.exists() }.mapDataErrors()
    }

    override suspend fun setSaved(uid: String, logId: String, saved: Boolean): Result<Unit> = suspendRunCatching {
        UserIds.requireValid(uid)
        LogIds.requireValid(logId)
        if (uid != currentUid()) throw PermissionDeniedException("Not the signed-in user")
        val markerRef = savesOf(uid).document(logId)
        val logRef = firestore.collection(FirestorePaths.LOGS).document(logId)
        // 트랜잭션 안에서 던진 예외는 SDK가 감쌀 수 있어서, 결과만 돌려받고 예외는 밖에서 던진다.
        val decision = firestore.runTransaction { transaction ->
            val markerExists = transaction.get(markerRef).exists()
            val log = transaction.get(logRef)
            val logExists = log.exists()
            val canMark = logExists &&
                isVisibleTo(visibilityFromFirestore(log.get("visibility")), log.get("authorId") as? String, uid)
            // 표시 문서가 이미 원하는 상태면 수를 건드리지 않아야, 두 번 눌려도 저장 수가 어긋나지 않는다
            val result = decideMark(saved, markerExists, canMark)
            if (result == MarkDecision.CHANGE) {
                if (saved) {
                    transaction.set(markerRef, mapOf("savedAt" to System.currentTimeMillis()))
                } else {
                    transaction.delete(markerRef)
                }
                // 지워진 로그는 수를 고칠 곳이 없다. 표시만 지워서 지워진 로그도 보관함에서 뺄 수 있게 한다
                if (logExists) {
                    transaction.update(logRef, "saveCount", FieldValue.increment(if (saved) 1L else -1L))
                }
            }
            result
        }.await()
        // 남의 나만 보기 로그도 없는 로그와 같은 실패로 돌려서, 그런 로그가 있는지 드러나지 않게 한다
        if (decision == MarkDecision.LOG_NOT_FOUND) throw NotFoundException("Log not found: $logId")
    }
}
