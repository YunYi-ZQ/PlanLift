package app.planlift.fit.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.planlift.fit.R
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

            // 主题色
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

            // 纯黑
            SectionCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.theme_pure_black_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            stringResource(R.string.theme_pure_black_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = s.pureBlack,
                        onCheckedChange = { ThemePrefs.update(ctx, s.copy(pureBlack = it)) }
                    )
                }
            }
        }
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
