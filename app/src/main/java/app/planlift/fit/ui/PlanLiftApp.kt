package app.planlift.fit.ui

import androidx.compose.runtime.Composable
import app.planlift.fit.ui.nav.AppNav
import app.planlift.fit.ui.theme.PlanLiftTheme

@Composable
fun PlanLiftApp() {
    PlanLiftTheme {
        AppNav()
    }
}
