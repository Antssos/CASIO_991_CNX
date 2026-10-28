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
# 在 Android Studio 中打开本仓库根目录亦可。
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

本仓库 `.github/workflows/fx991cnx-android.yml` 自动安装 SDK/NDK、构建两个 ABI，仅检查打包结果，上传 APK、SHA-256、匹配源码和验证记录。可在分支提交触发或手动运行。临时预览签名不能保证跨 CI 构建直接升级。


## 原机固件兼容性测试（0.2）

`FirmwareAcceptanceTest` 为可选的真实固件测试类，ROM 不在源码或测试 APK 中。CI 仅在临时目录下载用户指定上游发行包，按固定 SHA-256 校验，然后通过 adb 将 ROM 写入测试设备的应用内部目录；测试结束后随临时设备销毁。APK、源码 ZIP、Artifact 和 Release 均不包含此 ROM。

测试只使用屏幕和物理按键路径，断言真实 VRAM 图案与人工核对过的 1/5、0.2、9+√2、矩阵及多项式答案一致，另检查中文 SETUP、ALPHA、DEL 和原机结果/变量恢复。屏幕指纹来自固定测试 ROM，其他固件版本需要另外验收。

Linux 原机探测工具可独立编译：

```sh
c++ -std=c++17 -O2 -Iapp/src/main/cpp/core tests/rom_probe.cpp app/src/main/cpp/core/Chipset/*.cpp app/src/main/cpp/core/Peripheral/*.cpp -o build/rom-probe
build/rom-probe /path/to/your/rom.bin /path/to/input.script
```

脚本支持 `tick 4000000`、`tap 0x60` 和 `dump /path/to/frame.lcd`，输出为 2048 字节 VRAM 加模式、对比度各一字节。该工具不包含或生成固件。

## 0.5：原机 SETUP 末页扩展构建方式

本轮按用户要求不运行任何 Android 测试。常用构建仅为：

```sh
./gradlew :app:assembleRelease --no-daemon
```

新增离线渲染依赖 `com.caverock:androidsvg-aar:1.4`（Apache-2.0）。四套 SVG、几何代码、音效与许可证均已提交，无需生成素材即可构建 APK。

如需修改面板，再运行 `python tools/generate_skins.py`。生成器使用 Python fontTools 和 Debian/Ubuntu 的 fonts-dejavu-core、fonts-noto-cjk，把少量标签字形转成路径，并同时写入 `SkinGeometry.java`，避免绘制与热区分开维护。桌面 PNG 预览可由 CairoSVG 渲染，预览工具不属于 Android 运行依赖。面板使用 344×736 设计坐标，以可见机身区域铺满窗口；绘制及触摸使用相同的双轴变换，不再预留外部工具栏。LCD 扩展只匹配原机最后一页的标签区域，保留语言/对比度，追加主题/按键。字库改为 OFL Fusion Pixel 12px 中文点阵及 DejaVu 数字，按 12×12 字格、16 像素行距生成 JSON 与 LcdGlyphs.java；无需运行时加载字体。按键按事件顺序在 MCU 工作线程路由，成对处理按下/释放，计算核心不变。

当前 CI 不安装或启动 Emulator，也不调用 connectedAndroidTest；仅编译、检查签名/对齐/权限/内置素材，然后上传 APK 与完整源码。历史 Android 测试代码保留供后续自愿使用，不作为本次完成条件。

## 可选的授权固件内置构建

默认构建不含 ROM。仅当已核验具体 ROM 允许随 APK 分发时，准备 ROM 文件及对应授权原文，执行：

```sh
./gradlew :app:assembleRelease \
  -PlicensedRomFile=/private/authorized-fx991cnx.rom \
  -PromLicenseFile=/private/REDISTRIBUTION-LICENSE.txt
```

Gradle 仅把这两个文件复制到 build/generated 的 firmware 资源目录；缺少任一文件或 ROM 长度不为 256 KiB 会中止。普通构建会清空该目录，避免上次带固件构建污染后续无 ROM 包。应用首次启动检查这组资源，复制到私有存储后运行，后续优先使用已经导入的 ROM。提供文件不等于自动取得授权，发布者仍须核验。当前 CI 不传这些参数，并检查交付 APK 无 ROM；若今后授权内置，还须调整 CI 检查、来源声明和测试记录，不能直接把未授权文件加入源码。

## 0.5 点阵字库与不依赖 Android 的回归

字库上游为 https://github.com/TakWolf/fusion-pixel-font/releases/tag/2026.10.25 中的 `fusion-pixel-font-12px-monospaced-bdf-v2026.10.25.zip`。解压后使用 `fusion-pixel-12px-monospaced-zh_hans.bdf`，SHA-256 为 `bdb520006cddacccc1d3f780d5fab79007a4d6d1f3559d04665e652af4cdf50f`：

```sh
python tools/generate_lcd_font.py /path/to/fusion-pixel-12px-monospaced-zh_hans.bdf
# 仅当自行持有已支持的中文固件时，可运行以下原生/JVM 检查；不启动 Android：
tests/run-setup-integration.sh /private/fx991cnx.rom
```

生成器需要 Pillow 以及系统 DejaVuSansMono-Bold.ttf。字体完整 OFL 及上游归属见 `licenses/Fusion-Pixel/`，APK 同步附带。这些字形不是从 ROM 提取的。回归脚本在 git 忽略的 build/private-setup-check 内生成临时显存转储，验证原机四页识别、数字键透传、末页追加、主题、四档反馈和 DEL/AC，不将转储或 ROM 打入 APK。图标为透明 PNG 前景加透明自适应背景；无需联网或运行图片生成工具即可复现构建。

## 0.6 用户审定图标

使用用户选中的图 1 方形原稿，保留 C／%／= 和莫兰迪鼠尾草绿、米白、浅暖灰配色。直接替换 PNG，不预先剪圆角，不再使用旧蓝橙图标。`drawable/ic_launcher_foreground.xml` 四边各设 3% inset；在 108dp 自适应资源、常见 72dp 可见窗口中，原稿约 48% 的主体占比会显示为约 68%，使留白比合成预览适度缩小。`colors.xml` 提供配套的浅暖灰底色，系统决定最终裁切形状。原稿已提交，构建不需图片生成服务。此次只改资源与版本，不重跑计算器功能测试或 Android 运行测试。

## 独立仓库构建与发布

克隆 https://github.com/Antssos/CASIO_991_CNX 后，在仓库根目录执行上述 Gradle 命令。工作流监听 main 的 push 和 pull request；发布任务只发布 main 上构建成功的产物。原开发分支名称以同名分支保留，后续可通过 PR 合入 main。

迁移 0.6 时，发布工作流使用可选 original_apk_blob 与 original_apk_sha256 输入，从本仓库 Git 对象取回原已签名 APK 并校验 SHA-256；新仓库 CI 则独立构建和打包，二者在验证记录中区分。该迁移机制不处理密钥，也不重新签署原 APK；之后普通发布不传这两个输入。项目全部 app 源码、资源、Gradle/NDK 配置均保持与原 0.6 一致，仅目录位置、文档及 CI 地址适配新仓库。
