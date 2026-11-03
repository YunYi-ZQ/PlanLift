package app.planlift.fit.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.planlift.fit.data.settings.ThemeMode
import app.planlift.fit.data.settings.ThemePrefs
import app.planlift.fit.ui.nav.AppNav
import app.planlift.fit.ui.theme.PlanLiftTheme

@Composable
fun PlanLiftApp() {
    val mode by ThemePrefs.mode.collectAsState()
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    PlanLiftTheme(darkTheme = dark) {
        AppNav()
    }
}
