package com.example.walklog.data.user

/** 아이디(handle) 규칙: 영어 소문자 · 숫자 · '.' · '_'만, 3~20자, '.'로 시작 · 끝나거나 '..'이 들어가면 안 된다. */
object HandleRules {
    private val ALLOWED = Regex("^[a-z0-9._]{3,20}$")

    fun normalize(handle: String): String = handle.trim().lowercase()

    fun isValid(normalized: String): Boolean =
        ALLOWED.matches(normalized) &&
            !normalized.startsWith('.') &&
            !normalized.endsWith('.') &&
            !normalized.contains("..")
}
