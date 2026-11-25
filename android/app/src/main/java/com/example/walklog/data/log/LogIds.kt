package com.example.walklog.data.log

/**
 * 로그 id 규칙: 영문 · 숫자 · `_` · `-` 1~64자, `__`로 시작하면서 `__`로 끝나면 안 된다.
 * logs/ 문서 id 와 좋아요 · 저장 표시 문서 id 로 그대로 쓰이므로, Firestore 를 건드리기 전에 확인한다.
 */
object LogIds {
    private val ALLOWED = Regex("^[A-Za-z0-9_-]{1,64}$")

    // Firestore 는 __로 시작하고 끝나는 문서 id 를 예약어로 막는다
    private const val RESERVED_MARK = "__"

    fun isValid(id: String): Boolean =
        ALLOWED.matches(id) && !(id.startsWith(RESERVED_MARK) && id.endsWith(RESERVED_MARK))

    /** 규칙에 맞지 않으면 [IllegalArgumentException]을 던진다. */
    fun requireValid(id: String) {
        require(isValid(id)) { "Invalid log id: $id" }
    }
}
