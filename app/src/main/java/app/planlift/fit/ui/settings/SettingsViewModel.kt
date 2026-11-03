package app.planlift.fit.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.planlift.fit.AppContainer
import app.planlift.fit.data.export.Exporter
import app.planlift.fit.data.local.SetLogEntity
import app.planlift.fit.data.reminder.ReminderScheduler
import app.planlift.fit.data.sync.SyncConfig
import app.planlift.fit.data.sync.SyncConfigStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
