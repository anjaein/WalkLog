package com.example.walklog.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.example.walklog.ui.theme.WalkLogTheme

/**
 * 고르는 칩 (탐색 주제, 추천 · 첫 사진 같은 빠른 선택).
 * 기본 높이 36, large = true면 40 (탐색 필터 줄).
 */
@Composable
fun WalkLogChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    large: Boolean = false,
) {
    val colors = WalkLogTheme.colors
    val sizes = WalkLogTheme.sizes
    val shapes = WalkLogTheme.shapes
    val motion = WalkLogTheme.motion

    val container by animateColorAsState(
        targetValue = if (selected) colors.ink else colors.surface,
        animationSpec = tween(motion.durationFast),
        label = "chipContainer",
    )
    val content by animateColorAsState(
        targetValue = if (selected) colors.background else colors.ink,
        animationSpec = tween(motion.durationFast),
        label = "chipContent",
    )

    Box(
        modifier = modifier
            .height(if (large) sizes.chipLarge else sizes.chip)
            .clip(shapes.pill)
            .background(container)
            .then(if (selected) Modifier else Modifier.border(sizes.hairline, colors.hairline, shapes.pill))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = if (large) WalkLogTheme.spacing.s16 else sizes.chipPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = WalkLogTheme.typography.labelBold, color = content, maxLines = 1)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFBF8F2)
@Composable
private fun WalkLogChipPreview() {
    WalkLogTheme {
        Row(
            modifier = Modifier.padding(WalkLogTheme.spacing.gutter),
            horizontalArrangement = Arrangement.spacedBy(WalkLogTheme.spacing.s8),
        ) {
            WalkLogChip("내 주변 2km", selected = true, onClick = {}, large = true)
            WalkLogChip("단풍", selected = false, onClick = {}, large = true)
            WalkLogChip("추천", selected = false, onClick = {})
        }
    }
}
