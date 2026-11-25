package com.example.walklog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.example.walklog.ui.theme.WalkLogTheme

/**
 * 모서리 16 카드. 안쪽 여백은 쓰는 곳에서 정한다.
 * onClick을 주면 누를 수 있는 카드가 된다.
 */
@Composable
fun WalkLogCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = WalkLogTheme.colors.surface,
    shape: Shape = WalkLogTheme.shapes.card,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(color)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        content = content,
    )
}

/**
 * 홈 위젯처럼 모서리 24, 안쪽 여백 16인 큰 카드.
 */
@Composable
fun WalkLogWidget(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = WalkLogTheme.colors.surface,
    content: @Composable BoxScope.() -> Unit,
) {
    WalkLogCard(
        modifier = modifier,
        onClick = onClick,
        color = color,
        shape = WalkLogTheme.shapes.widget,
    ) {
        Box(modifier = Modifier.padding(WalkLogTheme.spacing.s16), content = content)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFBF8F2)
@Composable
private fun WalkLogCardPreview() {
    WalkLogTheme {
        Column(
            modifier = Modifier.padding(WalkLogTheme.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(WalkLogTheme.spacing.s12),
        ) {
            WalkLogWidget(modifier = Modifier.fillMaxWidth(), color = WalkLogTheme.colors.tomato, onClick = {}) {
                Text("걷는 중 · 망원 한강공원", style = WalkLogTheme.typography.title)
            }
            WalkLogCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "카드 안 내용",
                    style = WalkLogTheme.typography.body,
                    modifier = Modifier.padding(WalkLogTheme.spacing.s16),
                )
            }
        }
    }
}
