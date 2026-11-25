package com.example.walklog.data.user

/**
 * 아이디(handle) 규칙: 영어 소문자 · 숫자 · '.' · '_'만, 3~20자, '.'로 시작 · 끝나거나 '..'이 들어가면 안 된다.
 * '__'로 시작하면서 '__'로 끝나는 아이디도 안 된다.
 */
object HandleRules {
    private val ALLOWED = Regex("^[a-z0-9._]{3,20}$")

    // handles/{handle} 문서 id 로 쓰이는데, Firestore 는 __로 시작하고 끝나는 문서 id 를 예약어로 막는다
    private const val RESERVED_MARK = "__"

    fun normalize(handle: String): String = handle.trim().lowercase()

    fun isValid(normalized: String): Boolean =
        ALLOWED.matches(normalized) &&
            !normalized.startsWith('.') &&
            !normalized.endsWith('.') &&
            !normalized.contains("..") &&
            !(normalized.startsWith(RESERVED_MARK) && normalized.endsWith(RESERVED_MARK))
}
