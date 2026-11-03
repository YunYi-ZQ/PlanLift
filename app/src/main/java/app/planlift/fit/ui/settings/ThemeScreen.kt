package app.planlift.fit.ui.settings

import android.app.Activity
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.planlift.fit.R
import app.planlift.fit.data.settings.AppLanguage
import app.planlift.fit.data.settings.ColorStyle
import app.planlift.fit.data.settings.FontStyle
import app.planlift.fit.data.settings.ThemeMode
import app.planlift.fit.data.settings.ThemePrefs
import app.planlift.fit.ui.common.SectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val s by ThemePrefs.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.theme_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("‹ " + stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 实时预览
            SectionCard(container = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    stringResource(R.string.theme_preview_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.theme_preview_heading),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.theme_preview_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {}) { Text(stringResource(R.string.theme_preview_button)) }
                    OutlinedButton(onClick = {}) { Text(stringResource(R.string.action_cancel)) }
                }
            }

            // 模式
            SectionCard {
                Text(
                    stringResource(R.string.theme_mode_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(10.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = s.mode == mode,
                            onClick = { ThemePrefs.update(ctx, s.copy(mode = mode)) },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size)
                        ) {
                            Text(
                                when (mode) {
                                    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                                    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                    ThemeMode.DARK -> stringResource(R.string.theme_dark)
                                }
                            )
                        }
                    }
                }
            }

            // 主题色 + 配色风格 + 动态取色
            SectionCard {
                Text(
                    stringResource(R.string.theme_seed_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.theme_seed_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ThemePrefs.PRESET_SEEDS.take(4).forEach { seed ->
                        Swatch(seed = seed, selected = s.seed == seed && !s.dynamic) {
                            ThemePrefs.update(ctx, s.copy(seed = seed, dynamic = false))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ThemePrefs.PRESET_SEEDS.drop(4).forEach { seed ->
                        Swatch(seed = seed, selected = s.seed == seed && !s.dynamic) {
                            ThemePrefs.update(ctx, s.copy(seed = seed, dynamic = false))
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.theme_style_title),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColorStyle.entries.forEach { style ->
                        FilterChip(
                            selected = s.colorStyle == style,
                            onClick = { ThemePrefs.update(ctx, s.copy(colorStyle = style)) },
                            label = {
                                Text(
                                    when (style) {
                                        ColorStyle.TONAL -> stringResource(R.string.style_tonal)
                                        ColorStyle.VIBRANT -> stringResource(R.string.style_vibrant)
                                        ColorStyle.NEUTRAL -> stringResource(R.string.style_neutral)
                                        ColorStyle.EXPRESSIVE -> stringResource(R.string.style_expressive)
                                    }
                                )
                            }
                        )
                    }
                }

                if (Build.VERSION.SDK_INT >= 31) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.theme_dynamic_title),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                stringResource(R.string.theme_dynamic_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = s.dynamic,
                            onCheckedChange = { ThemePrefs.update(ctx, s.copy(dynamic = it)) }
                        )
                    }
                }
            }

            // 字体 + 文本缩放
            SectionCard {
                Text(
                    stringResource(R.string.theme_font_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FontStyle.entries.forEach { f ->
                        FilterChip(
                            selected = s.font == f,
                            onClick = { ThemePrefs.update(ctx, s.copy(font = f)) },
                            label = {
                                Text(
                                    when (f) {
                                        FontStyle.SYSTEM -> stringResource(R.string.font_system)
                                        FontStyle.SERIF -> stringResource(R.string.font_serif)
                                        FontStyle.MONOSPACE -> stringResource(R.string.font_mono)
                                        FontStyle.CURSIVE -> stringResource(R.string.font_cursive)
                                    }
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.theme_text_scale_title),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.9f, 1.0f, 1.15f, 1.3f).forEach { scale ->
                        FilterChip(
                            selected = kotlin.math.abs(s.textScale - scale) < 0.01f,
                            onClick = { ThemePrefs.update(ctx, s.copy(textScale = scale)) },
                            label = { Text("${(scale * 100).toInt()}%") }
                        )
                    }
                }
            }

            // 交互与底栏
            SectionCard {
                ToggleRow(
                    title = stringResource(R.string.theme_ripple_title),
                    subtitle = stringResource(R.string.theme_ripple_hint),
                    checked = s.ripple,
                    onChange = { ThemePrefs.update(ctx, s.copy(ripple = it)) }
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                ToggleRow(
                    title = stringResource(R.string.theme_nav_floating_title),
                    subtitle = stringResource(R.string.theme_nav_floating_hint),
                    checked = s.navFloating,
                    onChange = { ThemePrefs.update(ctx, s.copy(navFloating = it)) }
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                ToggleRow(
                    title = stringResource(R.string.theme_glass_title),
                    subtitle = stringResource(R.string.theme_glass_hint),
                    checked = s.glass,
                    onChange = { ThemePrefs.update(ctx, s.copy(glass = it)) }
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                ToggleRow(
                    title = stringResource(R.string.theme_pure_black_title),
                    subtitle = stringResource(R.string.theme_pure_black_hint),
                    checked = s.pureBlack,
                    onChange = { ThemePrefs.update(ctx, s.copy(pureBlack = it)) }
                )
            }

            // 语言
            SectionCard {
                Text(
                    stringResource(R.string.theme_language_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.theme_language_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = s.language == lang,
                            onClick = {
                                ThemePrefs.update(ctx, s.copy(language = lang))
                                applyLanguage(ctx, lang)
                            },
                            label = {
                                Text(
                                    when (lang) {
                                        AppLanguage.SYSTEM -> stringResource(R.string.lang_system)
                                        AppLanguage.ZH -> "中文"
                                        AppLanguage.EN -> "English"
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

/** Android 13+ 用系统 API 设应用语言；更早版本重建 Activity 由 attachBaseContext 生效 */
private fun applyLanguage(ctx: android.content.Context, lang: AppLanguage) {
    val tag = when (lang) {
        AppLanguage.SYSTEM -> null
        AppLanguage.ZH -> "zh"
        AppLanguage.EN -> "en"
    }
    if (Build.VERSION.SDK_INT >= 33) {
        val lm = ctx.getSystemService(LocaleManager::class.java)
        lm.applicationLocales = if (tag == null) {
            LocaleList.getEmptyLocaleList()
        } else {
            LocaleList.forLanguageTags(tag)
        }
    } else {
        (ctx as? Activity)?.recreate()
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun Swatch(seed: Long, selected: Boolean, onClick: () -> Unit) {
    val color = Color(seed)
    Box(
        Modifier
            .semantics { contentDescription = "seed-$seed" }
            .size(42.dp)
            .background(color, CircleShape)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    ) {
        if (selected) {
            Text(
                "✓",
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
