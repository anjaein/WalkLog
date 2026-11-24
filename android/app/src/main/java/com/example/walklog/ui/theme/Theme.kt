package com.example.walklog.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val DefaultShapes = WalkLogShapes()
private val DefaultSpacing = WalkLogSpacing()
private val DefaultSizes = WalkLogSizes()
private val DefaultElevation = WalkLogElevation()
private val DefaultMotion = WalkLogMotion()

private val LocalWalkLogColors = staticCompositionLocalOf { LightWalkLogColors }
private val LocalWalkLogTypography = staticCompositionLocalOf { DefaultWalkLogTypography }
private val LocalWalkLogShapes = staticCompositionLocalOf { DefaultShapes }
private val LocalWalkLogSpacing = staticCompositionLocalOf { DefaultSpacing }
private val LocalWalkLogSizes = staticCompositionLocalOf { DefaultSizes }
private val LocalWalkLogElevation = staticCompositionLocalOf { DefaultElevation }
private val LocalWalkLogMotion = staticCompositionLocalOf { DefaultMotion }

private val MaterialTypography = DefaultWalkLogTypography.toMaterialTypography()
private val MaterialShapes = DefaultShapes.toMaterialShapes()

// Material 컴포넌트도 앱 색을 쓰도록 맞춘다.
private val WalkLogMaterialColors = lightColorScheme(
    primary = Ink,
    onPrimary = Cream,
    primaryContainer = Hairline,
    onPrimaryContainer = Ink,
    secondary = Tomato,
    onSecondary = Ink,
    tertiary = Cobalt,
    onTertiary = OnDark,
    background = Cream,
    onBackground = Ink,
    surface = SurfaceWhite,
    onSurface = Ink,
    surfaceVariant = Cream,
    onSurfaceVariant = InkSecondary,
    outline = Hairline,
    outlineVariant = Hairline,
    error = Danger,
    onError = OnDark,
    scrim = Ink,
)

/**
 * 화면에서 토큰을 꺼내 쓰는 곳. 예: `WalkLogTheme.colors.tomato`, `WalkLogTheme.spacing.gutter`
 */
object WalkLogTheme {
    val colors: WalkLogColors
        @Composable @ReadOnlyComposable get() = LocalWalkLogColors.current
    val typography: WalkLogTypography
        @Composable @ReadOnlyComposable get() = LocalWalkLogTypography.current
    val shapes: WalkLogShapes
        @Composable @ReadOnlyComposable get() = LocalWalkLogShapes.current
    val spacing: WalkLogSpacing
        @Composable @ReadOnlyComposable get() = LocalWalkLogSpacing.current
    val sizes: WalkLogSizes
        @Composable @ReadOnlyComposable get() = LocalWalkLogSizes.current
    val elevation: WalkLogElevation
        @Composable @ReadOnlyComposable get() = LocalWalkLogElevation.current
    val motion: WalkLogMotion
        @Composable @ReadOnlyComposable get() = LocalWalkLogMotion.current
}

// 디자인에 다크 모드가 아직 없어서 라이트 색 한 벌만 쓴다. 기기 배경화면 색을 따라가는 동적 색상도 쓰지 않는다.
// 지도 탭처럼 어두운 화면은 colors.mapBackground 등 지도 전용 색으로 그린다.
@Composable
fun WalkLogTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalWalkLogColors provides LightWalkLogColors,
        LocalWalkLogTypography provides DefaultWalkLogTypography,
        LocalWalkLogShapes provides DefaultShapes,
        LocalWalkLogSpacing provides DefaultSpacing,
        LocalWalkLogSizes provides DefaultSizes,
        LocalWalkLogElevation provides DefaultElevation,
        LocalWalkLogMotion provides DefaultMotion,
    ) {
        MaterialTheme(
            colorScheme = WalkLogMaterialColors,
            typography = MaterialTypography,
            shapes = MaterialShapes,
            content = content,
        )
    }
}
