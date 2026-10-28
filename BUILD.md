# 可复现构建步骤

## 固定工具版本

- JDK 17（本地也验证 JDK 21）
- Gradle 8.9（项目 Wrapper）、Android Gradle Plugin 8.7.3
- Android compile/target SDK 35、min SDK 29，Build Tools 35.0.0
- NDK 27.2.12479018、CMake 3.22.1、C++17
- 发布 arm64-v8a，另构建用于 Emulator 的 x86_64；JNI 库配置 16 KiB ELF 页对齐

## 本地

安装 Android SDK 命令行工具后：

```sh
sdkmanager --licenses
sdkmanager 'platforms;android-35' 'build-tools;35.0.0' 'platform-tools' 'ndk;27.2.12479018' 'cmake;3.22.1'
export ANDROID_HOME=/your/android-sdk
# 在 Android Studio 中打开 fx991cnx-android 目录亦可。
keytool -genkeypair -keystore /your/preview.keystore -storepass android \
  -alias androiddebugkey -keypass android -dname 'CN=Android Debug,O=Android,C=US' \
  -keyalg RSA -keysize 2048 -validity 10000
export FX_KEYSTORE=/your/preview.keystore
./gradlew :app:assembleRelease :app:assembleDebug :app:assembleDebugAndroidTest
sha256sum app/build/outputs/apk/release/fx991cnx-arm64-release.apk
```

正式签名可通过 `FX_KEYSTORE`、`FX_STORE_PASSWORD`、`FX_KEY_ALIAS`、`FX_KEY_PASSWORD` 注入，禁止提交 keystore 或密码。未注入时使用标准 debug.keystore；首次编译前需如上生成。可重复产生相同功能与 ABI 的 APK；签名、工具元数据影响 APK 字节，不能承诺跨签名逐字节一致。

受代理控制的环境需要通过用户自己的 Gradle `gradle.properties` 设置 `systemProp.http.proxyHost`、`systemProp.http.proxyPort`、HTTPS 对应项。不要将环境凭据写入项目。

## 自动化测试

Linux 安装 g++ 后执行：

```sh
tests/run-native.sh
```

运行 AddressSanitizer/UndefinedBehaviorSanitizer/LeakSanitizer，覆盖 MCU 指令、RAM/LCD 访问、49 键矩阵、全状态往返与冷实例恢复。某些 ptrace 沙箱不能运行 LeakSanitizer，应在正常 Linux 或 CI 执行。

Android 测试：

```sh
sdkmanager 'emulator' 'system-images;android-30;google_apis;x86_64'
# 创建、启动 API 30 x86_64 AVD 后：
adb shell settings put global airplane_mode_on 1
adb shell svc wifi disable
adb shell svc data disable
./gradlew :app:connectedDebugAndroidTest
adb pull /sdcard/Android/data/org.fx991cnx.emulator/files/screenshots/ screenshots/
```

测试 APK 自行生成原创诊断固件。它只绘制 TEST ROM 和扫描键盘；不含 Casio 专有代码，不随发布 APK 安装，也不模拟任何 Casio 数学功能。`app/src/androidTest` 不编入发布 APK。

## GitHub Actions

根目录 `.github/workflows/fx991cnx-android.yml` 自动安装 SDK/NDK、构建两个 ABI、运行 Linux 原生测试和离线 Android Emulator 测试，上传 APK、SHA-256、匹配源码和测试报告。可在分支提交触发或手动运行。临时预览签名不能保证跨 CI 构建直接升级。
