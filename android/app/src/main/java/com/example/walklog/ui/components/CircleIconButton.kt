package com.example.walklog.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.example.walklog.ui.theme.WalkLogTheme

enum class CircleIconButtonStyle {
    // 흰 원 (기본)
    PLAIN,

    // 흰 원 + 얇은 테두리 (홈 설정 버튼)
    LINE,

    // 흰 원 + 그림자 (사진 · 지도 위에 뜬 뒤로 · 메뉴 버튼)
    FLOAT,
}

/**
 * 48×48 원형 아이콘 버튼. 아이콘만 있으므로 contentDescription은 꼭 넣는다.
 */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: CircleIconButtonStyle = CircleIconButtonStyle.PLAIN,
    enabled: Boolean = true,
) {
    val colors = WalkLogTheme.colors
    val sizes = WalkLogTheme.sizes
    val shapes = WalkLogTheme.shapes
    val motion = WalkLogTheme.motion

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) motion.pressScaleSmall else 1f,
        animationSpec = tween(motion.durationFast, easing = motion.easeOut),
        label = "circleIconPressScale",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else motion.disabledAlpha
            }
            .size(sizes.iconButton)
            .then(
                if (style == CircleIconButtonStyle.FLOAT) Modifier.shadow(WalkLogTheme.elevation.float, shapes.pill) else Modifier,
            )
            .clip(shapes.pill)
            .background(colors.surface)
            .then(
                if (style == CircleIconButtonStyle.LINE) Modifier.border(sizes.hairline, colors.hairline, shapes.pill) else Modifier,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = colors.ink, modifier = Modifier.size(sizes.icon))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFBF8F2)
@Composable
private fun CircleIconButtonPreview() {
    WalkLogTheme {
        Row(
            modifier = Modifier.padding(WalkLogTheme.spacing.gutter),
            horizontalArrangement = Arrangement.spacedBy(WalkLogTheme.spacing.s12),
        ) {
            CircleIconButton(WalkLogIcons.Back, contentDescription = "뒤로", onClick = {})
            CircleIconButton(WalkLogIcons.Gear, contentDescription = "설정", onClick = {}, style = CircleIconButtonStyle.LINE)
            CircleIconButton(WalkLogIcons.More, contentDescription = "메뉴", onClick = {}, style = CircleIconButtonStyle.FLOAT)
        }
    }
}
