# 第三方代码、来源与授权

## CasioEmuNeo — GPL-3.0

- https://github.com/qiufuyu123/CasioEmuNeo
- 研究/移植基准：`23de3431a5b17f2e0570385455e2698c7463a47c`
- 复用 `emulator/Chipset`、`emulator/Peripheral`、`HardwareId` 和日志接口。
- 上游模拟核心源自 CasioEmu 社区；版权归原作者与贡献者所有。原始 GPL 全文保留在项目 LICENSE 和 APK assets/LICENSE。
- 本项目修改：去除 Lua 调试接口、断点及桌面界面；改为 Android JNI；键盘仅替换事件与配置入口，保留原机 KO/KI 扫描及 ghosting 逻辑；替换 SDL LCD 渲染层；新增完整状态序列化、确定性初始化、错误边界、Android 生命周期和原生面板。新代码与修改同样按 GPL-3.0 提供。

## CasioEmuMsvc — GPL-3.0（研究参考）

- https://github.com/telecomadm1145/CasioEmuMsvc
- 研究基准：`ed094b7f6fb84e75f4edccb43f5f82a86b60d8dc`
- 参考其 Android Gradle/NDK 接入方法；未打包其调试 UI、在线服务、Sentry、插件、素材或固件。
- Gradle wrapper JAR 引导文件取自该仓库。Gradle 原作者版权、Apache-2.0，许可证见 licenses/Gradle-Apache-2.0.txt。

## 外观与硬件资料

- 用户指定 https://calculator.weo.asia/app/ 为外观参考。研究公开面板、按键排列及 `model/model.lua` 的矩阵参数；未复制网站脚本、图片、WASM、字体或 ROM 到 APK。
- 按键坐标、矩阵代码、寄存器地址是兼容所需的硬件事实；界面和状态栏由本项目代码绘制，使用 Android 系统字体。
- fx-991CN X、ClassWiz、Casio 是相应权利人的名称和商标。本项目为独立兼容实现，非官方产品，不表示获得 Casio 认可。
- 未发现允许重分发专有 Casio ROM 的许可，发布包不含 ROM；用户需自行合法持有并导入。不将网上可下载等同于获准再分发。

## 原创测试固件

`tests/native_test.cpp` 与 `app/src/androidTest/.../EmulatorTest.java` 内的合成 nX-U8 机器码是本项目原创，按 GPL-3.0 提供。它们不含原机算法、字库或数据，也不用于冒充数学功能验收。


## 0.2 固件兼容性样本

使用 CasioEmuNeo v0.5.0 完整发行包中的 fx991cnx ROM 做临时兼容性测试。其公开可下载不被解释为取得 Casio 再分发授权，GPL-3.0 不自动覆盖该 ROM；不随源码、APK、测试 APK、Artifact 或 Release 提供。源码中仅记录长度、SHA-256、操作序列和显示区域摘要，测试截图为实际执行结果。

## 0.3 面板与设置

- 四套面板为项目代码绘制，以用户给出的照片作为外观参考，不直接复制、裁切或打包照片像素，也不包含预制的原机菜单画面。
- AndroidSVG 1.4，Paul LeBeau / BigBadaboom，Apache-2.0：https://github.com/BigBadaboom/androidsvg 。仅解析 APK 内置 SVG 并绘制到原生 Canvas，不访问网络。
- 部分轮廓由 DejaVu Sans / DejaVu Sans Condensed / DejaVu Serif Italic 和 Noto Sans CJK SC 字形转换而来，字体原作者版权与许可分别保留在 `licenses/DejaVu-copyright-and-license.txt`、`licenses/Noto-CJK-copyright-and-OFL.txt`。只使用必要字符的图形轮廓，未打包完整字体；对应许可也存放在 APK 的 assets/licenses 中。
- `key_click.wav` 是本项目用数学波形生成的短音效，GPL-3.0；没有外部录音或音频素材。
