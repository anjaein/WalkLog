package com.example.walklog.data.log

import com.example.walklog.data.firebase.FirestorePaths
import com.example.walklog.data.firebase.toFirestore
import com.example.walklog.data.firebase.walkLogFromFirestore
import com.example.walklog.data.model.FeedSort
import com.example.walklog.data.model.Visibility
import com.example.walklog.data.model.WalkLog
import com.example.walklog.data.photo.PhotoRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

// 여기 쿼리를 바꾸면 firebase/firestore.indexes.json 의 복합 색인도 같이 맞춰야 한다(에뮬레이터는 색인 없이도 돌아서 놓치기 쉽다).
class FirebaseLogRepository(
    private val firestore: FirebaseFirestore,
    private val photoRepository: PhotoRepository,
) : LogRepository {

    private val logs get() = firestore.collection(FirestorePaths.LOGS)

    override fun observeLog(logId: String): Flow<WalkLog?> =
        logs.document(logId).snapshots().map { snapshot ->
            snapshot.data?.let { walkLogFromFirestore(snapshot.id, it) }
        }

    override fun observeLogsByUser(uid: String, includePrivate: Boolean): Flow<List<WalkLog>> {
        var query: Query = logs.whereEqualTo("authorId", uid)
        if (!includePrivate) query = query.whereEqualTo("visibility", PUBLIC)
        return query.orderBy("startedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { it.toWalkLogs() }
    }

    override fun observeFeed(tag: String?, sort: FeedSort, limit: Long): Flow<List<WalkLog>> {
        var query: Query = logs.whereEqualTo("visibility", PUBLIC)
        if (tag != null) query = query.whereArrayContains("tags", tag)
        val orderField = when (sort) {
            FeedSort.RECENT -> "createdAt"
            FeedSort.POPULAR -> "likeCount"
        }
        return query.orderBy(orderField, Query.Direction.DESCENDING)
            .limit(limit)
            .snapshots()
            .map { it.toWalkLogs() }
    }

    override suspend fun createLog(log: WalkLog): Result<String> = runCatching {
        val now = System.currentTimeMillis()
        val ref = logs.document()
        val data = log.copy(id = "", likeCount = 0, saveCount = 0, createdAt = now, updatedAt = now).toFirestore()
        ref.set(data).await()
        ref.id
    }

    override suspend fun updateLog(log: WalkLog): Result<Unit> = runCatching {
        val data = log.toFirestore() - IMMUTABLE_FIELDS + ("updatedAt" to System.currentTimeMillis())
        logs.document(log.id).update(data).await()
        Unit
    }

    override suspend fun deleteLog(logId: String): Result<Unit> = runCatching {
        val ref = logs.document(logId)
        val snapshot = ref.get().await()
        val photos = snapshot.data?.let { walkLogFromFirestore(snapshot.id, it) }?.photos.orEmpty()
        // 사진 파일 하나를 못 지웠다고 로그 삭제까지 막으면 사용자가 로그를 영영 못 지우게 된다
        photos.forEach { photo -> runCatching { photoRepository.delete(photo) } }
        ref.delete().await()
        Unit
    }

    private fun QuerySnapshot.toWalkLogs(): List<WalkLog> =
        documents.mapNotNull { doc -> doc.data?.let { walkLogFromFirestore(doc.id, it) } }

    private companion object {
        val PUBLIC = Visibility.PUBLIC.name.lowercase()

        // 좋아요 · 저장 수는 트랜잭션으로만 바뀌고, 작성자 · 작성 시각은 처음 값을 지킨다
        val IMMUTABLE_FIELDS = setOf("likeCount", "saveCount", "authorId", "createdAt")
    }
}
