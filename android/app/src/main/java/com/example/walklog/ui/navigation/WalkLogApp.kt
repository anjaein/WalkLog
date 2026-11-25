package com.example.walklog.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.walklog.ui.components.WalkLogNavBar
import com.example.walklog.ui.theme.WalkLogTheme

/**
 * 앱 전체 틀: 화면(NavHost) 위에 하단 냅바를 띄운다.
 * 냅바는 화면 스크롤 밖에 있어서 화면을 넘겨도 제자리에 있다.
 */
@Composable
fun WalkLogApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val tab = navTabFor(route)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WalkLogTheme.colors.background),
    ) {
        WalkLogNavHost(navController = navController, modifier = Modifier.fillMaxSize())
        if (tab != null) {
            WalkLogNavBar(
                selected = tab,
                onSelect = { navController.navigateToTab(it) },
                light = usesLightNavBar(route),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = WalkLogTheme.sizes.navBarBottom),
            )
        }
    }
}
