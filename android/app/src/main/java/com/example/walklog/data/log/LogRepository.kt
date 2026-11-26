package com.example.walklog.data.log

import com.example.walklog.data.DataException
import com.example.walklog.data.NetworkException
import com.example.walklog.data.NotFoundException
import com.example.walklog.data.PermissionDeniedException
import com.example.walklog.data.TooManyRequestsException
import com.example.walklog.data.UnknownDataException
import com.example.walklog.data.model.FeedSort
import com.example.walklog.data.model.WalkLog
import com.example.walklog.data.user.UserIds
import kotlinx.coroutines.flow.Flow

/**
 * 모든 실패(Result · Flow 오류)는 [DataException]이다. 아래 함수마다 적은 것 말고도
 * 네트워크가 안 되면 [NetworkException], 보안 규칙에 막히면 [PermissionDeniedException],
 * 사용 한도를 넘으면 [TooManyRequestsException], 그 밖은 [UnknownDataException](원래 예외는 cause)으로 실패한다.
 * id 가 [LogIds] · [UserIds] 규칙에 맞지 않는 것 같은 호출 실수도 [UnknownDataException](cause 는 IllegalArgumentException)이다.
 * cause 가 IllegalArgumentException 인 [UnknownDataException]은 잘못된 id 같은 입력 실수라 확실히 실패한 것이다. 같은 값으로 다시 시도하지 않는다.
 */
interface LogRepository {
    /**
     * 로그 하나를 지켜본다. 지워졌거나 없으면 null. logId 가 [LogIds] 규칙에 맞지 않으면 수집할 때 [UnknownDataException]으로 실패한다.
     * 나만 보기 로그는 작성자에게만 보인다. 작성자가 아니면(로그인 안 함 포함) 없는 로그처럼 null 을 준다.
     */
    fun observeLog(logId: String): Flow<WalkLog?>

    /**
     * 한 사용자의 로그, 최근 순. 내 프로필이면 includePrivate = true 로 나만 보기 로그까지 받는다.
     * includePrivate = true 여도 uid 가 지금 로그인한 사용자가 아니면(로그인 안 함 포함) 공개 로그만 준다.
     * 로그인한 사용자는 부를 때 한 번 확인하므로, 로그인 · 로그아웃 뒤에는 다시 불러야 한다.
     * uid 가 [UserIds] 규칙에 맞지 않으면 수집할 때 [UnknownDataException]으로 실패한다.
     */
    fun observeLogsByUser(uid: String, includePrivate: Boolean): Flow<List<WalkLog>>

    /** 탐색 피드: 공개 로그만. tag 를 주면 그 태그가 붙은 로그만 거른다. limit 이 0 이하면 수집할 때 [UnknownDataException]으로 실패한다. */
    fun observeFeed(tag: String? = null, sort: FeedSort = FeedSort.RECENT, limit: Long = 50): Flow<List<WalkLog>>

    /**
     * 지금 로그인한 사용자의 새 로그로 저장하고 logId 를 돌려준다. 태그는 [normalizeTags]로 다듬어 저장한다.
     * 작성자는 log.authorId 와 상관없이 로그인한 uid 로 채우고, 좋아요 수 · 저장 수 · 작성 시각은 무시하고 새로 채운다.
     * 로그인하지 않았으면 [PermissionDeniedException]으로 실패한다.
     *
     * log.id 가 비어 있으면 새 id 를 만들고, 있으면 그 id 로 저장한다([LogIds] 규칙에 맞지 않으면 [UnknownDataException]).
     * 쓰기는 연결이 있어야 한다. [NetworkException]은 서버가 저장했는데 응답만 못 받았을 때도 올 수 있으므로,
     * 화면은 초안마다 id 를 한 번 만들어(예: UUID) 재시도할 때도 같은 id 를 넘겨야 한다. 새 id 로 다시 시도하면 로그가 두 개 생길 수 있다.
     *
     * 재시도는 이미 있는 로그를 덮어쓰지 않는다: 같은 작성자의 로그가 이미 있으면 그대로 두고 성공으로 그 id 를 돌려주고,
     * 다른 작성자의 로그가 그 id 를 쓰고 있으면 [PermissionDeniedException]으로 실패한다.
     * 그래서 첫 시도가 실제로는 저장됐고 그 뒤 사용자가 초안을 고쳤다면, 재시도는 성공해도 예전 내용이 남는다.
     * 화면은 재시도가 성공한 뒤 고친 초안으로 [updateLog]를 불러야 한다.
     */
    suspend fun createLog(log: WalkLog): Result<String>

    /**
     * 좋아요 수 · 저장 수 · 작성자 · 작성 시각은 바꾸지 않는다. 태그는 [normalizeTags]로 다듬어 저장한다.
     * 로그가 없으면 [NotFoundException], 로그인하지 않았거나 저장된 작성자가 로그인한 사용자가 아니면 [PermissionDeniedException],
     * log.id 가 [LogIds] 규칙에 맞지 않으면 [UnknownDataException]으로 실패한다.
     * 쓰기는 연결이 있어야 한다. [NetworkException]은 서버가 고쳤는데 응답만 못 받았을 때도 올 수 있으며, 같은 내용으로 다시 시도하면 된다.
     * 이번 수정을 위해 새로 올린 사진은, 수정이 확실히 안 됐을 때([NotFoundException] · [PermissionDeniedException] ·
     * cause 가 IllegalArgumentException 인 [UnknownDataException])만 화면이 지운다. 이 실패들은 다시 시도하지 않는다.
     * 그 밖의 실패([NetworkException] · 나머지 [UnknownDataException])는 이미 저장됐을 수 있으니 사진을 지우지 말고 같은 내용으로 다시 시도한다.
     * [TooManyRequestsException]도 사진을 지우지 말고, 한도가 풀린 뒤(나중에) 같은 내용으로 다시 시도한다.
     * 수정이 성공하면 목록에서 뺀 사진 파일은 화면이 지운다(Repository 는 지우지 않는다).
     */
    suspend fun updateLog(log: WalkLog): Result<Unit>

    /**
     * 로그와 그 로그의 사진 파일을 함께 지운다. 이미 없는 로그면 지울 것 없이 성공한다(응답을 못 받은 삭제를 다시 시도해도 성공하도록).
     * 로그인하지 않았거나 저장된 작성자가 로그인한 사용자가 아니면 [PermissionDeniedException],
     * logId 가 [LogIds] 규칙에 맞지 않으면 [UnknownDataException]으로 실패한다.
     * 쓰기는 연결이 있어야 한다. [NetworkException]은 서버가 지웠는데 응답만 못 받았을 때도 올 수 있다.
     * 사진 파일은 로그 문서가 지워진 뒤에 지우며, 사진을 못 지워도 실패로 돌리지 않는다(사진 정리는 최대 10초까지만 기다린다).
     * 한 번 부르면 화면을 나가 호출한 코루틴이 취소돼도 삭제와 사진 정리를 끝까지 한다. 취소된 쪽은 성공 · 실패 결과 대신 CancellationException 을 받는다.
     * 알려진 문제: 서버가 로그를 지운 뒤 응답을 못 받으면, 재시도는 지울 로그를 못 찾아 성공하고 사진 파일은 남을 수 있다(서버 쪽 정리는 없다).
     */
    suspend fun deleteLog(logId: String): Result<Unit>
}
