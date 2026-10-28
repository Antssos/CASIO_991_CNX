# fx-991CN X Emulator

Android 10+ 离线原机模拟器的 **0.1.0 预览版**。采用 CasioEmuNeo 的 nX-U8 C++ 核心、NDK/JNI 和原生 Android 面板，不含普通科学计算引擎、WebView、广告或网络权限。

**本版本没有内置 Casio ROM，也尚未完成原机固件的计算功能验收，不能视为已验收的日常计算器。** 已实现真实 CPU、键盘扫描、LCD 和全机状态存取；自编测试固件通过不代表原机分数、矩阵、方程等功能通过。具体证据与限制见 [TEST_REPORT.md](TEST_REPORT.md)。

## 下载与安装

在本仓库 [fx991cnx Android APK 工作流](https://github.com/Antssos/cloudcodex/actions/workflows/fx991cnx-android.yml) 的成功运行中下载 `fx991cnx-arm64-release` Artifact，解压后安装 `fx991cnx-arm64-release.apk`。无需 Android Studio。若有 GitHub Release，可直接下载 APK；Artifact 下载需要登录 GitHub。

1. 手机需 Android 10 或以上、arm64 CPU。允许文件管理器“安装未知应用”。
2. 对照同批 `SHA256SUMS.txt` 校验 APK，点击安装。
3. 首次启动点液晶屏，使用系统文件选择器导入您有权使用的 **fx-991CN X 完整 256 KiB 原机 ROM**。不支持 ES、EX、CW、模拟器专用 ROM、ZIP 或 `.mdl` 包。
4. 导入保存在应用内部，之后不再选择文件。应用本身没有下载 ROM 的功能，不提供未授权固件获取链接。
5. 长按顶部型号可重新导入、清除计算器状态或阅读 GPL 许可证。

导入器会检查长度、向量和可识别的文本标识。**没有经过授权验证的 ROM 指纹数据库，无法自动、可靠地辨别全部版本**；无型号标识时要求您确认候选 ROM 的来源和机型。错误机型不会被转换为中文版。

## 使用

实体按键直接连接原机键盘矩阵；SHIFT、ALPHA、MENU/SETUP、OPTN、CALC、方向键、AC、DEL、S⇔D 均由固件解释。支持按住按键，触摸反馈和连续输入；短触摸会保留至少 64 ms 以覆盖扫描周期。不同时按多键，第二根手指会取消当前按键。

LCD 主显示区域为 192×63，另绘状态栏。竖屏、横屏和平板按比例显示整台面板。退出、转到后台时保存；前台每约 5 秒自动保存。保存内容包含 CPU、RAM、LCD、键盘、计时器、中断和其他 X 系列外设，按 ROM SHA-256 隔离，并用 CRC 校验及原子替换保存。卸载或清除应用数据会删除 ROM 和状态；突然断电可能丢失最近几秒输入。

面板为自行绘制，非授权 Casio 产品图片。键帽形状、字体、次功能文字、状态图标和机身比例仍有原机差异，未宣称像素级复刻。没有隐藏的替代数学引擎：未导入可运行固件时无法计算。

## 许可与源码

项目以 GPL-3.0 发布。与 APK 对应的完整源代码、CMake、Gradle 和 CI 脚本随 Artifact 的 `fx991cnx-source.zip` 提供，也在 `feat/fx991cnx-android` 分支下 `fx991cnx-android/` 目录中。允许安装自行修改和签名的版本。第三方归属见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)，构建见 [BUILD.md](BUILD.md)。

预览版使用开发签名，签名私钥不公开。CI 未配置固定发布签名时每次生成临时开发签名；不同构建间可能需卸载后安装，卸载前请保留您自己的 ROM。尚无生产版签名与升级保证。
