package com.example.walklog.data.firebase

/** Firestore 컬렉션 이름. users/{uid}/likes, users/{uid}/saves 는 사용자 문서 아래 하위 컬렉션이다. */
object FirestorePaths {
    const val USERS = "users"
    const val HANDLES = "handles"
    const val LOGS = "logs"
    const val LIKES = "likes"
    const val SAVES = "saves"
}
