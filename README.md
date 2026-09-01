# 🐟 电子木鱼 (Wooden Fish Android)

基于 **Kotlin + Jetpack Compose + Material 3** 构建的现代化、极简禅意风安卓电子木鱼应用。

---

## ✨ 功能特性

- 🎯 **极致纯净**：无广告、无冗余外链、无任何操作提示，保留最纯粹的修行体验。
- 🔊 **多音效一键切换**：屏幕左上角提供【音效 1】与【音效 2】快速切换。
- 🎵 **沉浸模式 (BGM)**：屏幕左上角可一键开启/暂停空灵背景音乐。
- 👁️ **清屏模式**：屏幕右上角一键隐藏除木鱼以外的所有元素（计数与操作栏全隐藏）。
- ✨ **真实物理打击动效**：
  - 模拟真实受力下压、挤压形变（拉伸 X，压缩 Y）与微倾斜，配合高阻尼弹簧回弹。
  - 伴随原生马达轻微触觉震动反馈，支持在右上角一键开启/关闭。
- ⚙️ **调节与统计清零**：右上角调节弹窗支持自由调整 BGM 音量以及一键清零已累积功德。

---

## 🛠️ 技术栈

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
