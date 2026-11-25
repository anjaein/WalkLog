package com.example.walklog.data.model

/** 사용자 프로필. Firestore users/{uid} 문서와 같은 모양이다. 시각은 모두 epoch 밀리초. */
data class User(
    val uid: String,
    val name: String,
    val handle: String,
    val photoUrl: String? = null,
    val bio: String = "",
    val area: String = "",
    val headerColor: HeaderColor = HeaderColor.TOMATO,
    val defaultVisibility: Visibility = Visibility.PUBLIC,
    val language: String = "ko",
    val joinedAt: Long = 0L,
)

enum class HeaderColor { TOMATO, COBALT, LEMON, MINT, INK }

enum class Visibility { PUBLIC, PRIVATE }
