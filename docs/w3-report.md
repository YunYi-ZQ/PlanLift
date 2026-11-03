# W3 交付报告（体验与数据）

> 版本：v0.4.0　｜　日期：2026-11-03

## 一、本期交付

| 模块 | 内容 | 验证 |
|---|---|---|
| **训练提醒** | WorkManager 每日 18:00；点通知直接进 App。设置页开关，Android 13+ 自动申请通知权限 | 通知栏实测出现「今天该练了 / 下一个课表：全身 B（新手全身 3 练）」 |
| **导出** | JSON（含进度/训练/每组/围度，结构即 W4 快照雏形）+ CSV（可拖进 Excel） | 文件落在 `Android/data/<pkg>/files/exports/`，CSV 表头与数据实测正确 |
| **Material You** | Android 12+ 跟随壁纸取色，低版本回落品牌色 | Android 15 真机启动正常，无崩溃 |
| **围度记录** | 体重/体脂/腰围/臀围，保存后展示最近一条 | 实测写入 72 / 18 / 80 / 95，四项无串位 |
| 调试自检 | debug 构建下设置页有「立即触发」按钮，可预览通知 | 用于本次验证，不影响 release |

## 二、过程中修复的真实问题

| # | 问题 | 现象 | 修复 |
|---|---|---|---|
| 1 | 围度对话框被键盘遮挡 | 手机弹出键盘后，下面两个输入框点不到 | 对话框加 `verticalScroll` + `imePadding` |
| 2 | 版本号写死 | 关于卡片永远显示 v0.2.0 | 改用 `BuildConfig.VERSION_NAME` |
| 3 | BuildConfig 不可用 | AGP 8 默认不生成 | `buildFeatures { buildConfig = true }` |

## 三、测试中踩的坑（测试代码问题，非 App）

- Compose 的 Switch 在 uiautomator 里不是 `android.widget.Switch`，要按 `checkable="true"` 找。
- 键盘弹出后旧坐标失效，必须在每次输入前重新 dump 并收键盘（KEYCODE_ESCAPE）。
- 之前的「崩溃日志」检查是假阳性：crash buffer 里其它 App 的旧日志被算进来了，按包名过滤后为 0 条。

## 四、当前能力边界

1. 提醒时间固定 18:00，暂不可自定义
2. 导出走系统分享面板，没有「一键存到下载目录」
3. 围度只有最近一条的展示，没有折线趋势
4. 同步仍未做（W4）

## 五、命令

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```
