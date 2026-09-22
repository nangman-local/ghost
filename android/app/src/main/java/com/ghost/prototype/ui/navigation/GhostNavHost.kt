package com.ghost.prototype.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ghost.prototype.ui.more.DeveloperToolsRoute

/** 문자열 라우트. 타입 세이프 라우트는 serialization 플러그인이 필요해 쓰지 않는다. */
object Routes {
    const val HOME = "home"
}

@Composable
fun GhostNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME, modifier = modifier) {
        // 온보딩·홈 화면이 들어오기 전까지 기존 화면(개발자 도구)을 홈에 둔다.
        composable(Routes.HOME) { DeveloperToolsRoute() }
    }
}
