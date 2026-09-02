# 📖 正念木鱼 (woofish) 实战案例与避坑复盘集

本文档记录了 `woofish` 项目从零重构、演进到发布过程中的典型实战案例与排错复盘，供开发者与 Agent 参考学习。

---

## 案例一：Web 项目重构为 Android 原生的跨端架构选型

### 背景
原开源项目是 SolidJS + Vite + Howler.js 的 Web 应用，用户希望将其重构成轻量、纯净的 Android 应用。

### 决策过程
- **方案 A：Capacitor 壳** $\to$ 优点是快，但缺点是高频连击木鱼时 WebView 音频存在微秒级延迟，且 APK 体积较大。
- **方案 B：原生 Jetpack Compose + SoundPool** $\to$ 决定采用。代码量仅约 300 行，利用 `SoundPool` 将音频预加载至内存，实现毫秒级零延迟重叠敲击，配合系统 `Vibrator` 提供触感反馈，体验远超 Web 版。

---

## 案例二：包名精简导致 Android Manifest Merger 编译失败

### 错误现象
用户希望简化深层目录 `com/areschang/woodenfish`，重构成短目录 `woofish`，导致 GitHub Actions 编译报错：
```
Package name 'woofish' at position AndroidManifest.xml should contain at least one '.' (dot) character
```

### 根本原因
Android 系统的包名规范强制要求至少包含一个点号（`.`），不能为单单词。

### 优雅解法
将命名空间与包名调整为标准的 **`com.woofish`**，磁盘物理目录为两层 `app/src/main/java/com/woofish/`，既满足 Android 编译器的硬性规范，又做到了最极致的扁平整洁。

---

## 案例三：清屏切换时的顶栏图标纵向颠簸修复

### 错误现象
点击【进入清屏】时，顶部图标向上跳动；【退出清屏】时，顶部图标向下跳动。

### 根本原因
Compose 的 `AnimatedVisibility` 默认会同时伸缩组件的宽高（`expandIn` / `shrinkOut`）。在折叠右侧次要按钮时，顶栏的总体测量高度发生动态变化，导致整行图标产生 Y 轴位移颠簸。

### 优雅解法
1. 顶栏 `Row` 显式固定高度为 `60.dp`，并设置 `Alignment.CenterVertically`。
2. 局部按钮折叠显隐改用 `expandHorizontally(expandFrom = Alignment.End)` + `fadeOut()`，彻底切断对高度维度的干扰。
3. 功德计数字域改用纯透明度渐变（`fadeIn` / `fadeOut`）。

---

## 案例四：GitHub Releases 无发布产物与泛化 Changelog 优化

### 错误现象
1. 编译成功但 Releases 页面没有生成对应 APK。
2. GitHub 默认的 Release 说明是一串毫无意义的 commit diff 比较链接（`Full Changelog: [v0.2...v0.3]`）。

### 根本原因
1. 工作流仅在推送 Git Tag 时才执行 Release 创建，分支普通 push 跳过了该步骤。
2. 未向 `softprops/action-gh-release` 提供结构化的 `body` / `body_path` 参数。

### 优雅解法
1. 优化工作流，无论分支推送还是 Tag 推送，均全自动生成/覆盖 Release。
2. 引入专用的 `RELEASE_NOTES.md`，输出详实的中文功能升级日志，禁用默认的泛化生成。
