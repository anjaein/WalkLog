package com.example.walklog.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * 프로토타입(js/ui.js)의 24px 선 아이콘을 그대로 옮긴 것. 선 굵기 2, 끝 둥글게.
 * 색은 Icon(tint = ...)으로 정하므로 여기 선 색은 의미 없다.
 * 아이콘 라이브러리(material-icons-extended)를 따로 넣지 않으려고 직접 만들었다.
 */
object WalkLogIcons {
    val Home: ImageVector by lazy {
        lineIcon("Home", "M4 10.5 12 4l8 6.5V20a1 1 0 0 1-1 1h-4.5v-6h-5v6H5a1 1 0 0 1-1-1z")
    }
    val Map: ImageVector by lazy {
        lineIcon("Map", "M9 4 3.5 6v14L9 18l6 2 5.5-2V4L15 6z", "M9 4v14M15 6v14")
    }
    val Compass: ImageVector by lazy {
        lineIcon("Compass", circle(12f, 12f, 9f), "m15.5 8.5-2 5-5 2 2-5z")
    }
    val Back: ImageVector by lazy {
        lineIcon("Back", "M19 12H5M11 18l-6-6 6-6")
    }
    val Close: ImageVector by lazy {
        lineIcon("Close", "M6 6l12 12M18 6 6 18")
    }
    val Search: ImageVector by lazy {
        lineIcon("Search", circle(11f, 11f, 6.5f), "m20 20-4.2-4.2")
    }
    val Plus: ImageVector by lazy {
        lineIcon("Plus", "M12 5v14M5 12h14")
    }
    val ChevronRight: ImageVector by lazy {
        lineIcon("ChevronRight", "m9 6 6 6-6 6")
    }
    val Share: ImageVector by lazy {
        lineIcon("Share", "M12 15V4M8 8l4-4 4 4M5 13v6h14v-6")
    }
    val Menu: ImageVector by lazy {
        lineIcon("Menu", "M4 7h16M4 12h16M4 17h16")
    }
    val More: ImageVector by lazy {
        lineIcon("More", circle(5f, 12f, 1.2f), circle(12f, 12f, 1.2f), circle(19f, 12f, 1.2f))
    }
    val Gear: ImageVector by lazy {
        lineIcon(
            "Gear",
            circle(12f, 12f, 3f),
            "M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z",
        )
    }
}

private const val VIEWPORT = 24f
private const val STROKE_WIDTH = 2f

// SVG <circle>을 path로 바꾼 것: 왼쪽 끝에서 시작해 반원 두 개로 한 바퀴
private fun circle(cx: Float, cy: Float, r: Float): String =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

private fun lineIcon(name: String, vararg paths: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = VIEWPORT.dp,
        defaultHeight = VIEWPORT.dp,
        viewportWidth = VIEWPORT,
        viewportHeight = VIEWPORT,
    ).apply {
        paths.forEach { d ->
            addPath(
                pathData = addPathNodes(d),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = STROKE_WIDTH,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()
