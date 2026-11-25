package com.example.walklog.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.walklog.R
import com.example.walklog.ui.components.CircleIconButton
import com.example.walklog.ui.components.CircleIconButtonStyle
import com.example.walklog.ui.components.PillButton
import com.example.walklog.ui.components.PillSize
import com.example.walklog.ui.components.PillStyle
import com.example.walklog.ui.components.WalkLogIcons
import com.example.walklog.ui.theme.WalkLogTheme

@Immutable
data class PlaceholderLink(val label: String, val onClick: () -> Unit)

/**
 * 아직 만들지 않은 화면 자리. 화면 이름, 만들 폴더, 받은 값, 이동할 수 있는 화면 버튼만 보여 준다.
 * 각자 화면을 만들면 WalkLogNavHost에서 이 함수 호출을 그 화면으로 바꾸면 된다.
 */
@Composable
fun PlaceholderScreen(
    title: String,
    featureFolder: String,
    links: List<PlaceholderLink>,
    modifier: Modifier = Modifier,
    params: List<Pair<String, String?>> = emptyList(),
    onBack: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    hasNavBar: Boolean = false,
    dark: Boolean = false,
) {
    val colors = WalkLogTheme.colors
    val spacing = WalkLogTheme.spacing
    val sizes = WalkLogTheme.sizes
    val typography = WalkLogTheme.typography
    val background = if (dark) colors.mapBackground else colors.background
    val ink = if (dark) colors.mapInk else colors.ink
    val subInk = if (dark) colors.mapInkSecondary else colors.inkTertiary

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = spacing.gutter),
    ) {
        Spacer(Modifier.height(spacing.s12))
        if (onBack != null || onShare != null) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    CircleIconButton(
                        icon = WalkLogIcons.Back,
                        contentDescription = stringResource(R.string.action_back),
                        onClick = onBack,
                        style = CircleIconButtonStyle.LINE,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (onShare != null) {
                    CircleIconButton(
                        icon = WalkLogIcons.Share,
                        contentDescription = stringResource(R.string.action_share),
                        onClick = onShare,
                        style = CircleIconButtonStyle.LINE,
                    )
                }
            }
            Spacer(Modifier.height(spacing.s16))
        }
        Text(text = title, style = typography.display, color = ink)
        Spacer(Modifier.height(spacing.s8))
        Text(
            text = stringResource(R.string.placeholder_owner, featureFolder),
            style = typography.label,
            color = subInk,
        )
        params.forEach { (name, value) ->
            Text(
                text = stringResource(R.string.placeholder_param, name, value ?: stringResource(R.string.placeholder_param_empty)),
                style = typography.label,
                color = subInk,
            )
        }
        Spacer(Modifier.height(spacing.s32))
        if (links.isNotEmpty()) {
            Text(text = stringResource(R.string.placeholder_links), style = typography.labelBold, color = subInk)
            Spacer(Modifier.height(spacing.s8))
            Column(verticalArrangement = Arrangement.spacedBy(spacing.s8)) {
                links.forEach { link ->
                    PillButton(
                        text = link.label,
                        onClick = link.onClick,
                        modifier = Modifier.fillMaxWidth(),
                        size = PillSize.MEDIUM,
                        style = PillStyle.LINE,
                        trailingIcon = WalkLogIcons.ChevronRight,
                    )
                }
            }
        }
        // 떠 있는 냅바에 마지막 버튼이 가리지 않게 아래를 비운다
        Spacer(
            Modifier
                .navigationBarsPadding()
                .height(
                    if (hasNavBar) {
                        sizes.navBarItemHeight + sizes.navBarPadding * 2 + sizes.navBarBottom + spacing.s24
                    } else {
                        spacing.s24
                    },
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderScreenPreview() {
    WalkLogTheme {
        PlaceholderScreen(
            title = "로그 상세",
            featureFolder = "feature/record",
            params = listOf("logId" to "l-1118"),
            links = listOf(PlaceholderLink("지도에서 보기") {}, PlaceholderLink("로그 수정") {}),
            onBack = {},
            onShare = {},
        )
    }
}
