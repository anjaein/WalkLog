package com.example.walklog.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// 값은 웹 프로토타입 css/tokens.css(여름 필름 모드)와 같다.

// 기본 바탕 · 글자
val Cream = Color(0xFFFBF8F2)
val SurfaceWhite = Color(0xFFFFFFFF)
val Ink = Color(0xFF1C1B1A)
val InkSecondary = Color(0xFF57514A)
val InkTertiary = Color(0xFF6E675D)
val Hairline = Color(0xFFE9E3D8)
val RouteDay = Color(0xFFCFC7BA)
val OnDark = Color(0xFFFFFFFF)
val Dim = Color(0x731C1B1A)

// 포인트 4색: 토마토 = 내 로그, 코발트 = 남의 로그
val Tomato = Color(0xFFFF5A36)
val Cobalt = Color(0xFF2F5BFF)
val Lemon = Color(0xFFFFD43B)
val Mint = Color(0xFF2BD9A0)
val Danger = Color(0xFFC9381A)

// 남의 프로필 로그 묶음(주머니) 옅은 색
val PocketApricot = Color(0xFFFFC9A8)
val PocketSky = Color(0xFFBFD3FF)
val PocketLemon = Color(0xFFFFE89A)
val PocketPink = Color(0xFFFFD0D6)
val PocketMint = Color(0xFFBDF2DD)
val PocketLime = Color(0xFFD9F0A8)
val PocketWater = Color(0xFFC6E8F0)

// 지도 탭 어두운 판
val MapBackground = Color(0xFF151515)
val MapInk = Color(0xFFFBF8F2)
val MapInkSecondary = Color(0xFF9A9A9A)
val MapSurface = Color(0xFF1C1C1C)
val MapHairline = Color(0xFF5A5A5A)

// 지도 그림 (로그 안 작은 지도, 지도 화면)
val MapLand = Color(0xFFECE6DA)
val MapRoad = Color(0xFFFFFFFF)
val MapPark = Color(0xFFD9E5C8)
val MapWater = Color(0xFFD3E1EA)

@Immutable
data class WalkLogColors(
    val background: Color,
    val surface: Color,
    val ink: Color,
    val inkSecondary: Color,
    val inkTertiary: Color,
    val hairline: Color,
    val routeDay: Color,
    val onDark: Color,
    val dim: Color,
    val myLog: Color,
    val othersLog: Color,
    val tomato: Color,
    val cobalt: Color,
    val lemon: Color,
    val mint: Color,
    val danger: Color,
    val pocketApricot: Color,
    val pocketSky: Color,
    val pocketLemon: Color,
    val pocketPink: Color,
    val pocketMint: Color,
    val pocketLime: Color,
    val pocketWater: Color,
    val mapBackground: Color,
    val mapInk: Color,
    val mapInkSecondary: Color,
    val mapSurface: Color,
    val mapHairline: Color,
    val mapLand: Color,
    val mapRoad: Color,
    val mapPark: Color,
    val mapWater: Color,
)

val LightWalkLogColors = WalkLogColors(
    background = Cream,
    surface = SurfaceWhite,
    ink = Ink,
    inkSecondary = InkSecondary,
    inkTertiary = InkTertiary,
    hairline = Hairline,
    routeDay = RouteDay,
    onDark = OnDark,
    dim = Dim,
    myLog = Tomato,
    othersLog = Cobalt,
    tomato = Tomato,
    cobalt = Cobalt,
    lemon = Lemon,
    mint = Mint,
    danger = Danger,
    pocketApricot = PocketApricot,
    pocketSky = PocketSky,
    pocketLemon = PocketLemon,
    pocketPink = PocketPink,
    pocketMint = PocketMint,
    pocketLime = PocketLime,
    pocketWater = PocketWater,
    mapBackground = MapBackground,
    mapInk = MapInk,
    mapInkSecondary = MapInkSecondary,
    mapSurface = MapSurface,
    mapHairline = MapHairline,
    mapLand = MapLand,
    mapRoad = MapRoad,
    mapPark = MapPark,
    mapWater = MapWater,
)
