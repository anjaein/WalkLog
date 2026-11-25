package com.example.walklog.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

@Immutable
data class WalkLogShapes(
    val chip: CornerBasedShape = RoundedCornerShape(12.dp),
    val card: CornerBasedShape = RoundedCornerShape(16.dp),
    val modal: CornerBasedShape = RoundedCornerShape(20.dp),
    val widget: CornerBasedShape = RoundedCornerShape(24.dp),
    // 아래에서 올라오는 시트는 위 두 모서리만 둥글다
    val sheet: CornerBasedShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    val pill: CornerBasedShape = RoundedCornerShape(percent = 50),
)

internal fun WalkLogShapes.toMaterialShapes() = Shapes(
    small = chip,
    medium = card,
    large = widget,
)
