package app.planlift.fit.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.planlift.fit.R
import app.planlift.fit.data.content.isChinese
import app.planlift.fit.ui.history.HistoryScreen
import app.planlift.fit.ui.plans.PlanDetailScreen
import app.planlift.fit.ui.plans.PlanListScreen
import app.planlift.fit.ui.settings.SettingsScreen
import app.planlift.fit.ui.today.TodayScreen
import app.planlift.fit.ui.workout.WorkoutScreen

object Routes {
    const val TODAY = "today"
    const val PLANS = "plans"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val PLAN_DETAIL = "plan/{planId}"
    const val WORKOUT = "workout/{dayIndex}"

    fun planDetail(id: String) = "plan/$id"
    fun workout(dayIndex: Int) = "workout/$dayIndex"
}

private data class Tab(val route: String, val labelRes: Int, val icon: String)

private val TABS = listOf(
    Tab(Routes.TODAY, R.string.nav_today, "◎"),
    Tab(Routes.PLANS, R.string.nav_plans, "≡"),
    Tab(Routes.HISTORY, R.string.nav_history, "▤"),
    Tab(Routes.SETTINGS, R.string.nav_settings, "☰")
)

@Composable
fun AppNav() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination
    val isZh = isChinese(LocalConfiguration.current.locales[0]?.language)

    val showBar = current?.route in TABS.map { it.route }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(visible = showBar) {
                NavigationBar {
                    TABS.forEach { tab ->
                        val selected = current?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Text(tab.icon) },
                            label = { Text(stringResource(tab.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.TODAY,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.TODAY) {
                TodayScreen(
                    isZh = isZh,
                    onBrowsePlans = { navController.navigate(Routes.PLANS) },
                    onStartWorkout = { dayIndex -> navController.navigate(Routes.workout(dayIndex)) }
                )
            }
            composable(Routes.PLANS) {
                PlanListScreen(
                    isZh = isZh,
                    onOpenPlan = { id -> navController.navigate(Routes.planDetail(id)) }
                )
            }
            composable(Routes.PLAN_DETAIL) { entry ->
                val planId = entry.arguments?.getString("planId").orEmpty()
                PlanDetailScreen(
                    planId = planId,
                    isZh = isZh,
                    onBack = { navController.popBackStack() },
                    onEnrolled = {
                        navController.navigate(Routes.TODAY) {
                            popUpTo(Routes.TODAY) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.WORKOUT) { entry ->
                val dayIndex = entry.arguments?.getString("dayIndex")?.toIntOrNull() ?: 0
                WorkoutScreen(
                    dayIndex = dayIndex,
                    isZh = isZh,
                    onDone = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(Routes.HISTORY) { HistoryScreen(isZh = isZh) }
            composable(Routes.SETTINGS) { SettingsScreen(isZh = isZh) }
        }
    }
}
