# PlanLift

> 计划先行，重量跟上 · Plan first. Load follows.

一个**离线优先**的 Android 训练计划 App：内置 16 套成品计划（男 8 / 女 8），自动计算下次该加多少重量。

## 状态

**W1 完成** — 计划库 / 今日训练 / 逐组记录 / 组间计时 / 历史（v0.2.0）

## 技术栈

| 项 | 版本 |
|---|---|
| Kotlin | 2.0.0 |
| AGP / Gradle | 8.2.0 / 8.5 |
| Compose BOM | 2024.09.03（Material 3） |
| compileSdk / targetSdk / minSdk | 34 / 34 / 26 |
| JDK | 17（Adoptium 17.0.20.1） |
| ABI | arm64-v8a |

## 构建

```bash
export JAVA_HOME="D:/Java/jdk-17.0.20.1+1"
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` 里的 `sdk.dir` 指向 `D:/Program Files/Android/Sdk`，该文件不入库。

## 注意事项

- **路径不能含中文**：AGP 会直接拒绝非 ASCII 项目路径，工程放在 `D:/Dev/PlanLift`。
- **必须用 JDK 17**：Gradle 8.5 的嵌入式 Kotlin 无法解析 JDK 25 的版本号（`JavaVersion.parse("25.0.2")` 抛异常）。
- **Maven 走阿里云**：本机 `maven.google.com` 不通（实测 20s 超时）。
- 英文串里的撇号要写成 `\'`，否则 AAPT 报 "Invalid unicode escape sequence"。

## 路线图

- [x] W0 环境 + 双语骨架 + 可安装 APK
- [ ] W1 MVP：计划库 / 今日训练 / 逐组记录 / 组间计时 / 历史
- [ ] W2 计划引擎：自动加重、8 周周期化、组数看板、e1RM
- [ ] W3 体验：提醒、图表、导出、主题
- [ ] W4 WebDAV 同步（快照 + 每设备变更 + 原子替换）
- [ ] W5 开源发布：签名 APK、README、CI

## 许可

MIT
