# 可复现构建步骤

## 固定工具版本

- 完整 JDK 17（包含 javac 和 jlink，只有 JRE 不能构建）
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
./gradlew :app:assembleRelease
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
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.fx991cnx.emulator.EmulatorTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true
adb pull /sdcard/Pictures/fx991-screens/ screenshots/
```

测试 APK 自行生成原创诊断固件。它只绘制 TEST ROM 和扫描键盘；不含 Casio 专有代码，不随发布 APK 安装，也不模拟任何 Casio 数学功能。`app/src/androidTest` 不编入发布 APK。

## GitHub Actions

根目录 `.github/workflows/fx991cnx-android.yml` 自动安装 SDK/NDK、构建两个 ABI，仅检查打包结果，上传 APK、SHA-256、匹配源码和验证记录。可在分支提交触发或手动运行。临时预览签名不能保证跨 CI 构建直接升级。


## 原机固件兼容性测试（0.2）

`FirmwareAcceptanceTest` 为可选的真实固件测试类，ROM 不在源码或测试 APK 中。CI 仅在临时目录下载用户指定上游发行包，按固定 SHA-256 校验，然后通过 adb 将 ROM 写入测试设备的应用内部目录；测试结束后随临时设备销毁。APK、源码 ZIP、Artifact 和 Release 均不包含此 ROM。

测试只使用屏幕和物理按键路径，断言真实 VRAM 图案与人工核对过的 1/5、0.2、9+√2、矩阵及多项式答案一致，另检查中文 SETUP、ALPHA、DEL 和原机结果/变量恢复。屏幕指纹来自固定测试 ROM，其他固件版本需要另外验收。

Linux 原机探测工具可独立编译：

```sh
c++ -std=c++17 -O2 -Iapp/src/main/cpp/core tests/rom_probe.cpp app/src/main/cpp/core/Chipset/*.cpp app/src/main/cpp/core/Peripheral/*.cpp -o build/rom-probe
build/rom-probe /path/to/your/rom.bin /path/to/input.script
```

脚本支持 `tick 4000000`、`tap 0x60` 和 `dump /path/to/frame.lcd`，输出为 2048 字节 VRAM 加模式、对比度各一字节。该工具不包含或生成固件。

## 0.4：全屏与 LCD 设置版构建方式

本轮按用户要求不运行任何 Android 测试。常用构建仅为：

```sh
./gradlew :app:assembleRelease --no-daemon
```

新增离线渲染依赖 `com.caverock:androidsvg-aar:1.4`（Apache-2.0）。四套 SVG、几何代码、音效与许可证均已提交，无需生成素材即可构建 APK。

如需修改面板，再运行 `python tools/generate_skins.py`。生成器使用 Python fontTools 和 Debian/Ubuntu 的 fonts-dejavu-core、fonts-noto-cjk，把少量标签字形转成路径，并同时写入 `SkinGeometry.java`，避免绘制与热区分开维护。桌面 PNG 预览可由 CairoSVG 渲染，预览工具不属于 Android 运行依赖。面板使用 344×736 设计坐标，以可见机身区域铺满窗口；绘制及触摸使用相同的双轴变换，不再预留外部工具栏。LCD 扩展菜单使用 OFL Noto Sans CJK SC 生成的 13 px 单色字库，可用 `python tools/generate_lcd_font.py`（Pillow）再生成；它不是 ROM 字库。菜单选“原机设置”时回到真实固件 SETUP，计算核心不变。

当前 CI 不安装或启动 Emulator，也不调用 connectedAndroidTest；仅编译、检查签名/对齐/权限/内置素材，然后上传 APK 与完整源码。历史 Android 测试代码保留供后续自愿使用，不作为本次完成条件。

## 可选的授权固件内置构建

默认构建不含 ROM。仅当已核验具体 ROM 允许随 APK 分发时，准备 ROM 文件及对应授权原文，执行：

```sh
./gradlew :app:assembleRelease \
  -PlicensedRomFile=/private/authorized-fx991cnx.rom \
  -PromLicenseFile=/private/REDISTRIBUTION-LICENSE.txt
```

Gradle 仅把这两个文件复制到 build/generated 的 firmware 资源目录；缺少任一文件或 ROM 长度不为 256 KiB 会中止。普通构建会清空该目录，避免上次带固件构建污染后续无 ROM 包。应用首次启动检查这组资源，复制到私有存储后运行，后续优先使用已经导入的 ROM。提供文件不等于自动取得授权，发布者仍须核验。当前 CI 不传这些参数，并检查交付 APK 无 ROM；若今后授权内置，还须调整 CI 检查、来源声明和测试记录，不能直接把未授权文件加入源码。
