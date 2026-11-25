package com.example.walklog.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.example.walklog.R
import com.example.walklog.ui.theme.WalkLogTheme

enum class NavTab(@StringRes val labelRes: Int) {
    HOME(R.string.nav_home),
    MAP(R.string.nav_map),
    EXPLORE(R.string.nav_explore),
    ;

    val icon: ImageVector
        get() = when (this) {
            HOME -> WalkLogIcons.Home
            MAP -> WalkLogIcons.Map
            EXPLORE -> WalkLogIcons.Compass
        }
}

/**
 * 화면 아래에 떠 있는 알약 냅바. 아이콘만 있다.
 * 위치(아래 여백, 시스템 내비게이션 바 피하기)는 놓는 쪽에서 정한다.
 *
 * @param light 지도 탭처럼 어두운 화면 위에서는 밝은 냅바를 쓴다.
 */
@Composable
fun WalkLogNavBar(
    selected: NavTab?,
    onSelect: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
    light: Boolean = false,
) {
    val colors = WalkLogTheme.colors
    val sizes = WalkLogTheme.sizes
    val shapes = WalkLogTheme.shapes
    val motion = WalkLogTheme.motion

    val barColor = if (light) colors.background else colors.ink
    val itemColor = if (light) colors.ink else colors.background
    val activeColor = if (light) colors.ink else colors.background
    val activeItemColor = if (light) colors.background else colors.ink

    Row(
        modifier = modifier
            .shadow(WalkLogTheme.elevation.float, shapes.pill)
            .clip(shapes.pill)
            .background(barColor)
            .padding(sizes.navBarPadding)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(sizes.navBarItemGap),
    ) {
        NavTab.entries.forEach { tab ->
            val isSelected = tab == selected
            val background by animateColorAsState(
                targetValue = if (isSelected) activeColor else barColor,
                animationSpec = tween(motion.durationFast, easing = motion.easeOut),
                label = "navItemBackground",
            )
            val tint by animateColorAsState(
                targetValue = if (isSelected) activeItemColor else itemColor,
                animationSpec = tween(motion.durationFast, easing = motion.easeOut),
                label = "navItemTint",
            )
            Box(
                modifier = Modifier
                    .size(width = sizes.navBarItemWidth, height = sizes.navBarItemHeight)
                    .clip(shapes.pill)
                    .background(background)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) }),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = stringResource(tab.labelRes),
                    tint = tint,
                    modifier = Modifier.size(sizes.icon),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFBF8F2)
@Composable
private fun WalkLogNavBarPreview() {
    WalkLogTheme {
        Column(
            modifier = Modifier.padding(WalkLogTheme.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(WalkLogTheme.spacing.s16),
        ) {
            WalkLogNavBar(selected = NavTab.HOME, onSelect = {})
            Box(modifier = Modifier.background(WalkLogTheme.colors.mapBackground).padding(WalkLogTheme.spacing.s16)) {
                WalkLogNavBar(selected = NavTab.MAP, onSelect = {}, light = true)
            }
        }
    }
}
