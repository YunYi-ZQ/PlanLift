# PlanLift 环境基线（W0 实测）

> 记录时间：2026-11-03　｜　状态：**W0 通过（4/5，剩真机安装待验）**

## 一、最终生效的工具链

| 项 | 值 |
|---|---|
| 工程路径 | `D:\Dev\PlanLift`（**不可含中文**，AGP 硬性限制） |
| JDK | Adoptium **17.0.20.1**（`D:/Java/jdk-17.0.20.1+1`） |
| Android Studio | 2026.1.3，自带 JBR **25.0.2**（仅 Studio 自身使用，不参与 Gradle 构建） |
| Android SDK | `D:/Program Files/Android/Sdk`（platform-tools 37.0.1，许可已接受） |
| platform / build-tools | android-34 / 34.0.0 |
| Gradle | 8.5（复用 `~/.gradle/wrapper/dists` 已有发行版，未重新下载） |
| AGP / Kotlin | 8.2.0 / 2.0.0（+ `plugin.compose`） |
| compileSdk / targetSdk / minSdk | 34 / 34 / 26 |
| ABI | arm64-v8a |
| Maven 源 | 阿里云 google / central / gradle-plugin（`maven.google.com` 实测不可达） |

## 二、实测结果

| 指标 | 实测 | 目标 | 结论 |
|---|---|---|---|
| 首次构建 | 1 min 53 s（含依赖下载） | — | ✅ |
| 增量构建 | 3 s | — | ✅ |
| **断网构建**（删 APK 后 `--offline` 重建） | **3 s，成功** | 成功 | ✅ |
| APK 体积（debug） | **8.6 MB** | < 8 MB | ⚠️ debug 含 tooling；release + R8 后预计 < 4 MB |
| 包名 | `app.planlift.fit.debug` | — | ✅ |
| 版本 | versionCode 1 / versionName 0.1.0-debug | — | ✅ |
| minSdk / targetSdk | 26 / 34 | — | ✅ |
| 打包语言 | `--_--`（默认中文）+ `en` | zh + en | ✅ |
| native-code | arm64-v8a | arm64 | ✅ |
| 真机安装 | **未执行（adb 无设备）** | 装到 K60 | ⏳ |

## 三、W0 踩到的三个坑（已解决，后续必看）

1. **JDK 25 不能用于 Gradle 8.5**
   `java.lang.IllegalArgumentException: 25.0.2`，来自 Gradle 嵌入式 Kotlin 的 `JavaVersion.parse()`。
   → 解决：装 Adoptium JDK 17，`gradle.properties` 写 `org.gradle.java.home=D:/Java/jdk-17.0.20.1+1`。

2. **AGP 拒绝非 ASCII 工程路径**
   原路径 `...\Desktop\大四\workout plan\PlanLift` 直接失败，提示 "Your project path contains non-ASCII characters"。
   → 解决：工程移到 `D:/Dev/PlanLift`。**不要**用 `android.overridePathCheck=true` 绕过，AAPT2/NDK 后续会出别的问题。

3. **英文资源里的撇号未转义**
   `Today's Workout` → AAPT 报 "Invalid unicode escape sequence in string"。
   → 解决：写成 `Today\'s Workout`。

## 四、构建命令

```bash
cd /d/Dev/PlanLift
export JAVA_HOME="D:/Java/jdk-17.0.20.1+1"
./gradlew assembleDebug            # 在线
./gradlew assembleDebug --offline  # 断网可复现
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

## 五、待办（W0 收尾）

- [ ] 手机连 USB 或开无线调试 → `adb install -r` → 确认打开不闪退
- [ ] 切英文系统语言（或 `adb shell cmd locale set-app-locales app.planlift.fit.debug --locales en`）→ 验证全英文
- [ ] 记录冷启动耗时
