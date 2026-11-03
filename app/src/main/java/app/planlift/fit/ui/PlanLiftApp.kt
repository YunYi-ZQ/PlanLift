package app.planlift.fit.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.planlift.fit.R
import app.planlift.fit.data.settings.ThemeMode
import app.planlift.fit.data.settings.ThemePrefs
import app.planlift.fit.ui.nav.AppNav
import app.planlift.fit.ui.theme.PlanLiftTheme
import kotlinx.coroutines.delay

@Composable
fun PlanLiftApp() {
    val s by ThemePrefs.state.collectAsState()
    val dark = when (s.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    PlanLiftTheme(
        darkTheme = dark,
        seed = Color(s.seed),
        dynamic = s.dynamic,
        pureBlack = s.pureBlack,
        colorStyle = s.colorStyle,
        fontStyle = s.font,
        textScale = s.textScale,
        followSystemScale = s.followSystemScale,
        ripple = s.ripple
    ) {
        var splash by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            delay(780)
            splash = false
        }
        if (splash) SplashScreen() else AppNav()
    }
}

/** 简洁启动动画：App 图标缩放淡入 + 名称上浮 */
@Composable
private fun SplashScreen() {
    val scale = remember { Animatable(0.84f) }
    val fade = remember { Animatable(0f) }
    val rise = remember { Animatable(16f) }

    LaunchedEffect(Unit) {
        android.util.Log.d("PlanLift", "splash-shown")
        scale.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        fade.animateTo(1f, tween(600))
        rise.animateTo(0f, tween(680, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        alpha = fade.value
                    }
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(30.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "PlanLift",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .alpha(fade.value)
                    .graphicsLayer { translationY = rise.value }
            )
        }
    }
}
