package com.example.walklog.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// 이름의 숫자가 dp 값이다 (프로토타입 --space-2 ~ --space-32).
@Immutable
data class WalkLogSpacing(
    val s2: Dp = 2.dp,
    val s4: Dp = 4.dp,
    val s6: Dp = 6.dp,
    val s8: Dp = 8.dp,
    val s12: Dp = 12.dp,
    val s16: Dp = 16.dp,
    val s20: Dp = 20.dp,
    val s24: Dp = 24.dp,
    val s32: Dp = 32.dp,
    // 화면 좌우 기본 여백
    val gutter: Dp = 20.dp,
)

// 공통 컴포넌트의 정해진 크기. 화면 코드에서 dp를 직접 쓰지 않도록 여기에 모은다.
@Immutable
data class WalkLogSizes(
    val pillLarge: Dp = 56.dp,
    val pillMedium: Dp = 48.dp,
    val pillSmall: Dp = 40.dp,
    val iconButton: Dp = 48.dp,
    val icon: Dp = 24.dp,
    val iconSmall: Dp = 20.dp,
    val chip: Dp = 36.dp,
    val chipLarge: Dp = 40.dp,
    val chipPadding: Dp = 14.dp,
    val navBarPadding: Dp = 6.dp,
    val navBarItemGap: Dp = 4.dp,
    val navBarItemWidth: Dp = 64.dp,
    val navBarItemHeight: Dp = 48.dp,
    // 냅바 아래쪽 띄움. 화면 아래 여백 = navBarItemHeight + navBarPadding * 2 + navBarBottom
    val navBarBottom: Dp = 20.dp,
    val hairline: Dp = 1.dp,
    val sheetHandleWidth: Dp = 40.dp,
    val sheetHandleHeight: Dp = 4.dp,
)

// 그림자 높이. 프로토타입 box-shadow 값을 Compose 그림자에 가깝게 옮겼다.
@Immutable
data class WalkLogElevation(
    val sheet: Dp = 6.dp,
    val float: Dp = 8.dp,
    val card: Dp = 12.dp,
    val marker: Dp = 4.dp,
)
