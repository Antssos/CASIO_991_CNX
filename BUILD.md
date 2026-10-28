# 构建

需要 JDK 17、Android SDK 35、Build Tools 35.0.0、NDK 27.2.12479018 和 CMake 3.22.1。

```sh
sdkmanager 'platforms;android-35' 'build-tools;35.0.0' 'ndk;27.2.12479018' 'cmake;3.22.1'
keytool -genkeypair -keystore /path/to/release.keystore -storepass android \
  -alias androiddebugkey -keypass android -dname 'CN=Android Debug,O=Android,C=US' \
  -keyalg RSA -keysize 2048 -validity 10000
export ANDROID_HOME=/path/to/android-sdk
export FX_KEYSTORE=/path/to/release.keystore
./gradlew :app:assembleRelease
```

输出：`app/build/outputs/apk/release/fx991cnx-arm64-release.apk`。

签名参数：`FX_KEYSTORE`、`FX_STORE_PASSWORD`、`FX_KEY_ALIAS`、`FX_KEY_PASSWORD`，后三项默认分别为 `android`、`androiddebugkey`、`android`。

可选内置固件：同时传入 `-PlicensedRomFile=/path/to/firmware.rom` 和 `-PromLicenseFile=/path/to/license.txt`。

## 测试与素材

```sh
tests/run-native.sh
tests/run-setup-integration.sh /path/to/fx991cnx.rom
python tools/generate_skins.py
python tools/generate_lcd_font.py /path/to/fusion-pixel-12px-monospaced-zh_hans.bdf
```

原生测试需要 C++17 编译器，菜单测试另需 JDK 17。素材工具使用 fontTools、Pillow、DejaVu 和 Noto CJK 系统字体；点阵字库输入使用 [Fusion Pixel 12px，2026.10.25](https://github.com/TakWolf/fusion-pixel-font/releases/tag/2026.10.25) 的简体中文 BDF。

## CI 与发布

`main` 提交和 PR 自动构建 APK，检查签名与对齐，并上传 APK、源码和校验值。发布前更新 `app/build.gradle` 的版本号，再手动运行 `Publish APK`，填写成功的构建任务 ID。
