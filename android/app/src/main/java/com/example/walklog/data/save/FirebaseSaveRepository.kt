package com.example.walklog.data.save

import com.example.walklog.data.firebase.FirestorePaths
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirebaseSaveRepository(private val firestore: FirebaseFirestore) : SaveRepository {

    private fun savesOf(uid: String) =
        firestore.collection(FirestorePaths.USERS).document(uid).collection(FirestorePaths.SAVES)

    override fun observeSavedLogIds(uid: String): Flow<List<String>> =
        savesOf(uid).orderBy("savedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot -> snapshot.documents.map { it.id } }

    override fun observeIsSaved(uid: String, logId: String): Flow<Boolean> =
        savesOf(uid).document(logId).snapshots().map { it.exists() }

    override suspend fun setSaved(uid: String, logId: String, saved: Boolean): Result<Unit> = runCatching {
        val markerRef = savesOf(uid).document(logId)
        val logRef = firestore.collection(FirestorePaths.LOGS).document(logId)
        firestore.runTransaction<Unit> { transaction ->
            // 표시 문서가 이미 원하는 상태면 수를 건드리지 않아야, 두 번 눌려도 저장 수가 어긋나지 않는다
            val markerExists = transaction.get(markerRef).exists()
            val logExists = transaction.get(logRef).exists()
            if (markerExists != saved) {
                if (saved) {
                    transaction.set(markerRef, mapOf("savedAt" to System.currentTimeMillis()))
                } else {
                    transaction.delete(markerRef)
                }
                // 지워진 로그는 수를 고칠 곳이 없다. 표시만 지워서 지워진 로그도 해제할 수 있게 한다
                if (logExists) {
                    transaction.update(logRef, "saveCount", FieldValue.increment(if (saved) 1L else -1L))
                }
            }
        }.await()
        Unit
    }
}
