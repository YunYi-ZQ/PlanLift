# W4 交付报告（WebDAV 同步）· v1.0.0

> 日期：2026-11-03　｜　对接真实服务器：坚果云 `https://dav.jianguoyun.com/dav/`

## 一、协议设计

```
/PlanLift/
└── state/
    ├── snapshot.json     # 权威全量快照（含 rev）
    └── snapshot.tmp      # 上传中转，成功后 MOVE 覆盖
```

**一次同步**

1. `GET /PlanLift/state/snapshot.json`（404 视为首次）
2. 本地 Room 全量读出
3. 逐表逐条合并：**rev 大者胜** → rev 相同比 `updatedAt` → 再相同比 `deviceId`（保证两台设备算出同一结果）
4. 合并结果写回 Room（含 `deleted` 墓碑，删除同样传播）
5. 生成新快照（rev+1）
6. **原子替换**：`PUT snapshot.tmp` → `MOVE` 覆盖 `snapshot.json`；MOVE 不被支持时退回直接 PUT
7. 记录最后同步时间

## 二、代码结构

| 文件 | 职责 |
|---|---|
| `domain/SyncMerge.kt` | 纯函数合并算法（可单测） |
| `data/sync/Snapshot.kt` | 快照数据结构 + 结果类型 |
| `data/sync/WebDavClient.kt` | PROPFIND / MKCOL / GET / PUT / MOVE / DELETE，Basic 认证，10s 连接超时 |
| `data/sync/SyncConfig.kt` | 配置存取，密码走 **EncryptedSharedPreferences**（Keystore 主密钥） |
| `data/sync/SyncManager.kt` | 编排：读远端 → 合并 → 落本地 → 原子上传 |
| `ui/settings/SettingsScreen.kt` | 地址 / 用户名 / 密码 / 目录 / 测试连接 / 立即同步 |

## 三、测试

### 单元测试（8 项，全过）

- 仅远端有 → 下载
- 仅本地有 → 保留
- rev 高者胜（即使时间更早）
- rev 相同比时间
- rev 与时间都相同 → deviceId 字典序，正反向合并结果一致
- 键取并集
- 完全相同的记录计为 same
- `deleted` 随胜者传播

合计单测 **20 项**（加重引擎 12 + 合并 8），`./gradlew testDebugUnitTest` 全绿。

### 真机双设备（A → 清空 → B）

```
[PASS] A 加入计划、练一组
[PASS] A 同步完成 · ↑3 ↓4 · rev 3
[PASS] B 清空后为空状态
[PASS] B 同步完成 · ↑0 ↓7 · rev 4
[PASS] B 今日恢复：新手全身 3 练 · 全身 C · 第 1 / 8 周
[PASS] B 历史恢复：全身 A 21:48（250 kg）、全身 A 21:30（200 kg）
```

## 四、踩到的坑

| # | 问题 | 原因 | 修复 |
|---|---|---|---|
| 1 | 测试连接失败 | 清单里从来没有 `INTERNET` 权限（前几周全是本地功能） | 加 `INTERNET` + `ACCESS_NETWORK_STATE` |
| 2 | 连接失败时只有「连不上」三个字 | `ping()` 把异常吞了 | 改为返回失败原因（HTTP 码 / 异常类名），设置页直接展示 |
| 3 | 自动化输入把 URL 变成「https：//大V。坚果云。com/大V/」 | 中文输入法把拉丁字符转成全角/中文 | 仅 debug 支持 intent 预填配置（`-e sync_url …`），release 无此代码 |
| 4 | 同步后「今日」不显示计划 | 恢复的那条 enrollment 的 `status` 是 `dropped`（此前测试点过退出） | 非 Bug；换带活跃计划的场景重测通过 |

## 五、安全处理

- 密码不落明文：`EncryptedSharedPreferences` + AES256_GCM，主密钥由 Android Keystore 托管
- 密码不会出现在日志、导出文件、快照里（快照只含训练数据）
- 自动化脚本里的账号密码已全部改为读环境变量；测试日志已删除
- **建议**：这个应用密码已在对话中出现过，如果需要长期使用，去坚果云网页端吊销并重新生成一个

## 六、当前边界

1. 同步是手动触发（设置页按钮），未做「训练结束自动同步」
2. 冲突只做记录级取舍，没有「保留双方」的分叉视图
3. 只支持 Basic 认证；群晖等仅 Digest 的服务器暂不支持（设计里留了位置）
4. 快照全量上传，数据量到几万组后需要改成增量（个人使用 5–10 年都到不了）

## 七、命令

```bash
./gradlew testDebugUnitTest    # 20 项单测
./gradlew assembleDebug
```
