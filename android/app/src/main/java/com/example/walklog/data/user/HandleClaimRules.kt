package com.example.walklog.data.user

sealed interface ClaimDecision {
    /** 새 아이디를 다른 사람이 쓰고 있다. 아무것도 쓰지 않는다. */
    data object Taken : ClaimDecision

    /** 새 아이디를 차지한다. [releaseHandle]이 있으면 그 예전 아이디 문서를 지운다. */
    data class Claim(val releaseHandle: String?) : ClaimDecision
}

/**
 * 예전 아이디가 풀어 줄 후보인지 본다. 새 아이디와 같거나, 규칙에 맞지 않으면(문서 경로를 만들 수 없다) null.
 * 트랜잭션은 이 후보의 문서만 읽어 주인을 확인한다.
 */
fun releaseCandidate(oldHandle: String?, newHandle: String): String? =
    oldHandle?.takeIf { it != newHandle && HandleRules.isValid(it) }

/**
 * 새 아이디 문서가 있는데 주인이 내가 아니면(주인 값이 없는 것도 포함) 차지할 수 없다.
 * 예전 아이디는 그사이 다른 사람이 차지했을 수 있어서, 아직 내 것일 때만 풀어 준다.
 * 같은 아이디로 다시 만들면 풀어 줄 것 없이 다시 차지한다.
 */
fun decideClaim(
    newHandle: String,
    newHandleExists: Boolean,
    newHandleOwner: String?,
    oldHandle: String?,
    oldHandleOwner: String?,
    myUid: String,
): ClaimDecision {
    if (newHandleExists && newHandleOwner != myUid) return ClaimDecision.Taken
    val release = releaseCandidate(oldHandle, newHandle)?.takeIf { oldHandleOwner == myUid }
    return ClaimDecision.Claim(release)
}
