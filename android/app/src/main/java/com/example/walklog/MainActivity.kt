package com.example.walklog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.walklog.ui.navigation.WalkLogApp
import com.example.walklog.ui.theme.Cream
import com.example.walklog.ui.theme.Ink
import com.example.walklog.ui.theme.WalkLogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // 앱 바탕이 밝은 크림색이라, 기기가 다크 모드여도 상태바 · 내비게이션 바 아이콘을 어두운 색으로 둔다
        val systemBarStyle = SystemBarStyle.light(scrim = Cream.toArgb(), darkScrim = Ink.toArgb())
        enableEdgeToEdge(statusBarStyle = systemBarStyle, navigationBarStyle = systemBarStyle)
        setContent {
            WalkLogTheme {
                WalkLogApp()
            }
        }
    }
}
