package app.planlift.fit.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun Color.tone(white: Float): Color = lerp(this, Color.White, white)
private fun Color.shade(black: Float): Color = lerp(this, Color.Black, black)

/**
 * 从种子色生成一套明/暗配色。
 * 不是完整的 Material Color Utilities，但足以保证主色、容器色、描边色协调。
 */
private fun schemeFrom(seed: Color, dark: Boolean) = if (dark) {
    val primary = seed.tone(0.55f)
    darkColorScheme(
        primary = primary,
        onPrimary = Color(0xFF06121F),
        primaryContainer = seed.shade(0.55f),
        onPrimaryContainer = seed.tone(0.75f),
        secondary = seed.tone(0.35f),
        onSecondary = Color(0xFF0B1220),
        secondaryContainer = seed.shade(0.70f),
        onSecondaryContainer = seed.tone(0.70f),
        tertiary = seed.tone(0.45f),
        background = Color(0xFF0B1220),
        onBackground = Color(0xFFE6ECF5),
        surface = Color(0xFF0B1220),
        onSurface = Color(0xFFE6ECF5),
        surfaceVariant = Color(0xFF1B2436),
        onSurfaceVariant = Color(0xFFA9B4C7),
        outline = Color(0xFF44506A)
    )
} else {
    val primary = seed.shade(0.18f)
    lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = seed.tone(0.78f),
        onPrimaryContainer = seed.shade(0.55f),
        secondary = seed.shade(0.35f),
        onSecondary = Color.White,
        secondaryContainer = seed.tone(0.86f),
        onSecondaryContainer = seed.shade(0.60f),
        tertiary = seed.shade(0.45f),
        background = Color(0xFFF7F9FC),
        onBackground = Color(0xFF111827),
        surface = Color(0xFFF7F9FC),
        onSurface = Color(0xFF111827),
        surfaceVariant = Color(0xFFE7ECF4),
        onSurfaceVariant = Color(0xFF48536B),
        outline = Color(0xFFB3BDCF)
    )
}

@Composable
fun PlanLiftTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    seed: Color = Color(0xFF38BDF8),
    dynamic: Boolean = false,
    pureBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var colors = if (dynamic && Build.VERSION.SDK_INT >= 31) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        schemeFrom(seed, darkTheme)
    }

    if (darkTheme && pureBlack) {
        colors = colors.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF141A26)
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = colors, content = content)
}
