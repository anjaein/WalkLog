package com.example.walklog.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.walklog.R

val Pretendard = FontFamily(
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_bold, FontWeight.Bold),
    Font(R.font.pretendard_extrabold, FontWeight.ExtraBold),
)

// 숫자 · 영문용 글꼴(Bricolage · Doto · Red Hat)에는 한글이 없다. 한글을 쓰면 Pretendard로 보이지 않으니 숫자 · 영문에만 쓴다.
// Doto는 영문 · 숫자 · 기본 문장부호만 남겨 용량을 줄였다.
val BricolageGrotesque = FontFamily(
    Font(R.font.bricolage_grotesque_bold, FontWeight.Bold),
    Font(R.font.bricolage_grotesque_extrabold, FontWeight.ExtraBold),
)

val Doto = FontFamily(
    Font(R.font.doto_bold, FontWeight.Bold),
    Font(R.font.doto_black, FontWeight.Black),
)

val RedHatDisplay = FontFamily(
    Font(R.font.red_hat_display_bold, FontWeight.Bold),
)

@Immutable
data class WalkLogTypography(
    val display: TextStyle,
    val title: TextStyle,
    val subhead: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val labelBold: TextStyle,
    val caption: TextStyle,
    val button: TextStyle,
    val buttonSmall: TextStyle,
    val numberBig: TextStyle,
    val numberStat: TextStyle,
    val numberMeta: TextStyle,
    val dotBig: TextStyle,
    val dotStat: TextStyle,
    val dotStamp: TextStyle,
    // 화면에 쓸 때 대문자로 바꿔서 넣는다 (예: "AUTO TRACKING")
    val eyebrow: TextStyle,
)

val DefaultWalkLogTypography = WalkLogTypography(
    display = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.02).em,
    ),
    title = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.01).em,
    ),
    subhead = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.01).em,
    ),
    body = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    label = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelBold = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    caption = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    button = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.01).em,
    ),
    buttonSmall = TextStyle(
        fontFamily = Pretendard,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.01).em,
    ),
    numberBig = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.03).em,
    ),
    numberStat = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.02).em,
    ),
    numberMeta = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 14.sp,
    ),
    dotBig = TextStyle(
        fontFamily = Doto,
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        lineHeight = 36.sp,
    ),
    dotStat = TextStyle(
        fontFamily = Doto,
        fontWeight = FontWeight.Black,
        fontSize = 18.sp,
        lineHeight = 22.sp,
    ),
    dotStamp = TextStyle(
        fontFamily = Doto,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.12.em,
    ),
    eyebrow = TextStyle(
        fontFamily = RedHatDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.18.em,
    ),
)

// Material 컴포넌트(TextField, Snackbar 등)가 쓰는 글자 스타일도 같은 값으로 맞춘다.
internal fun WalkLogTypography.toMaterialTypography() = Typography(
    displaySmall = display,
    headlineLarge = display,
    titleLarge = title,
    titleMedium = subhead,
    bodyLarge = body,
    bodyMedium = body,
    bodySmall = caption,
    labelLarge = labelBold,
    labelMedium = label,
    labelSmall = caption,
)
