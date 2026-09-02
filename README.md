# 🐟 正念木鱼 (woofish)

基于 **Kotlin + Jetpack Compose + Material 3** 构建的现代化、极简禅意风安卓电子木鱼应用。

---

## 📌 项目来源与致谢

本项目重构自开源项目：[**WoodenFish (by Ares-Chang)**](https://github.com/ares-chang/wooden-fish)。

原项目采用 SolidJS + Vite 开发，现已完全重构为基于 Kotlin + Jetpack Compose 的 Android 原生应用，并针对移动端手感、沉浸清屏体验和极简交互进行了全面升级。

---

## ✨ 核心特性

- 🎯 **极致纯净**：桌面应用名称为“**正念木鱼**”，无广告、无冗余外链、无任何操作提示，保留最纯粹的修行体验。
- 🎨 **全新黑白极简图标**：重新设计了纯黑底色 + 小巧白木鱼的 Android Adaptive Icon 自适应图标。
- 📱 **全屏敲击模式**：在调节设置中支持开启全屏点击，手指点击屏幕任意区域均可正常敲击木鱼。
- 👁️ **沉浸清屏模式**：
  - 点击右上角【清屏】后，仅隐藏功德数字与文字，顶栏所有按钮（音效、动效、BGM、设置等）均完整保留，随心操控。
- 🧭 **人体工学顶栏布局**：
  - 左侧：`[🎵 BGM]` + `[✨ 动效]`
  - 右侧：`[🔊 音效 1/2]` + `[👁️ 清屏]` + `[⚙️ 设置]`（设置固定位于最右侧）
- ✨ **真实物理打击动效**：
  - 模拟真实受力下压、挤压形变（拉伸 X，压缩 Y）与微倾斜，配合高阻尼弹簧回弹。
  - 伴随原生马达轻微触觉震动反馈，支持随时一键开启/关闭。
- ⚙️ **调节与统计清零**：右上角调节弹窗支持自由调整 BGM 音量、全屏点击开关以及一键清零已累积功德。

---

## 🛠️ 技术栈

- **应用名称**：`正念木鱼` (woofish)
- **语言**：Kotlin 2.0
- **UI 框架**：Jetpack Compose (Material 3)
- **音频引擎**：`SoundPool`（零延迟敲击） + `MediaPlayer`（BGM 循环）
- **触觉反馈**：`Vibrator` / `VibrationEffect`
- **CI/CD 自动化**：GitHub Actions 自动构建与发布 Release

---

## 📲 发布与下载说明

### 1. 自动打 Tag 发布新版本
只需在本地创建并推送 Tag，GitHub Actions 就会自动编译并发布到 Releases：
```bash
git tag v0.2.1
git push origin v0.2.1
```

### 2. 下载安装包
在 GitHub 仓库主页右侧 **Releases** 栏目直接下载发布的 **`woofish-v0.2.1.apk`** 安装包。
