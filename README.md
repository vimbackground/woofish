# ⏰ 正念 (woofish)

> **极简 · 禅意 · 专注**  
> 一款跨平台的电子木鱼与正念修行伴侣，帮你在快节奏的生活中找回内心的平静。

[![Release](https://img.shields.io/github/v/release/vimbackground/woofish?color=3388ff&label=%E6%9C%80%E6%96%B0%E7%89%88%E6%9C%AC)](https://github.com/vimbackground/woofish/releases/latest)
[![Platform](https://img.shields.io/badge/%E5%B9%B3%E5%8F%B0-Android%20%7C%20Windows-brightgreen)](https://github.com/vimbackground/woofish/releases/latest)
[![Size](https://img.shields.io/badge/Android%20%E4%BD%93%E7%A7%AF-1.2%20MB-blue)](https://github.com/vimbackground/woofish/releases/latest)
[![License](https://img.shields.io/badge/%E7%BA%AF%E7%B2%B9-%E6%97%A0%E5%B9%BF%E5%91%8A%20%7C%20%E6%97%A0%E8%BF%BD%E8%B8%AA-orange)](https://github.com/vimbackground/woofish)

---

## 🌿 什么是「正念」？

在忙碌喧嚣的日常与工作间隙，给心灵留出一片清净。

**正念 (woofish)** 是一款极简纯粹的正念与律动辅助工具，支持 **Android 手机** 与 **Windows 电脑**。无论是敲木鱼积攒功德、番茄钟深度专注、听音节拍律动，还是在白噪音中放松小憩，它都能为你提供温润、舒适且无打扰的陪伴。

---

## ✨ 核心功能

### 🐟 电子木鱼 · 凝神解压
- **真实原木音效**：精选禅韵木鱼、沉厚木鱼、清脆木鱼等多款高品质音效，触碰即响、清透温润。
- **真实敲击震感**：细腻的物理触觉震动反馈（支持 0~500ms 强度调节，手机放在桌上也能感知机械敲击感）。
- **自动念颂与节奏**：内置 5 档经典节奏档位（禅修、沉静、舒缓、诵经、精进），支持自由调速。
- **功德计数**：自动统计敲击次数，支持在主界面长按大字快速清零。

### 🍅 禅意番茄钟 · 深度工作与学习
- **极简黑白钟表**：纯白钟面与沉静指针，界面干净优雅。
- **轻柔秒针走针声**：专属治愈滴答声，伴随呼吸与时光流动（顶栏支持一键静音）。
- **专注零打扰**：倒计时进行中杜绝击打杂音与误触噪音。
- **一触即发**：首页直控预设时长（2分、5分、10分、15分、25分），轻触即刻开跑。
- **多阶段循环**：支持自定义专注与休息组合（如 `25+5` 工作法、`45+15` 深度工作）。

### ⏱️ 节拍器与手鼓 · 节奏随行
- **机械节拍器**：经典黑白往复摆杆，支持 30~300 BPM 任意精准调速。
- **真实木质手鼓**：真实录音室非洲手鼓原声采样，打击手感通透纯正。
- **智能测速**：支持指尖点击手动测速，以及环境音频实时识别测速。

### 🎧 沉浸与个性化定制
- **本地背景音乐**：可导入本地的轻音乐、雨声、流水白噪音，在应用内沉浸伴读伴修。
- **沉浸清屏模式**：一键隐藏计数器与繁杂控件，全屏仅留静心图腾，杜绝视觉干扰。
- **自定义文字**：支持在屏幕上展示“功德”、“正念”、“静心”等自定义寄语。

---

## 📥 下载与安装

请前往 [**GitHub Releases 最新发布页**](https://github.com/vimbackground/woofish/releases/latest) 下载对应平台的安装包：

| 平台 | 安装方式 | 特点 |
| :--- | :--- | :--- |
| 📱 **Android** | 下载 `woofish-x.x.x.apk` 直接安装 | **体积仅约 1.2 MB**，秒速下载安装，轻巧纯粹 |
| 💻 **Windows** | 下载 `woofish-x.x.x-windows-x64.zip` 解压使用 | **绿色免安装**，解压后双击 `woofish.exe` 即可启动，无需配置环境 |

---

## ⌨️ Windows 电脑快捷键

Windows 桌面版为办公摸鱼与键盘操作提供了贴心快捷键支持：

- **`空格键 (Space)`**：敲击木鱼 / 击打手鼓 / 启停番茄钟
- **`数字键 1 ~ 5`**：快速切换自动节奏档位
- **`回车键 (Enter)` / `退格键 (Backspace)`**：计数一键清零
- **`Z 键`**：一键切换沉浸清屏模式
- **`M 键`**：背景音乐开关

---

## 🛡️ 纯净声明

- **零广告**：没有任何开屏、弹窗或悬浮广告。
- **零追踪**：不收集任何个人隐私信息，无需注册登录。
- **零多余权限**：不申请敏感系统权限，所有数据与设置均仅保存在本地设备。

---

<details>
<summary>🛠️ 开发者指南 (技术栈与本地编译构建)</summary>

### 技术栈
- **语言**：Kotlin 2.0
- **跨平台 UI**：Compose Multiplatform (Material 3)
- **多模块架构**：`:shared` (共享 UI 与业务逻辑)、`:app` (Android 端)、`:desktop` (Windows 桌面端)
- **编译优化**：Google R8 代码优化与资源精简 (Android APK 仅 1.2 MB)

### 本地编译
在项目根目录下，双击运行对应脚本即可快速编译（要求已安装 JDK 17 及 Android SDK）：
- **单独编译 Android APK**：运行 `./build_apk.bat`，产物位于 `_Dist/android/`
- **双端全量编译**：运行 `./build_local.bat`，产物位于 `_Dist/`

</details>

---

## 📌 致谢

本项目最初灵感来自开源项目 [WoodenFish (by Ares-Chang)](https://github.com/ares-chang/wooden-fish)，在此对原作者表示衷心感谢！
