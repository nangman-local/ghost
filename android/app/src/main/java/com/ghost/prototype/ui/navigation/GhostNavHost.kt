package com.ghost.prototype.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ghost.prototype.ui.more.DeveloperToolsRoute
import com.ghost.prototype.ui.onboarding.OnboardingPrefs
import com.ghost.prototype.ui.onboarding.OnboardingRoute

/** 문자열 라우트. 타입 세이프 라우트는 serialization 플러그인이 필요해 쓰지 않는다. */
object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
}

@Composable
fun GhostNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val context = LocalContext.current
    // 온보딩은 첫 실행 때 한 번만 보여준다.
    val start = remember { if (OnboardingPrefs(context).done) Routes.HOME else Routes.ONBOARDING }
    NavHost(navController = navController, startDestination = start, modifier = modifier) {
        composable(Routes.ONBOARDING) {
            OnboardingRoute(onFinished = {
                navController.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            })
        }
        // 홈 화면이 들어오기 전까지 기존 화면(개발자 도구)을 홈에 둔다(#48).
        composable(Routes.HOME) { DeveloperToolsRoute() }
    }
}
