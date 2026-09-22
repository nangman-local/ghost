package com.ghost.prototype.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ghost.prototype.ui.calendar.CalendarScreen
import com.ghost.prototype.ui.component.GhostNavBar
import com.ghost.prototype.ui.component.MainTab
import com.ghost.prototype.ui.home.HomeRoute
import com.ghost.prototype.ui.more.DeveloperToolsRoute
import com.ghost.prototype.ui.onboarding.OnboardingPrefs
import com.ghost.prototype.ui.onboarding.OnboardingRoute

/** 문자열 라우트. 타입 세이프 라우트는 serialization 플러그인이 필요해 쓰지 않는다. */
object Routes {
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
}

private val MainTab.route get() = "tab/${name.lowercase()}"

@Composable
fun GhostNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val context = LocalContext.current
    // 온보딩은 첫 실행 때 한 번만 보여준다.
    val start = remember { if (OnboardingPrefs(context).done) Routes.MAIN else Routes.ONBOARDING }
    NavHost(navController = navController, startDestination = start, modifier = modifier) {
        composable(Routes.ONBOARDING) {
            OnboardingRoute(onFinished = {
                navController.navigate(Routes.MAIN) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            })
        }
        composable(Routes.MAIN) { MainTabs() }
    }
}

/** 하단 탭(캘린더 · 홈 · 더보기)과 탭별 화면. */
@Composable
private fun MainTabs(navController: NavHostController = rememberNavController()) {
    val entry by navController.currentBackStackEntryAsState()
    val selected = MainTab.entries.firstOrNull { it.route == entry?.destination?.route } ?: MainTab.HOME
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        NavHost(
            navController = navController,
            startDestination = MainTab.HOME.route,
            modifier = Modifier.weight(1f).consumeWindowInsets(WindowInsets.navigationBars),
        ) {
            composable(MainTab.CALENDAR.route) { CalendarScreen() }
            composable(MainTab.HOME.route) { HomeRoute() }
            // 더보기: 디자인 전까지 기존 프로토타입 화면을 개발자 도구로 둔다.
            composable(MainTab.MORE.route) { DeveloperToolsRoute() }
        }
        Box(Modifier.fillMaxWidth().navigationBarsPadding(), contentAlignment = Alignment.Center) {
            GhostNavBar(
                selected = selected,
                onSelect = { tab ->
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.widthIn(max = 480.dp),
            )
        }
    }
}
