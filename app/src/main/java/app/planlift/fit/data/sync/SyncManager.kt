package app.planlift.fit.data.sync

import android.content.Context
import app.planlift.fit.data.repo.WorkoutRepository
import kotlinx.serialization.json.Json

class SyncManager(
    private val context: Context,
    private val repo: WorkoutRepository
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun testConnection(config: SyncConfig): Result<String> = runCatching {
        val client = WebDavClient(config.url, config.user, config.password)
        require(config.configured) { "请先填写服务器地址、用户名和密码" }
        client.ping()?.let { reason -> error("连接失败：$reason") }
        if (!client.ensureDir(config.dir)) error("无法创建目录 ${config.dir}")
        "连接成功"
    }

    /**
     * 一次完整同步：
     * GET 远端快照 → 与本地逐条合并（rev 大者胜）→ 写回本地 → 原子上传新快照。
     */
    suspend fun sync(config: SyncConfig): SyncResult {
        if (!config.configured) return SyncResult(false, message = "未配置")
        val client = WebDavClient(config.url, config.user, config.password)
        val stateDir = "${config.dir.trim('/')}/state"
        val remotePath = "$stateDir/snapshot.json"
        val tmpPath = "$stateDir/snapshot.tmp"

        if (!client.ensureDir(stateDir)) return SyncResult(false, message = "无法创建目录")

        val remote: Snapshot? = try {
            client.get(remotePath)?.let { json.decodeFromString<Snapshot>(it.decodeToString()) }
        } catch (e: Exception) {
            null
        }

        val local = repo.collectSnapshot()
        val (merged, stats) = repo.mergeInto(local, remote)

        val newRev = maxOf(local.rev, remote?.rev ?: 0L) + 1
        val mergedSnapshot = Snapshot(
            schema = 1,
            rev = newRev,
            updatedAt = System.currentTimeMillis(),
            deviceId = repo.deviceIdValue(),
            profiles = merged.profiles,
            enrollments = merged.enrollments,
            sessions = merged.sessions,
            setLogs = merged.setLogs,
            metrics = merged.metrics,
            records = merged.records
        )
        repo.applySnapshot(mergedSnapshot)

        val bytes = json.encodeToString(Snapshot.serializer(), mergedSnapshot).encodeToByteArray()

        // 原子替换：先写 tmp，再 MOVE 覆盖；MOVE 不支持时退回直接 PUT
        val uploaded = if (client.put(tmpPath, bytes) && client.move(tmpPath, remotePath)) {
            true
        } else {
            client.put(remotePath, bytes)
        }

        if (uploaded) SyncConfigStore.markSynced(context, System.currentTimeMillis())

        return SyncResult(
            ok = uploaded,
            uploaded = stats.localWins,
            downloaded = stats.remoteWins,
            conflicts = 0,
            remoteRev = newRev,
            message = if (uploaded) "同步完成 · rev $newRev" else "上传失败"
        )
    }
}
