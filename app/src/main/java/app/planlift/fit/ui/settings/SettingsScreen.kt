package app.planlift.fit.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.planlift.fit.AppContainer
import app.planlift.fit.BuildConfig
import app.planlift.fit.R
import app.planlift.fit.data.export.Exporter
import app.planlift.fit.data.local.SetLogEntity
import app.planlift.fit.data.reminder.ReminderScheduler
import app.planlift.fit.data.sync.SyncConfig
import app.planlift.fit.data.sync.SyncConfigStore
import app.planlift.fit.ui.common.SectionCard
import app.planlift.fit.ui.common.rememberVm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SettingsState(
    val reminder: Boolean = false,
    val busy: Boolean = false,
    val lastExport: String = "",
    val url: String = "",
    val user: String = "",
    val password: String = "",
    val dir: String = "PlanLift",
    val syncStatus: String = "",
    val lastSyncAt: Long = 0L
)

class SettingsViewModel(
    container: AppContainer,
    private val appContext: android.content.Context
) : ViewModel() {
    private val repo = container.repo
    private val sync = container.sync

    private val _state = MutableStateFlow(
        SettingsState(
            reminder = ReminderScheduler.isEnabled(appContext),
            lastSyncAt = SyncConfigStore.lastSyncAt(appContext)
        ).let { initial ->
            val cfg = SyncConfigStore.load(appContext)
            initial.copy(url = cfg.url, user = cfg.user, password = cfg.password, dir = cfg.dir)
        }
    )
    val state = _state.asStateFlow()

    private fun currentConfig() = SyncConfig(
        url = _state.value.url,
        user = _state.value.user,
        password = _state.value.password,
        dir = _state.value.dir
    )

    fun setReminder(enabled: Boolean) {
        ReminderScheduler.setEnabled(appContext, enabled)
        _state.value = _state.value.copy(reminder = enabled)
    }

    fun updateUrl(v: String) { _state.value = _state.value.copy(url = v) }
    fun updateUser(v: String) { _state.value = _state.value.copy(user = v) }
    fun updatePassword(v: String) { _state.value = _state.value.copy(password = v) }
    fun updateDir(v: String) { _state.value = _state.value.copy(dir = v) }

    fun testConnection() = viewModelScope.launch {
        val cfg = currentConfig()
        _state.value = _state.value.copy(busy = true, syncStatus = "测试中…")
        SyncConfigStore.save(appContext, cfg)
        val result = withContext(Dispatchers.IO) { sync.testConnection(cfg) }
        _state.value = _state.value.copy(
            busy = false,
            syncStatus = result.getOrElse { it.message ?: "连接失败" }
        )
    }

    fun syncNow() = viewModelScope.launch {
        val cfg = currentConfig()
        _state.value = _state.value.copy(busy = true, syncStatus = "同步中…")
        SyncConfigStore.save(appContext, cfg)
        val result = withContext(Dispatchers.IO) { sync.sync(cfg) }
        _state.value = _state.value.copy(
            busy = false,
            syncStatus = if (result.ok) {
                "同步完成 · ↑${result.uploaded} ↓${result.downloaded} · rev ${result.remoteRev}"
            } else {
                result.message.ifBlank { "同步失败" }
            },
            lastSyncAt = SyncConfigStore.lastSyncAt(appContext)
        )
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
        _state.value = _state.value.copy(busy = false, lastExport = file.absolutePath)
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
            // ---- 同步 ----
            SectionCard {
                Text(
                    stringResource(R.string.sync_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.sync_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.url,
                    onValueChange = vm::updateUrl,
                    label = { Text(stringResource(R.string.sync_url)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.user,
                    onValueChange = vm::updateUser,
                    label = { Text(stringResource(R.string.sync_user)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.password,
                    onValueChange = vm::updatePassword,
                    label = { Text(stringResource(R.string.sync_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.dir,
                    onValueChange = vm::updateDir,
                    label = { Text(stringResource(R.string.sync_dir)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = vm::testConnection, enabled = !state.busy) {
                        Text(stringResource(R.string.sync_test))
                    }
                    Button(onClick = vm::syncNow, enabled = !state.busy) {
                        Text(stringResource(R.string.sync_now))
                    }
                }
                if (state.syncStatus.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        state.syncStatus,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (state.lastSyncAt > 0) {
                    Text(
                        stringResource(
                            R.string.sync_last,
                            SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                                .format(Date(state.lastSyncAt))
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ---- 提醒 ----
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

            if (BuildConfig.DEBUG) {
                SectionCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.debug_notify),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { ReminderScheduler.runNow(ctx.applicationContext) }) {
                            Text(stringResource(R.string.debug_trigger))
                        }
                    }
                }
            }

            // ---- 导出 ----
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
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.settings_disclaimer_body),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
