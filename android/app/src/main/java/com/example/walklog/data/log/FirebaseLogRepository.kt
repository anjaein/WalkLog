package com.example.walklog.data.log

import com.example.walklog.data.firebase.FirestorePaths
import com.example.walklog.data.firebase.toFirestore
import com.example.walklog.data.firebase.walkLogFromFirestore
import com.example.walklog.data.model.FeedSort
import com.example.walklog.data.model.Visibility
import com.example.walklog.data.model.WalkLog
import com.example.walklog.data.photo.PhotoRepository
import com.example.walklog.data.suspendRunCatching
import com.example.walklog.data.user.UserIds
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

// 여기 쿼리를 바꾸면 firebase/firestore.indexes.json 의 복합 색인도 같이 맞춰야 한다(에뮬레이터는 색인 없이도 돌아서 놓치기 쉽다).
class FirebaseLogRepository(
    private val firestore: FirebaseFirestore,
    private val photoRepository: PhotoRepository,
    private val currentUid: () -> String?,
) : LogRepository {

    private val logs get() = firestore.collection(FirestorePaths.LOGS)

    override fun observeLog(logId: String): Flow<WalkLog?> {
        // 잘못된 id 를 부른 자리에서 바로 죽이지 않고, 수집할 때 실패로 알린다
        if (!LogIds.isValid(logId)) return flow<WalkLog?> { LogIds.requireValid(logId) }
        return logs.document(logId).snapshots().map { snapshot ->
            snapshot.data?.let { walkLogFromFirestore(snapshot.id, it) }
                ?.takeIf { isVisibleTo(it.visibility, it.authorId, currentUid()) }
        }
    }

    override fun observeLogsByUser(uid: String, includePrivate: Boolean): Flow<List<WalkLog>> {
        if (!UserIds.isValid(uid)) return flow<List<WalkLog>> { UserIds.requireValid(uid) }
        var query: Query = logs.whereEqualTo("authorId", uid)
        // 남의 프로필에서 includePrivate 를 잘못 넘겨도 나만 보기 로그가 새지 않게, 본인일 때만 허용한다
        val withPrivate = includePrivate && uid == currentUid()
        if (!withPrivate) query = query.whereEqualTo("visibility", PUBLIC)
        return query.orderBy("startedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { it.toWalkLogs() }
    }

    override fun observeFeed(tag: String?, sort: FeedSort, limit: Long): Flow<List<WalkLog>> {
        // Query.limit 은 0 이하를 부른 자리에서 바로 던지므로, 수집할 때 실패로 알린다
        if (limit <= 0) return flow<List<WalkLog>> { require(limit > 0) { "Invalid feed limit: $limit" } }
        var query: Query = logs.whereEqualTo("visibility", PUBLIC)
        normalizeTag(tag)?.let { query = query.whereArrayContains("tags", it) }
        val orderField = when (sort) {
            FeedSort.RECENT -> "createdAt"
            FeedSort.POPULAR -> "likeCount"
        }
        return query.orderBy(orderField, Query.Direction.DESCENDING)
            .limit(limit)
            .snapshots()
            .map { it.toWalkLogs() }
    }

    override suspend fun createLog(log: WalkLog): Result<String> = suspendRunCatching {
        if (log.id.isNotEmpty()) LogIds.requireValid(log.id)
        val uid = currentUid() ?: throw IllegalStateException("Not signed in")
        val ref = if (log.id.isEmpty()) logs.document() else logs.document(log.id)
        // 트랜잭션 안에서 던진 예외는 SDK가 감쌀 수 있어서, 결과만 돌려받고 예외는 밖에서 던진다.
        val decision = firestore.runTransaction { tx ->
            val existing = tx.get(ref)
            val result = decideCreate(existing.exists(), existing.getString("authorId"), uid)
            if (result == CreateDecision.CREATE) {
                val now = System.currentTimeMillis()
                val fresh = log.copy(
                    id = "",
                    authorId = uid,
                    tags = normalizeTags(log.tags),
                    likeCount = 0,
                    saveCount = 0,
                    createdAt = now,
                    updatedAt = now,
                )
                tx.set(ref, fresh.toFirestore())
            }
            result
        }.await()
        if (decision == CreateDecision.ID_TAKEN) throw IllegalStateException("Log id already used by another author")
        ref.id
    }

    override suspend fun updateLog(log: WalkLog): Result<Unit> = suspendRunCatching {
        LogIds.requireValid(log.id)
        val uid = currentUid() ?: throw IllegalStateException("Not signed in")
        val ref = logs.document(log.id)
        val data = log.copy(tags = normalizeTags(log.tags)).toFirestore() - IMMUTABLE_FIELDS + ("updatedAt" to System.currentTimeMillis())
        val decision = firestore.runTransaction { tx ->
            val existing = tx.get(ref)
            val result = decideChange(existing.exists(), existing.getString("authorId"), uid)
            if (result == ChangeDecision.ALLOWED) tx.update(ref, data)
            result
        }.await()
        when (decision) {
            ChangeDecision.NOT_FOUND -> throw NoSuchElementException("Log not found: ${log.id}")
            ChangeDecision.NOT_AUTHOR -> throw IllegalStateException("Log belongs to another author: ${log.id}")
            ChangeDecision.ALLOWED -> Unit
        }
    }

    override suspend fun deleteLog(logId: String): Result<Unit> = suspendRunCatching {
        LogIds.requireValid(logId)
        val uid = currentUid() ?: throw IllegalStateException("Not signed in")
        val ref = logs.document(logId)
        // 화면을 나가 취소돼도 트랜잭션은 서버에서 그대로 끝나는데, 그 뒤 사진 정리만 빠지면 문서가 지워져 재시도로도 사진을 못 찾는다.
        // 그래서 사용자가 확인한 삭제는 트랜잭션부터 사진 정리까지 취소와 상관없이 끝까지 한다
        finishEvenIfCancelled {
            val outcome = firestore.runTransaction { tx ->
                val snapshot = tx.get(ref)
                val result = decideChange(snapshot.exists(), snapshot.getString("authorId"), uid)
                val photos = if (result == ChangeDecision.ALLOWED) {
                    tx.delete(ref)
                    snapshot.data?.let { walkLogFromFirestore(snapshot.id, it) }?.photos.orEmpty()
                } else {
                    emptyList()
                }
                result to photos
            }.await()
            val (decision, photos) = outcome
            if (decision == ChangeDecision.NOT_AUTHOR) throw IllegalStateException("Log belongs to another author: $logId")
            // 문서 삭제가 확정된 뒤에만 사진을 지운다. 사진부터 지우면 문서 삭제가 실패했을 때 사진 없는 로그가 남는다.
            // 사진 파일 하나를 못 지웠다고 로그 삭제까지 실패로 돌리면 안 된다.
            // 다만 오프라인이면 Storage 가 몇 분씩 재시도하며 삭제 결과를 붙잡으므로, 정리는 정해진 시간까지만 기다린다(못 지운 사진은 남는다)
            withTimeoutOrNull(PHOTO_CLEANUP_TIMEOUT_MS) {
                photos.forEach { photo -> suspendRunCatching { photoRepository.delete(photo) } }
            }
        }
    }

    private fun QuerySnapshot.toWalkLogs(): List<WalkLog> =
        documents.mapNotNull { doc -> doc.data?.let { walkLogFromFirestore(doc.id, it) } }

    private companion object {
        val PUBLIC = Visibility.PUBLIC.name.lowercase()

        const val PHOTO_CLEANUP_TIMEOUT_MS = 10_000L

        // 좋아요 · 저장 수는 트랜잭션으로만 바뀌고, 작성자 · 작성 시각은 처음 값을 지킨다
        val IMMUTABLE_FIELDS = setOf("likeCount", "saveCount", "authorId", "createdAt")
    }
}

/**
 * [block]을 취소와 상관없이 끝까지 실행한다. 다만 부른 쪽이 그사이 취소됐으면 결과 대신 CancellationException 을 던진다.
 * withContext 는 디스패처가 바뀔 때만 돌아오면서 취소를 확인하는데 NonCancellable 은 디스패처를 바꾸지 않아서, 직접 확인한다
 */
internal suspend fun <T> finishEvenIfCancelled(block: suspend () -> T): T {
    val result = withContext(NonCancellable) { block() }
    currentCoroutineContext().ensureActive()
    return result
}
