package app.planlift.fit.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.core.view.WindowCompat
import app.planlift.fit.data.settings.ColorStyle
import app.planlift.fit.data.settings.FontStyle

private fun Color.tone(white: Float): Color = lerp(this, Color.White, white)
private fun Color.shade(black: Float): Color = lerp(this, Color.Black, black)

/** 按风格调整种子色的「浓度」；返回用于生成配色的实际主色 */
private fun applyStyle(seed: Color, style: ColorStyle): Color = when (style) {
    ColorStyle.TONAL -> seed
    // 鲜艳：提高饱和度
    ColorStyle.VIBRANT -> saturate(seed, 1.35f)
    // 中性：向灰色靠拢
    ColorStyle.NEUTRAL -> lerp(seed, Color(0xFF8A8F98), 0.55f)
    // 强调：更深的对比
    ColorStyle.EXPRESSIVE -> saturate(seed, 1.15f).shade(0.12f)
}

private fun saturate(c: Color, factor: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(c.toArgb(), hsv)
    hsv[1] = (hsv[1] * factor).coerceIn(0f, 1f)
    return Color(android.graphics.Color.HSVToColor(android.graphics.Color.alpha(c.toArgb()), hsv))
}

private fun schemeFrom(seed: Color, dark: Boolean, style: ColorStyle) = if (dark) {
    val primary = seed.tone(0.55f)
    darkColorScheme(
        primary = primary,
        onPrimary = Color(0xFF06121F),
        primaryContainer = seed.shade(if (style == ColorStyle.EXPRESSIVE) 0.35f else 0.55f),
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

private fun FontFamily.of(style: FontStyle): FontFamily = when (style) {
    FontStyle.SYSTEM -> FontFamily.Default
    FontStyle.SERIF -> FontFamily.Serif
    FontStyle.MONOSPACE -> FontFamily.Monospace
    FontStyle.CURSIVE -> FontFamily.Cursive
}

private fun typographyFor(font: FontFamily): Typography {
    val base = Typography()
    fun TextStyle.f() = copy(fontFamily = font)
    return Typography(
        displayLarge = base.displayLarge.f(),
        displayMedium = base.displayMedium.f(),
        displaySmall = base.displaySmall.f(),
        headlineLarge = base.headlineLarge.f(),
        headlineMedium = base.headlineMedium.f(),
        headlineSmall = base.headlineSmall.f(),
        titleLarge = base.titleLarge.f(),
        titleMedium = base.titleMedium.f(),
        titleSmall = base.titleSmall.f(),
        bodyLarge = base.bodyLarge.f(),
        bodyMedium = base.bodyMedium.f(),
        bodySmall = base.bodySmall.f(),
        labelLarge = base.labelLarge.f(),
        labelMedium = base.labelMedium.f(),
        labelSmall = base.labelSmall.f()
    )
}

@Composable
fun PlanLiftTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    seed: Color = Color(0xFF38BDF8),
    dynamic: Boolean = false,
    pureBlack: Boolean = false,
    colorStyle: ColorStyle = ColorStyle.TONAL,
    fontStyle: FontStyle = FontStyle.SYSTEM,
    textScale: Float = 1.0f,
    followSystemScale: Boolean = true,
    ripple: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var colors = if (dynamic && Build.VERSION.SDK_INT >= 31) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        schemeFrom(applyStyle(seed, colorStyle), darkTheme, colorStyle)
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
            // 全面屏：系统栏保持透明，只控制图标明暗；底色由内容自己画
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val density = LocalDensity.current
    val effective = if (followSystemScale) 1f else textScale
    val scaled = if (effective == 1f) {
        density
    } else {
        androidx.compose.ui.unit.Density(density.density, density.fontScale * effective)
    }

    if (ripple) {
        CompositionLocalProvider(LocalDensity provides scaled) {
            MaterialTheme(
                colorScheme = colors,
                typography = typographyFor(FontFamily.Default.of(fontStyle))
            ) { content() }
        }
    } else {
        @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
        CompositionLocalProvider(
            LocalDensity provides scaled,
            androidx.compose.material3.LocalRippleConfiguration provides
                androidx.compose.material3.RippleConfiguration(color = Color.Transparent)
        ) {
            MaterialTheme(
                colorScheme = colors,
                typography = typographyFor(FontFamily.Default.of(fontStyle))
            ) { content() }
        }
    }
}
