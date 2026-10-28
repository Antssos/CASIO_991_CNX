# 构建

需要 JDK 17、Android SDK 35、Build Tools 35.0.0、NDK 27.2.12479018 和 CMake 3.22.1。

在仓库根目录执行：

```sh
sdkmanager 'platforms;android-35' 'build-tools;35.0.0' 'ndk;27.2.12479018' 'cmake;3.22.1'
keytool -genkeypair -keystore /path/to/release.keystore -storepass android \
  -alias androiddebugkey -keypass android -dname 'CN=Android Debug,O=Android,C=US' \
  -keyalg RSA -keysize 2048 -validity 10000
export ANDROID_HOME=/path/to/android-sdk
export FX_KEYSTORE=/path/to/release.keystore
./gradlew :app:assembleRelease
```

Windows 使用 `gradlew.bat :app:assembleRelease`。

输出：`app/build/outputs/apk/release/fx991cnx-arm64-release.apk`。

签名参数：`FX_KEYSTORE`、`FX_STORE_PASSWORD`、`FX_KEY_ALIAS`、`FX_KEY_PASSWORD`，后三项默认分别为 `android`、`androiddebugkey`、`android`。

内置固件构建：同时传入 `-PlicensedRomFile=/path/to/firmware.rom` 和 `-PromLicenseFile=/path/to/grant.pdf`。授权文件仅在构建时读取，源码包和 APK 均不包含该文件。

## 测试

在仓库根目录执行 `tests/run-native.sh` 或 `tests/run-setup-integration.sh /path/to/fx991cnx.rom`。需要 C++17 编译器和 JDK 17。

## 素材

在仓库根目录运行 `python tools/generate_skins.py` 或 `python tools/generate_lcd_font.py /path/to/font.bdf`。

工具使用 fontTools、Pillow、DejaVu 和 Noto CJK 系统字体；点阵字库输入使用 [Fusion Pixel 12px，2026.10.25](https://github.com/TakWolf/fusion-pixel-font/releases/tag/2026.10.25) 的简体中文 BDF。

## CI 与发布

`main` 提交和 PR 验证源码构建，上传 APK、源码和校验值。内置固件版在本地签名构建；发布前更新版本号，运行 `Publish APK`，填写成功的构建任务 ID、已签名 APK 的 Git blob SHA 和文件 SHA-256。发布任务校验版本、固件和签名后上传内置版 Artifact 与 Release。
