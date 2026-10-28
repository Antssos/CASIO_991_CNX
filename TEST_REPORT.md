# 测试报告 — 0.1.0-preview

本报告严格区分“原生核心/原创诊断固件测试”与“Casio 原机固件功能验收”。尚未持有可用于本项目验收的合法 fx-991CN X ROM，**原机功能验收未通过（未执行），不是完整交付终点**。

## 已执行

2026-10-28，Linux g++、AddressSanitizer/UndefinedBehaviorSanitizer/LeakSanitizer：

- 原创 nX-U8 指令执行：MOV、ADD、ST、循环跳转；结果真实写入 RAM 和 LCD VRAM。
- 49 个实体矩阵按键逐个校验 KO/KI active-low 线路，包括 SHIFT、ALPHA、MENU、方向、AC、DEL；另有 ON 重置键。
- CPU、RAM、LCD、键盘、中断、计时器等全状态保存、修改、还原后字节一致；冷实例载入后 RAM 一致。状态有效载荷 10703 字节。
- 以上测试通过，未发现 sanitizer 错误。最初沙箱内 LeakSanitizer 被 ptrace 限制，正常执行环境重跑通过。

## Android 实际运行证据

[GitHub Actions 40662774487](https://github.com/Antssos/cloudcodex/actions/runs/40662774487) 全部成功。测试提交 `e8f3652b4b545ff537552b3dff55eacdb9dbe5ef`；后续文档提交没有修改生产应用代码。

- Android Emulator API 30（Android 11）、x86_64、Pixel 2，KVM 加速；断开 Wi-Fi/数据并设飞行模式后测试。
- 一个集成测试方法覆盖多组断言，**1 test、0 failures、0 skipped**。生成真实设备截图，没有使用静态示意图代替运行截图。
- 系统文件选择器打开/返回、导入结果处理、长度/向量检查及候选机型确认通过。测试自动调用选择器结果回调传入本地合成文件 URI；未自动点击选择器中的真实用户 ROM 文件。
- 原创测试 ROM 由真实 MCU 绘制 `TEST ROM`，启动、50 键视觉中心热区及触摸序列通过。按住数字 1 后 MCU 的扫描结果确实写入 LCD VRAM 为 `0xFE`，松手为 `0xFF`；验证的是硬件输入链路，不是 Casio 数学语义。
- 后台后返回并再次触摸扫描通过；结束 Activity 并重新打开可恢复运行。另用 `am force-stop` / `am start -W` 启动成功并保存截图。原生测试同时验证全状态跨冷实例还原。
- 竖屏与 1600×2560 / 240 dpi 平板截图已核对，整台面板可见，无系统栏遮挡。首轮横屏截图捕获了旋转过渡帧，已补充布局稳定断言与独立 ADB 截图，最终横屏证据以发布包中的 `09-landscape-stable.png` 为准。
- 卸载测试应用后，**实际安装 x86_64 release APK** 成功，启动返回 `Status: ok`，显示明确的无 ROM 导入引导。
- arm64 release 已真实构建，未在 arm64 实机或 arm64 Android Emulator 执行；不能把 x86_64 测试说成 arm64 实机验收。Android 10 / API 29 也没有独立运行测试。
- `apksigner verify` 通过，APK Signature Scheme v2 有效；`zipalign -c -P 16 4` 通过。`aapt dump permissions` 没有任何权限声明。检查 APK assets 只有 LICENSE，没有 ROM。

实际截图：[release 安装启动](docs/evidence/release-installed.png)、[原创测试固件](docs/evidence/synthetic-rom.png)、[平板](docs/evidence/tablet.png)。Artifact 中另有文件选择器、型号确认、按住数字键、状态重开和进程重启截图。

本地没有 KVM，软件虚拟化未能稳定启动 Android，因此 Android 运行验收来自上述 CI，而不是本地黑屏实例。前期 CI 暴露的导入界面同步、固定按键等待和返回已有 Activity 的等待问题已修复；最终整套测试重新执行成功。

## 原机计算验收矩阵

| 验收项 | 状态 | 原因/说明 |
|---|---|---|
| `10×0.25÷(10+2.5)` → `1/5` | 未测试 | 缺少合法原机固件 |
| S⇔D 分数/小数、根式、复杂输入 | 未测试 | 同上 |
| 中文 MENU/SETUP、SHIFT/ALPHA 功能含义 | 未测试 | 已验证物理线路，不等于已验证固件菜单 |
| 微积分、数值求解 | 未测试 | 同上 |
| 矩阵、向量、复数、方程 | 未测试 | 同上 |
| 统计、分布、数制、表格、其他模式 | 未测试 | 同上 |
| 原机 Ans、变量、历史恢复 | 未测试 | 合成固件状态测试不能替代原机验收 |

## 已知限制与原机差异

- 没有 ROM 时只有明确的导入引导，不能计算。导入流程只支持 256 KiB 完整原机镜像；不支持模拟器专用镜像。
- 机型自动识别不完整：只能检查格式与文本标识，未知固件需用户确认；没有可靠授权样本指纹库。
- 自绘面板接近 CN X 的 50 键排列，但不是原机照片；键帽轮廓、字形、部分次功能刻字和 LCD 状态图标尚未逐项完成原机校对。
- 主显示矩阵为真实 192×63；状态栏采用 Android 字体，不是原机图形。
- CPU 以 2 MHz 指令 tick 调度；没有实测硬件时序标定，不能保证所有低功耗、定时或硬件细节一致。
- 不同 ROM、不同核心存档版本不混用；CRC/长度损坏时冷启动。尚未完成真实 ROM 长时间压力测试。
- 开发签名预览 APK，未提供稳定升级签名；完整屏幕阅读器逐键导航、ROM 导出和状态备份还未实现。

## 原机固件验收的复测步骤

使用有权使用的 fx-991CN X ROM 导入后，在 Android Emulator 和 arm64 实机上执行：

1. 初始数学输入/数学输出下按 `1 0 × 0 . 2 5 ÷ ( 1 0 + 2 . 5 ) =`，截屏必须为 `1/5`；按 S⇔D 验证小数转换。
2. 测 SHIFT→MENU 中文 SETUP、ALPHA→变量、方向历史、AC/DEL、长按和连续按键。
3. 中文菜单进入矩阵/方程，完成实际运算；测试根式和复杂表达式。
4. 输入未完成表达式、变量/历史后依次回桌面、退出、强制结束进程、重开；检查状态。
5. 开飞行模式并关闭 Wi-Fi/移动网络，全程重复以上步骤。
6. 保存实际截图、ROM SHA-256（不发布 ROM）和设备/API 信息后才可标记验收完成。
