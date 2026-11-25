package com.example.walklog.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.walklog.R
import com.example.walklog.ui.components.NavTab
import com.example.walklog.ui.navigation.WalkLogRoutes.ARG_COURSE_ID
import com.example.walklog.ui.navigation.WalkLogRoutes.ARG_LOG_ID
import com.example.walklog.ui.navigation.WalkLogRoutes.ARG_PHOTO
import com.example.walklog.ui.navigation.WalkLogRoutes.ARG_POST_ID
import com.example.walklog.ui.navigation.WalkLogRoutes.ARG_STEP
import com.example.walklog.ui.navigation.WalkLogRoutes.ARG_USER_ID

// 아직 데이터가 없어서 빈 화면 사이를 오갈 때 쓰는 예시 id (프로토타입 가짜 데이터와 같다)
private const val SAMPLE_LOG_ID = "l-1118"
private const val SAMPLE_POST_ID = "f-03"
private const val SAMPLE_COURSE_ID = "f-01"
private const val SAMPLE_USER_ID = "mangwon_cat"

private const val FOLDER_AUTH = "feature/auth"
private const val FOLDER_MYPAGE = "feature/mypage"
private const val FOLDER_RECORD = "feature/record"
private const val FOLDER_PHOTOMAP = "feature/photomap"
private const val FOLDER_DISCOVER = "feature/discover"

/**
 * 화면 지도. 각자 화면을 만들면 해당 composable { } 안의 PlaceholderScreen을 자기 화면으로 바꾼다.
 * 화면 사이 이동은 여기 넘겨주는 람다(navController.navigate)로만 하고, 화면 안에서 navController를 직접 받지 않는다.
 */
@Composable
fun WalkLogNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = WalkLogRoutes.HOME,
) {
    val back: () -> Unit = { navController.popBackStack() }
    val go: (String) -> Unit = { route -> navController.navigate(route) }

    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        // ── 온보딩 (feature/auth) ──────────────────────────
        composable(
            route = WalkLogRoutes.ONBOARDING,
            arguments = listOf(navArgument(ARG_STEP) { type = NavType.IntType }),
        ) { entry ->
            val step = (entry.arguments?.getInt(ARG_STEP) ?: WalkLogRoutes.ONBOARDING_FIRST_STEP)
                .coerceIn(WalkLogRoutes.ONBOARDING_FIRST_STEP, WalkLogRoutes.ONBOARDING_LAST_STEP)
            val links = when (step) {
                WalkLogRoutes.ONBOARDING_FIRST_STEP -> listOf(
                    PlaceholderLink(stringResource(R.string.link_google_login)) { go(WalkLogRoutes.onboarding(3)) },
                    PlaceholderLink(stringResource(R.string.link_email_login)) { go(WalkLogRoutes.onboarding(2)) },
                )
                WalkLogRoutes.ONBOARDING_LAST_STEP -> listOf(
                    PlaceholderLink(stringResource(R.string.link_start)) { navController.finishOnboarding() },
                )
                else -> listOf(
                    PlaceholderLink(stringResource(R.string.link_next_step)) { go(WalkLogRoutes.onboarding(step + 1)) },
                )
            }
            PlaceholderScreen(
                title = stringResource(R.string.screen_onboarding, step),
                featureFolder = FOLDER_AUTH,
                links = links,
                onBack = if (step == WalkLogRoutes.ONBOARDING_FIRST_STEP) null else back,
            )
        }

        // ── 홈 탭 (feature/mypage) ─────────────────────────
        composable(WalkLogRoutes.HOME) {
            PlaceholderScreen(
                title = stringResource(R.string.screen_home),
                featureFolder = FOLDER_MYPAGE,
                hasNavBar = true,
                links = listOf(
                    PlaceholderLink(stringResource(R.string.screen_record_time)) { go(WalkLogRoutes.RECORD_TIME) },
                    PlaceholderLink(stringResource(R.string.screen_my_logs)) { go(WalkLogRoutes.MY_LOGS) },
                    PlaceholderLink(stringResource(R.string.screen_archive)) { go(WalkLogRoutes.ARCHIVE) },
                    PlaceholderLink(stringResource(R.string.screen_log)) { go(WalkLogRoutes.log(SAMPLE_LOG_ID)) },
                    PlaceholderLink(stringResource(R.string.screen_profile_edit)) { go(WalkLogRoutes.PROFILE_EDIT) },
                    PlaceholderLink(stringResource(R.string.screen_settings)) { go(WalkLogRoutes.SETTINGS) },
                ),
            )
        }
        composable(WalkLogRoutes.MY_LOGS) {
            PlaceholderScreen(
                title = stringResource(R.string.screen_my_logs),
                featureFolder = FOLDER_MYPAGE,
                hasNavBar = true,
                onBack = back,
                links = listOf(
                    PlaceholderLink(stringResource(R.string.screen_log)) { go(WalkLogRoutes.log(SAMPLE_LOG_ID)) },
                    PlaceholderLink(stringResource(R.string.screen_record_time)) { go(WalkLogRoutes.RECORD_TIME) },
                ),
            )
        }
        composable(WalkLogRoutes.ARCHIVE) {
            PlaceholderScreen(
                title = stringResource(R.string.screen_archive),
                featureFolder = FOLDER_MYPAGE,
                hasNavBar = true,
                onBack = back,
                links = listOf(
                    PlaceholderLink(stringResource(R.string.screen_post)) { go(WalkLogRoutes.post(SAMPLE_POST_ID)) },
                ),
            )
        }
        composable(WalkLogRoutes.SETTINGS) {
            PlaceholderScreen(
                title = stringResource(R.string.screen_settings),
                featureFolder = FOLDER_MYPAGE,
                onBack = back,
                links = listOf(
                    PlaceholderLink(stringResource(R.string.screen_profile_edit)) { go(WalkLogRoutes.PROFILE_EDIT) },
                    PlaceholderLink(stringResource(R.string.link_logout)) { navController.restartOnboarding() },
                    PlaceholderLink(stringResource(R.string.link_delete_account)) { navController.restartOnboarding() },
                ),
            )
        }
        composable(WalkLogRoutes.PROFILE_EDIT) {
            PlaceholderScreen(
                title = stringResource(R.string.screen_profile_edit),
                featureFolder = FOLDER_MYPAGE,
                onBack = back,
                links = listOf(PlaceholderLink(stringResource(R.string.link_save)) { back() }),
            )
        }

        // ── 로그 상세 · 로그 남기기 (feature/record) ────────────
        composable(
            route = WalkLogRoutes.LOG,
            arguments = listOf(navArgument(ARG_LOG_ID) { type = NavType.StringType }, photoArgument()),
        ) { entry ->
            val logId = entry.arguments?.getString(ARG_LOG_ID)
            PlaceholderScreen(
                title = stringResource(R.string.screen_log),
                featureFolder = FOLDER_RECORD,
                params = listOf(ARG_LOG_ID to logId, ARG_PHOTO to entry.arguments?.getInt(ARG_PHOTO)?.toString()),
                onBack = back,
                // QA: 공유 버튼은 메뉴(햄버거) 안이 아니라 밖에 바로 보이게 둔다. 공유 동작은 화면 담당이 연결
                onShare = {},
                links = listOf(
                    PlaceholderLink(stringResource(R.string.link_view_on_map)) { go(WalkLogRoutes.mapView(logId = logId)) },
                    PlaceholderLink(stringResource(R.string.screen_record_edit)) { go(WalkLogRoutes.recordCourse(editLogId = logId)) },
                ),
            )
        }
        composable(WalkLogRoutes.RECORD_TIME) {
            PlaceholderScreen(
                title = stringResource(R.string.screen_record_time),
                featureFolder = FOLDER_RECORD,
                onBack = back,
                links = listOf(PlaceholderLink(stringResource(R.string.link_next)) { go(WalkLogRoutes.recordCourse()) }),
            )
        }
        composable(
            route = WalkLogRoutes.RECORD_COURSE,
            arguments = listOf(
                navArgument(ARG_LOG_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val editLogId = entry.arguments?.getString(ARG_LOG_ID)
            val isEdit = editLogId != null
            PlaceholderScreen(
                title = stringResource(if (isEdit) R.string.screen_record_edit else R.string.screen_record_course),
                featureFolder = FOLDER_RECORD,
                params = listOf(ARG_LOG_ID to editLogId),
                onBack = back,
                links = if (isEdit) {
                    listOf(PlaceholderLink(stringResource(R.string.link_save)) { back() })
                } else {
                    listOf(PlaceholderLink(stringResource(R.string.link_register)) { go(WalkLogRoutes.RECORD_DONE) })
                },
            )
        }
        composable(WalkLogRoutes.RECORD_DONE) {
            PlaceholderScreen(
                title = stringResource(R.string.screen_record_done),
                featureFolder = FOLDER_RECORD,
                links = listOf(
                    PlaceholderLink(stringResource(R.string.link_view_log)) { navController.finishRecord(WalkLogRoutes.log(SAMPLE_LOG_ID)) },
                    PlaceholderLink(stringResource(R.string.action_close)) { navController.finishRecord(null) },
                ),
            )
        }

        // ── 지도 탭 (feature/photomap) ─────────────────────
        composable(WalkLogRoutes.MAP) {
            PlaceholderScreen(
                title = stringResource(R.string.screen_map),
                featureFolder = FOLDER_PHOTOMAP,
                hasNavBar = true,
                dark = true,
                links = listOf(
                    PlaceholderLink(stringResource(R.string.screen_record_time)) { go(WalkLogRoutes.RECORD_TIME) },
                    PlaceholderLink(stringResource(R.string.link_follow)) { go(WalkLogRoutes.follow(SAMPLE_COURSE_ID)) },
                    PlaceholderLink(stringResource(R.string.screen_map_view)) { go(WalkLogRoutes.mapView()) },
                ),
            )
        }
        composable(
            route = WalkLogRoutes.MAP_VIEW,
            arguments = listOf(
                navArgument(ARG_LOG_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(ARG_POST_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val logId = entry.arguments?.getString(ARG_LOG_ID)
            val postId = entry.arguments?.getString(ARG_POST_ID)
            PlaceholderScreen(
                title = stringResource(R.string.screen_map_view),
                featureFolder = FOLDER_PHOTOMAP,
                params = listOf(ARG_LOG_ID to logId, ARG_POST_ID to postId),
                hasNavBar = true,
                // 다른 화면에서 '지도에서 보기'로 왔을 때만 뒤로 가기가 있다
                onBack = if (logId != null || postId != null) back else null,
                links = listOf(
                    PlaceholderLink(stringResource(R.string.link_follow)) { go(WalkLogRoutes.follow(SAMPLE_COURSE_ID)) },
                    PlaceholderLink(stringResource(R.string.screen_record_time)) { go(WalkLogRoutes.RECORD_TIME) },
                ),
            )
        }
        composable(
            route = WalkLogRoutes.FOLLOW,
            arguments = listOf(navArgument(ARG_COURSE_ID) { type = NavType.StringType }),
        ) { entry ->
            PlaceholderScreen(
                title = stringResource(R.string.screen_follow),
                featureFolder = FOLDER_PHOTOMAP,
                params = listOf(ARG_COURSE_ID to entry.arguments?.getString(ARG_COURSE_ID)),
                hasNavBar = true,
                onBack = back,
                links = listOf(PlaceholderLink(stringResource(R.string.link_stop_walking)) { back() }),
            )
        }

        // ── 탐색 탭 (feature/discover) ─────────────────────
        composable(WalkLogRoutes.EXPLORE) {
            PlaceholderScreen(
                title = stringResource(R.string.screen_explore),
                featureFolder = FOLDER_DISCOVER,
                hasNavBar = true,
                links = listOf(
                    PlaceholderLink(stringResource(R.string.screen_search)) { go(WalkLogRoutes.SEARCH) },
                    PlaceholderLink(stringResource(R.string.screen_post)) { go(WalkLogRoutes.post(SAMPLE_POST_ID)) },
                    PlaceholderLink(stringResource(R.string.screen_user)) { go(WalkLogRoutes.user(SAMPLE_USER_ID)) },
                ),
            )
        }
        composable(WalkLogRoutes.SEARCH) {
            PlaceholderScreen(
                title = stringResource(R.string.screen_search),
                featureFolder = FOLDER_DISCOVER,
                onBack = back,
                links = listOf(PlaceholderLink(stringResource(R.string.screen_post)) { go(WalkLogRoutes.post(SAMPLE_POST_ID)) }),
            )
        }
        composable(
            route = WalkLogRoutes.POST,
            arguments = listOf(navArgument(ARG_POST_ID) { type = NavType.StringType }, photoArgument()),
        ) { entry ->
            val postId = entry.arguments?.getString(ARG_POST_ID)
            PlaceholderScreen(
                title = stringResource(R.string.screen_post),
                featureFolder = FOLDER_DISCOVER,
                params = listOf(ARG_POST_ID to postId, ARG_PHOTO to entry.arguments?.getInt(ARG_PHOTO)?.toString()),
                onBack = back,
                // QA: 남의 로그에도 공유 버튼을 둔다
                onShare = {},
                links = listOf(
                    PlaceholderLink(stringResource(R.string.screen_user)) { go(WalkLogRoutes.user(SAMPLE_USER_ID)) },
                    PlaceholderLink(stringResource(R.string.link_view_on_map)) { go(WalkLogRoutes.mapView(postId = postId)) },
                    PlaceholderLink(stringResource(R.string.link_follow)) { go(WalkLogRoutes.follow(postId ?: SAMPLE_COURSE_ID)) },
                ),
            )
        }
        composable(
            route = WalkLogRoutes.USER,
            arguments = listOf(navArgument(ARG_USER_ID) { type = NavType.StringType }),
        ) { entry ->
            PlaceholderScreen(
                title = stringResource(R.string.screen_user),
                featureFolder = FOLDER_DISCOVER,
                params = listOf(ARG_USER_ID to entry.arguments?.getString(ARG_USER_ID)),
                onBack = back,
                links = listOf(PlaceholderLink(stringResource(R.string.screen_post)) { go(WalkLogRoutes.post(SAMPLE_POST_ID)) }),
            )
        }

        composable(WalkLogRoutes.NOT_FOUND) {
            PlaceholderScreen(
                title = stringResource(R.string.screen_not_found),
                featureFolder = FOLDER_MYPAGE,
                links = listOf(PlaceholderLink(stringResource(R.string.link_go_home)) { navController.navigateToTab(NavTab.HOME) }),
            )
        }
    }
}

private fun photoArgument() = navArgument(ARG_PHOTO) {
    type = NavType.IntType
    defaultValue = 0
}

/** 하단 냅바 탭 이동: 탭마다 보던 화면을 기억하고, 같은 탭을 여러 번 쌓지 않는다. */
fun NavHostController.navigateToTab(tab: NavTab) {
    navigate(tab.route()) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** 로그아웃 · 계정 삭제: 지나온 화면을 모두 지우고 온보딩 첫 단계로. */
private fun NavHostController.restartOnboarding() {
    navigate(WalkLogRoutes.onboarding(WalkLogRoutes.ONBOARDING_FIRST_STEP)) {
        popUpTo(graph.id) { inclusive = true }
    }
}

/** 온보딩 끝: 온보딩 화면들을 지우고 홈으로. 뒤로 가기로 온보딩에 돌아가지 않는다. */
private fun NavHostController.finishOnboarding() {
    navigate(WalkLogRoutes.HOME) {
        popUpTo(graph.id) { inclusive = true }
    }
}

/**
 * 로그 등록 끝: 로그 남기기 화면들(시간 · 코스 · 완료)을 지운다.
 * logRoute가 있으면 그 로그로 가고, 뒤로 가면 로그 남기기를 시작하기 전 화면으로 돌아간다.
 */
private fun NavHostController.finishRecord(logRoute: String?) {
    if (logRoute == null) {
        popBackStack(WalkLogRoutes.RECORD_TIME, inclusive = true)
        return
    }
    navigate(logRoute) {
        popUpTo(WalkLogRoutes.RECORD_TIME) { inclusive = true }
    }
}
