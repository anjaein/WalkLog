package com.example.walklog.data.user

/**
 * uid 규칙: 비어 있거나 공백뿐이면 안 되고, '/'가 들어가거나 `.` · `..` 자체이거나 `__`로 시작하면서 `__`로 끝나면 안 된다.
 * users/{uid} 문서 경로로 그대로 쓰이므로, Firestore 를 건드리기 전에 확인한다.
 */
object UserIds {
    // Firestore 는 __로 시작하고 끝나는 문서 id 를 예약어로 막는다
    private const val RESERVED_MARK = "__"

    fun isValid(uid: String): Boolean =
        uid.isNotBlank() &&
            !uid.contains('/') &&
            uid != "." &&
            uid != ".." &&
            !(uid.startsWith(RESERVED_MARK) && uid.endsWith(RESERVED_MARK))

    /** 규칙에 맞지 않으면 [IllegalArgumentException]을 던진다. */
    fun requireValid(uid: String) {
        require(isValid(uid)) { "Invalid uid: $uid" }
    }
}
