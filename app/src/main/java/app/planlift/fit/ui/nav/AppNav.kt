package app.planlift.fit.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.planlift.fit.R
import app.planlift.fit.data.content.isChinese
import app.planlift.fit.data.settings.ThemePrefs
import app.planlift.fit.ui.history.HistoryScreen
import app.planlift.fit.ui.plans.PlanDetailScreen
import app.planlift.fit.ui.plans.PlanEditorScreen
import app.planlift.fit.ui.plans.PlanListScreen
import app.planlift.fit.ui.progress.ProgressScreen
import app.planlift.fit.ui.settings.AboutScreen
import app.planlift.fit.ui.settings.ExportScreen
import app.planlift.fit.ui.settings.FontScreen
import app.planlift.fit.ui.settings.SettingsScreen
import app.planlift.fit.ui.settings.SyncScreen
import app.planlift.fit.ui.settings.ThemeScreen
import app.planlift.fit.ui.today.TodayScreen
import app.planlift.fit.ui.workout.WorkoutScreen

object Routes {
    const val TODAY = "today"
    const val PLANS = "plans"
    const val HISTORY = "history"
    const val PROGRESS = "progress"
    const val SETTINGS = "settings"
    const val SETTINGS_SYNC = "settings/sync"
    const val SETTINGS_EXPORT = "settings/export"
    const val SETTINGS_THEME = "settings/theme"
    const val SETTINGS_FONT = "settings/font"
    const val SETTINGS_ABOUT = "settings/about"
    const val PLAN_DETAIL = "plan/{planId}"
    const val PLAN_EDIT = "plan_edit/{planId}"
    const val WORKOUT = "workout/{dayIndex}"

    fun planDetail(id: String) = "plan/$id"
    fun planEdit(id: String?) = "plan_edit/${id ?: "new"}"
    fun workout(dayIndex: Int) = "workout/$dayIndex"
}

private data class Tab(val route: String, val labelRes: Int, val icon: ImageVector)

private val TABS = listOf(
    Tab(Routes.TODAY, R.string.nav_today, Icons.Rounded.Today),
    Tab(Routes.PLANS, R.string.nav_plans, Icons.Rounded.List),
    Tab(Routes.HISTORY, R.string.nav_history, Icons.Rounded.History),
    Tab(Routes.PROGRESS, R.string.nav_progress, Icons.Rounded.Insights),
    Tab(Routes.SETTINGS, R.string.nav_settings, Icons.Rounded.Settings)
)

@Composable
fun AppNav() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination
    val isZh = isChinese(LocalConfiguration.current.locales[0]?.language)
    val theme by ThemePrefs.state.collectAsState()

    val showBar = current?.route in TABS.map { it.route }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBar,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                AppBottomBar(
                    currentRoute = current?.route,
                    floating = theme.navFloating,
                    glass = theme.glass,
                    onSelect = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.TODAY,
            modifier = Modifier.padding(padding),
            enterTransition = {
                fadeIn(tween(240)) + slideInHorizontally(tween(240)) { it / 8 }
            },
            exitTransition = { fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.98f) },
            popEnterTransition = {
                fadeIn(tween(240)) + slideInHorizontally(tween(240)) { -it / 8 }
            },
            popExitTransition = { fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.98f) }
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
                    onOpenPlan = { id -> navController.navigate(Routes.planDetail(id)) },
                    onNewPlan = { navController.navigate(Routes.planEdit(null)) }
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
            composable(Routes.PLAN_EDIT) { entry ->
                val id = entry.arguments?.getString("planId")
                PlanEditorScreen(
                    planId = id,
                    isZh = isZh,
                    onBack = { navController.popBackStack() },
                    onSaved = { planId ->
                        navController.navigate(Routes.planDetail(planId)) {
                            popUpTo(Routes.PLANS)
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
            composable(Routes.PROGRESS) { ProgressScreen(isZh = isZh) }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    isZh = isZh,
                    onOpenSync = { navController.navigate(Routes.SETTINGS_SYNC) },
                    onOpenExport = { navController.navigate(Routes.SETTINGS_EXPORT) },
                    onOpenTheme = { navController.navigate(Routes.SETTINGS_THEME) },
                    onOpenAbout = { navController.navigate(Routes.SETTINGS_ABOUT) }
                )
            }
            composable(Routes.SETTINGS_SYNC) { SyncScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.SETTINGS_EXPORT) { ExportScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.SETTINGS_THEME) {
                ThemeScreen(
                    onBack = { navController.popBackStack() },
                    onOpenFont = { navController.navigate(Routes.SETTINGS_FONT) }
                )
            }
            composable(Routes.SETTINGS_FONT) {
                FontScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS_ABOUT) { AboutScreen(onBack = { navController.popBackStack() }) }
        }
    }
}

@Composable
private fun AppBottomBar(
    currentRoute: String?,
    floating: Boolean,
    glass: Boolean,
    onSelect: (String) -> Unit
) {
    val barColor = if (glass) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    Box(
        Modifier
            .fillMaxWidth()
            .padding(
                start = if (floating) 18.dp else 0.dp,
                end = if (floating) 18.dp else 0.dp,
                bottom = if (floating) 14.dp else 0.dp
            )
    ) {
        Surface(
            shape = if (floating) RoundedCornerShape(30.dp) else RoundedCornerShape(0.dp),
            color = barColor,
            tonalElevation = if (floating) 6.dp else 2.dp,
            shadowElevation = if (floating) 12.dp else 0.dp,
            border = if (glass) {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            } else null
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                modifier = Modifier.height(if (floating) 76.dp else 80.dp)
            ) {
                TABS.forEach { tab ->
                    val selected = currentRoute == tab.route
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1.12f else 1f,
                        animationSpec = tween(220),
                        label = "tabScale"
                    )
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onSelect(tab.route) },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = stringResource(tab.labelRes),
                                modifier = Modifier.graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                            )
                        },
                        label = { Text(stringResource(tab.labelRes)) },
                        alwaysShowLabel = true
                    )
                }
            }
        }
    }
}
