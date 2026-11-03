package app.planlift.fit.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import app.planlift.fit.BuildConfig
import app.planlift.fit.R
import app.planlift.fit.data.reminder.ReminderScheduler
import app.planlift.fit.data.settings.ThemeMode
import app.planlift.fit.data.settings.ThemePrefs
import app.planlift.fit.ui.common.SectionCard
import app.planlift.fit.ui.common.pressable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isZh: Boolean,
    onOpenSync: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val ctx = LocalContext.current
    var reminder by remember { mutableStateOf(ReminderScheduler.isEnabled(ctx)) }
    var hour by remember { mutableIntStateOf(ReminderScheduler.hour(ctx)) }
    var minute by remember { mutableIntStateOf(ReminderScheduler.minute(ctx)) }
    var showTimePicker by remember { mutableStateOf(false) }
    var testStatus by remember { mutableStateOf("") }
    val themeSettings by ThemePrefs.state.collectAsState()

    val themeLabel = when (themeSettings.mode) {
        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
        ThemeMode.DARK -> stringResource(R.string.theme_dark)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            ReminderScheduler.setEnabled(ctx, true)
            reminder = true
        }
    }

    fun ensurePermissionThen(action: () -> Unit) {
        val granted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) action() else permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
                    onClick = onOpenTheme
                )
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                MenuRow(
                    title = stringResource(R.string.settings_export_title),
                    subtitle = stringResource(R.string.settings_export_body),
                    onClick = onOpenExport
                )
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                MenuRow(
                    title = stringResource(R.string.about_title),
                    subtitle = stringResource(R.string.about_entry_hint),
                    onClick = onOpenAbout
                )
            }

            SectionCard {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!reminder) {
                                ensurePermissionThen {
                                    ReminderScheduler.setEnabled(ctx, true)
                                    reminder = true
                                }
                            } else {
                                ReminderScheduler.setEnabled(ctx, false)
                                reminder = false
                            }
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.settings_reminder_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Switch(checked = reminder, onCheckedChange = null)
                }
                if (reminder) {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { showTimePicker = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.reminder_time_label),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "%02d:%02d".format(hour, minute),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (BuildConfig.DEBUG) {
                SectionCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.debug_notify),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (testStatus.isNotEmpty()) {
                                Text(
                                    testStatus,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        TextButton(onClick = {
                            ensurePermissionThen {
                                ReminderScheduler.runNow(ctx)
                                testStatus = "已发送测试通知"
                            }
                        }) {
                            Text(stringResource(R.string.debug_trigger))
                        }
                    }
                }
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

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.reminder_time_label)) },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(onClick = {
                    hour = pickerState.hour
                    minute = pickerState.minute
                    ReminderScheduler.setTime(ctx, hour, minute)
                    showTimePicker = false
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
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
            .padding(vertical = 8.dp)
            .pressable(onClick = onClick),
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
