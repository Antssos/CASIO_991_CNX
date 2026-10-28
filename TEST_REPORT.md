# 测试报告 — 0.2.0-preview

本版本在原有合成固件测试之外，增加指定 fx-991CN X 原机固件的兼容性验收。**ROM 不在 APK、源码、Artifact 或 Release 中；安装后仍须一次性导入。** 没有把上游公开发行视为卡西欧授权证明。

## 样本与可复核方法

样本来源为用户指定的 CasioEmuNeo v0.5.0 完整发行包内 `release/fx991cnx/rom.bin`，长度 262144 字节。ROM SHA-256：

```text
8b6b6430f96858a8b3e765796e2d6f340bc7073ac9b0aa464775f15e53a10788
```

上游同时提供了 fx-991CN X 机型定义，硬件 ID 4、真实键盘矩阵。上述指纹用于识别已测试的具体固件，不把其他 X、ES 或 CW 固件误认为中文版。未在发行包中找到单独的 Casio 固件分发许可，因此不重分发 ROM，也不在用户应用中提供自动下载功能。

原生 Linux 测试使用与 APK 相同的 C++ 核心，脚本只发送物理键代码并读取 LCD VRAM。Android 测试类 `FirmwareAcceptanceTest` 则通过实际绘制按键的触摸坐标输入，不直接写数学结果。预期结果先人工核对，再以不含光标的 LCD 区域指纹自动断言；摘要和区域定义在 `app/src/androidTest/assets/rom-acceptance-goldens.json`，其中没有 ROM、字库或固件代码。

## 已执行的原生原机固件测试

| 操作 | 实际结果 |
|---|---|
| `10×0.25÷(10+2.5)` | LCD 显示 `1/5` |
| S⇔D | LCD 显示 `0.2` |
| SHIFT → MENU | 中文 SETUP，含输入/输出、角度单位、显示格式、工程符号 |
| ALPHA → A、DEL | 输入 A，DEL 删除 |
| `√2+3²`（含右方向键退出根式） | `9+√2` |
| 定义 2×2 MatA = `[[1,2],[3,4]]`，计算 MatA×2 | `[[2,4],[6,8]]` |
| 多项式模式，系数 1、−5、6 | 根 `x₁=3`、`x₂=2` |
| 5 → STO → A，清屏后 ALPHA A = | `5` |

这些结果均由原机 ROM 经 nX-U8 指令执行产生，未使用替代科学计算引擎。对应 Android 端到端验收的最终通过/失败，以同批 CI 的 `firmware-android-test.txt` 及设备截图为准；发布工作流只接受全部测试成功的构建。

## Android 自动验收

API 30（Android 11）x86_64 Emulator、Pixel 2、KVM。构建 arm64-v8a 与 x86_64，应用关闭 Wi-Fi、移动数据并设飞行模式运行。应用没有任何网络权限。

- `EmulatorTest`：无 ROM 引导、系统文件选择器、合成 ROM 导入、50 键中心热区、真实 KO/KI 到 LCD 链路、后台恢复、重开、横竖屏。
- `FirmwareAcceptanceTest`：已知 ROM 指纹识别、上述数学及中文菜单操作、实际 1/5 结果退出重开后保留、A=5 变量退出重开后保留。
- 独立 ADB 步骤：进程强制停止再启动、稳定横屏、1600×2560 / 240 dpi 平板截图、卸载测试应用后安装 x86_64 release APK 并启动。
- 实际截图通过设备截图接口和 `adb screencap` 保存。文件名 `12-real-one-fifth`、`14-real-decimal`、`15-real-chinese-setup`、`18-real-matrix-times-two`、`19-real-equation-root-three`、`20-real-equation-root-two`、`21-real-variable-restored` 分别对应验收项。

CI 在临时目录取样和校验 ROM，将其放入临时 Android 测试设备内部。没有 ROM 资产进入编译步骤；发布包不会包含测试设备内部文件、RAM 存档或固件文件。测试结束后临时设备销毁。

## 原生核心回归与打包检查

`tests/run-native.sh` 的 ASan/UBSan/LeakSanitizer 测试验证 CPU 指令、RAM/LCD 访问、49 个矩阵按键、10703 字节全状态往返和跨冷实例恢复。先前 0.1 版本的完整 Android 回归记录在 [Actions 40664197615](https://github.com/Antssos/cloudcodex/actions/runs/40664197615)，当前版本重新执行相同回归并增加原机测试。

发布 APK 必须通过签名检查、16 KiB 对齐检查、无权限声明检查，以及 ZIP 清单中不存在 ROM 的检查。SHA-256 以同批 `SHA256SUMS.txt` 为准，不与历史版本混用。

## 仍未验证或存在差异

- 没有 arm64 实机运行证据；原机固件 Android 验收使用 x86_64，同一套 C++/Java 源码。Android 10 / API 29 也尚无单独运行测试。
- 尚未逐项验收微积分、求解器、复数、向量、统计、分布、数制、表格及所有 SHIFT/ALPHA 组合。固件具备的菜单不等于这些模式全部经过测试。
- 只自动识别上述精确指纹；未知版本仍需人工确认并另行测试，不能承诺所有 ROM 都兼容。
- 面板是自行绘制，键帽轮廓、字体、次功能刻字、LCD 状态栏与原机仍有差异；没有使用授权不明的原机照片。
- tick 调度不是经硬件标定的周期精确实现；未完成长时间压力、低功耗和所有边界条件验收。
- 预览开发签名尚无稳定升级保证；不同 CI 构建可能需卸载后重装，卸载会删除导入 ROM 和状态。请保留自己的原始 ROM 文件。
- 本地没有 KVM，Android 验收来自 GitHub Actions；本地提供原生核心和原机 ROM 运算测试，不将本地软件虚拟化黑屏误报为 APK 启动成功。

## 安装与许可

APK 可直接安装，但不会开箱自动计算。首次点液晶屏，从您有权使用的文件中导入 fx-991CN X ROM；以后无需网络或再次选择文件。GPL-3.0 只覆盖模拟器和本项目代码，不自动覆盖 Casio 专有固件。
