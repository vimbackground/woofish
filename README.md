# 🐟 woofish (电子木鱼)

基于 **Kotlin + Jetpack Compose + Material 3** 构建的现代化、极简禅意风安卓电子木鱼应用。

---

## 📌 项目来源与致谢

本项目重构自开源项目：[**WoodenFish (by Ares-Chang)**](https://github.com/ares-chang/wooden-fish)。

原项目采用 SolidJS + Vite 开发，现已完全重构为基于 Kotlin + Jetpack Compose 的 Android 原生应用，并针对移动端手感、沉浸清屏体验和极简交互进行了全面升级。

---

## ✨ 核心特性

- 🎯 **极致纯净**：无广告、无冗余外链、无任何操作提示，保留最纯粹的修行体验。
- 🎨 **全新黑金质感图标**：重新设计了符合 Android Adaptive Icon 标准的黑金禅意自适应图标。
- 👁️ **沉浸清屏模式**：
  - 点击右上角【清屏】后，隐藏功德数字与所有文字，仅留居中木鱼本体。
  - **清屏状态下依然支持**：自由开关背景音乐（BGM）和开启/关闭木鱼物理打击动效。
- 🔊 **多音效一键切换**：屏幕左上角提供【音效 1】与【音效 2】快速切换。
- 🎵 **沉浸模式 (BGM)**：屏幕左上角可一键开启/暂停空灵背景音乐。
- ✨ **真实物理打击动效**：
  - 模拟真实受力下压、挤压形变（拉伸 X，压缩 Y）与微倾斜，配合高阻尼弹簧回弹。
  - 伴随原生马达轻微触觉震动反馈，支持在右上角一键开启/关闭。
- ⚙️ **调节与统计清零**：右上角调节弹窗支持自由调整 BGM 音量以及一键清零已累积功德。

---

## 🛠️ 技术栈

- **项目名称**：`woofish`
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
git tag v0.2.0
git push origin v0.2.0
```

### 2. 下载安装包
在 GitHub 仓库主页右侧 **Releases** 栏目直接下载发布的 **`woofish-v0.2.0.apk`** 安装包。
