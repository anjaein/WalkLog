package com.example.walklog.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.example.walklog.ui.theme.WalkLogTheme

enum class PillSize { LARGE, MEDIUM, SMALL }

enum class PillStyle { INK, TOMATO, COBALT, LINE, GHOST }

/**
 * 알약 모양 버튼. 높이 L 56 · M 48 · S 40.
 * 꽉 채우려면 modifier에 fillMaxWidth()를 준다.
 */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: PillSize = PillSize.LARGE,
    style: PillStyle = PillStyle.INK,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    val colors = WalkLogTheme.colors
    val sizes = WalkLogTheme.sizes
    val spacing = WalkLogTheme.spacing
    val shapes = WalkLogTheme.shapes
    val motion = WalkLogTheme.motion

    val container = when (style) {
        PillStyle.INK -> colors.ink
        PillStyle.TOMATO -> colors.tomato
        PillStyle.COBALT -> colors.cobalt
        PillStyle.LINE -> colors.surface
        PillStyle.GHOST -> Color.Transparent
    }
    val content = when (style) {
        PillStyle.INK -> colors.background
        PillStyle.TOMATO -> colors.ink
        PillStyle.COBALT -> colors.onDark
        PillStyle.LINE -> colors.ink
        PillStyle.GHOST -> colors.inkSecondary
    }
    val height = when (size) {
        PillSize.LARGE -> sizes.pillLarge
        PillSize.MEDIUM -> sizes.pillMedium
        PillSize.SMALL -> sizes.pillSmall
    }
    val horizontalPadding = when (size) {
        PillSize.LARGE -> spacing.s24
        PillSize.MEDIUM -> spacing.s20
        PillSize.SMALL -> spacing.s16
    }
    val textStyle = if (size == PillSize.SMALL) WalkLogTheme.typography.buttonSmall else WalkLogTheme.typography.button
    val iconSize = if (size == PillSize.SMALL) sizes.iconSmall else sizes.icon

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) motion.pressScale else 1f,
        animationSpec = tween(motion.durationFast, easing = motion.easeOut),
        label = "pillPressScale",
    )

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else motion.disabledAlpha
            }
            .height(height)
            .clip(shapes.pill)
            .background(container)
            .then(
                if (style == PillStyle.LINE) Modifier.border(sizes.hairline, colors.hairline, shapes.pill) else Modifier,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(spacing.s6, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = content, modifier = Modifier.size(iconSize))
        }
        Text(text = text, style = textStyle, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (trailingIcon != null) {
            Icon(trailingIcon, contentDescription = null, tint = content, modifier = Modifier.size(iconSize))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFBF8F2)
@Composable
private fun PillButtonPreview() {
    WalkLogTheme {
        Column(
            modifier = Modifier.padding(WalkLogTheme.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(WalkLogTheme.spacing.s12),
        ) {
            PillButton("시작하기", onClick = {}, modifier = Modifier.fillMaxWidth())
            PillButton("오늘의 로그", onClick = {}, style = PillStyle.TOMATO, trailingIcon = WalkLogIcons.ChevronRight)
            PillButton("따라 걷기", onClick = {}, size = PillSize.MEDIUM, style = PillStyle.COBALT)
            PillButton("지도에서 보기", onClick = {}, size = PillSize.SMALL, style = PillStyle.LINE)
            PillButton("다음", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
        }
    }
}
