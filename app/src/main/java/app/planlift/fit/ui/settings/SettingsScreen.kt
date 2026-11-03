package app.planlift.fit.ui.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.planlift.fit.BuildConfig
import app.planlift.fit.R
import app.planlift.fit.data.reminder.ReminderScheduler
import app.planlift.fit.data.settings.ThemeMode
import app.planlift.fit.data.settings.ThemePrefs
import app.planlift.fit.ui.common.SectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isZh: Boolean,
    onOpenSync: () -> Unit,
    onOpenExport: () -> Unit
) {
    val ctx = LocalContext.current
    var showTheme by remember { mutableStateOf(false) }
    var reminder by remember { mutableStateOf(ReminderScheduler.isEnabled(ctx)) }
    val themeMode by ThemePrefs.mode.collectAsState()

    val themeLabel = when (themeMode) {
        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
        ThemeMode.DARK -> stringResource(R.string.theme_dark)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(stringResource(R.string.nav_settings), fontWeight = FontWeight.SemiBold)
            })
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
            SectionCard {
                MenuRow(
                    title = stringResource(R.string.sync_title),
                    subtitle = stringResource(R.string.sync_entry_hint),
                    onClick = onOpenSync
                )
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                MenuRow(
                    title = stringResource(R.string.theme_title),
                    subtitle = themeLabel,
                    onClick = { showTheme = true }
                )
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                MenuRow(
                    title = stringResource(R.string.settings_export_title),
                    subtitle = stringResource(R.string.settings_export_body),
                    onClick = onOpenExport
                )
            }

            SectionCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.settings_reminder_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            stringResource(R.string.settings_reminder_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = reminder,
                        onCheckedChange = { on ->
                            ReminderScheduler.setEnabled(ctx, on)
                            reminder = on
                        }
                    )
                }
            }

            if (BuildConfig.DEBUG) {
                SectionCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.debug_notify),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { ReminderScheduler.runNow(ctx) }) {
                            Text(stringResource(R.string.debug_trigger))
                        }
                    }
                }
            }

            SectionCard {
                Text(
                    stringResource(R.string.settings_about_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.settings_about_body, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionCard(container = MaterialTheme.colorScheme.errorContainer) {
                Text(
                    stringResource(R.string.settings_disclaimer_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.settings_disclaimer_body),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    if (showTheme) {
        AlertDialog(
            onDismissRequest = { showTheme = false },
            title = { Text(stringResource(R.string.theme_title)) },
            text = {
                Column {
                    ThemeOption(stringResource(R.string.theme_system), themeMode == ThemeMode.SYSTEM) {
                        ThemePrefs.set(ctx, ThemeMode.SYSTEM); showTheme = false
                    }
                    ThemeOption(stringResource(R.string.theme_light), themeMode == ThemeMode.LIGHT) {
                        ThemePrefs.set(ctx, ThemeMode.LIGHT); showTheme = false
                    }
                    ThemeOption(stringResource(R.string.theme_dark), themeMode == ThemeMode.DARK) {
                        ThemePrefs.set(ctx, ThemeMode.DARK); showTheme = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTheme = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun MenuRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
