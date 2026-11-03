package app.planlift.fit.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.planlift.fit.AppContainer
import app.planlift.fit.R
import app.planlift.fit.data.export.Exporter
import app.planlift.fit.data.local.SetLogEntity
import app.planlift.fit.data.reminder.ReminderScheduler
import app.planlift.fit.ui.common.SectionCard
import app.planlift.fit.ui.common.rememberVm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class SettingsState(
    val reminder: Boolean = false,
    val busy: Boolean = false,
    val lastExport: String = ""
)

class SettingsViewModel(container: AppContainer, private val appContext: android.content.Context) : ViewModel() {
    private val repo = container.repo
    private val _state = MutableStateFlow(SettingsState(reminder = ReminderScheduler.isEnabled(appContext)))
    val state = _state.asStateFlow()

    fun setReminder(enabled: Boolean) {
        ReminderScheduler.setEnabled(appContext, enabled)
        _state.value = _state.value.copy(reminder = enabled)
    }

    fun export(json: Boolean, onShare: (File) -> Unit) = viewModelScope.launch {
        _state.value = _state.value.copy(busy = true)
        val sessions = repo.allSessionsOnce()
        val sets: Map<String, List<SetLogEntity>> = sessions.associate { s ->
            s.id to repo.setsOf(s.id)
        }
        val file = withContext(Dispatchers.IO) {
            if (json) {
                val metrics = repo.allMetricsOnce().map { m ->
                    m.date to mapOf(
                        "weightKg" to m.weightKg,
                        "bodyFat" to m.bodyFat,
                        "waistCm" to m.waistCm,
                        "hipCm" to m.hipCm
                    )
                }
                Exporter.exportJson(appContext, repo.activeEnrollmentOnce(), sessions, sets, metrics)
            } else {
                Exporter.exportCsv(appContext, sessions, sets)
            }
        }
        _state.value = SettingsState(
            reminder = _state.value.reminder,
            busy = false,
            lastExport = file.absolutePath
        )
        onShare(file)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(isZh: Boolean) {
    val ctx = LocalContext.current
    val vm = rememberVm { SettingsViewModel(it, ctx.applicationContext) }
    val state by vm.state.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) vm.setReminder(true) }

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
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
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
                        checked = state.reminder,
                        onCheckedChange = { on ->
                            if (on && Build.VERSION.SDK_INT >= 33) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                vm.setReminder(on)
                            }
                        }
                    )
                }
            }

            if (app.planlift.fit.BuildConfig.DEBUG) {
                SectionCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "调试：测试通知",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = {
                            ReminderScheduler.runNow(ctx.applicationContext)
                        }) { Text("立即触发") }
                    }
                }
            }

            SectionCard {
                Text(
                    stringResource(R.string.settings_export_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.settings_export_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { vm.export(json = true) { Exporter.share(ctx, it) } },
                        enabled = !state.busy
                    ) { Text(stringResource(R.string.settings_export_json)) }
                    OutlinedButton(
                        onClick = { vm.export(json = false) { Exporter.share(ctx, it) } },
                        enabled = !state.busy
                    ) { Text(stringResource(R.string.settings_export_csv)) }
                }
                if (state.lastExport.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.settings_export_done, state.lastExport),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SectionCard {
                Text(
                    stringResource(R.string.settings_about_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.settings_about_body, app.planlift.fit.BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SectionCard {
                Text(
                    stringResource(R.string.settings_next_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.settings_next_body),
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
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.settings_disclaimer_body),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
