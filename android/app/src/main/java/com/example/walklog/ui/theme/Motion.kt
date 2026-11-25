package com.example.walklog.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable

@Immutable
data class WalkLogMotion(
    val easeOut: Easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f),
    val easeInOut: Easing = CubicBezierEasing(0.77f, 0f, 0.175f, 1f),
    val durationFast: Int = 160,
    val duration: Int = 240,
    val durationSlow: Int = 420,
    // 누를 때 살짝 줄어드는 비율 (버튼 0.97, 작은 아이콘 버튼 0.94)
    val pressScale: Float = 0.97f,
    val pressScaleSmall: Float = 0.94f,
    val disabledAlpha: Float = 0.35f,
)
