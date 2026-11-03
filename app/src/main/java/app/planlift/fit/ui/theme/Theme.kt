package app.planlift.fit.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Sky400,
    onPrimary = Navy900,
    primaryContainer = SkyContainer,
    onPrimaryContainer = OnSkyContainer,
    secondary = Navy800,
    surface = SurfaceLight
)

private val DarkColors = darkColorScheme(
    primary = Sky300,
    onPrimary = Navy900,
    primaryContainer = Navy800,
    onPrimaryContainer = SkyContainer,
    secondary = Sky300,
    background = SurfaceDark,
    surface = SurfaceDark
)

@Composable
fun PlanLiftTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = colors, content = content)
}
