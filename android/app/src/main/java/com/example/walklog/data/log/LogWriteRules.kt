package com.example.walklog.data.log

import com.example.walklog.data.model.Visibility

enum class CreateDecision { CREATE, ALREADY_SAVED, ID_TAKEN }

enum class ChangeDecision { ALLOWED, NOT_FOUND, NOT_AUTHOR }

/**
 * 같은 id 로 이미 문서가 있을 때, 같은 작성자면 앞선 저장이 성공했는데 응답만 못 받은 재시도로 본다.
 * 덮어쓰면 그사이 쌓인 좋아요 · 저장 수가 0으로 돌아가므로 새로 쓰지 않는다.
 */
fun decideCreate(exists: Boolean, existingAuthorId: String?, uid: String): CreateDecision = when {
    !exists -> CreateDecision.CREATE
    existingAuthorId == uid -> CreateDecision.ALREADY_SAVED
    else -> CreateDecision.ID_TAKEN
}

/** 로그인하지 않았으면(uid == null) 로그가 있든 없든 작성자가 아닌 것으로 본다. */
fun decideChange(exists: Boolean, existingAuthorId: String?, uid: String?): ChangeDecision = when {
    uid == null -> ChangeDecision.NOT_AUTHOR
    !exists -> ChangeDecision.NOT_FOUND
    existingAuthorId == uid -> ChangeDecision.ALLOWED
    else -> ChangeDecision.NOT_AUTHOR
}

/** 나만 보기 로그는 작성자에게만 보인다. 로그인하지 않았으면(uid == null) 공개 로그만 보인다. */
fun isVisibleTo(visibility: Visibility, authorId: String?, uid: String?): Boolean =
    visibility == Visibility.PUBLIC || (uid != null && authorId == uid)

/** 앞뒤 공백을 지우고, 빈 태그와 겹치는 태그를 뺀다. 순서는 처음 나온 순서를 지킨다. */
fun normalizeTags(tags: List<String>): List<String> =
    tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
