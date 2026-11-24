package com.example.walklog.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class WalkLogShapes(
    val chip: Shape = RoundedCornerShape(12.dp),
    val card: Shape = RoundedCornerShape(16.dp),
    val modal: Shape = RoundedCornerShape(20.dp),
    val widget: Shape = RoundedCornerShape(24.dp),
    // 아래에서 올라오는 시트는 위 두 모서리만 둥글다
    val sheet: Shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    val pill: Shape = RoundedCornerShape(percent = 50),
)

internal fun WalkLogShapes.toMaterialShapes() = Shapes(
    small = chip,
    medium = card,
    large = widget,
)
