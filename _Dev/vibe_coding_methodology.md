# 💡 Vibe Coding 核心方法论与工程策略沉淀

本文档沉淀了在人机结对编程（Vibe Coding）实战中提炼出的**通用工程方法论**。无论开发何种语言、框架或平台的项目，均可直接参考这些核心原则。

---

## 🏛️ 原则一：零跳动 UI 布局设计法则 (Zero-Shift Layout Principle)

### 现象与痛点
在组件展开/折叠、进入全屏/清屏或显示隐藏次要按钮时，如果容器高度为 `wrap_content` 或动画伴随纵向高度伸缩，会导致父容器或邻近图标发生“上下颠簸/位移跳动”，极大破坏用户的交互质感。

### 核心方法论
1. **固定基准高宽**：所有顶栏（TopBar）、底栏（BottomBar）等关键导航区域，必须显式固定高度（如 `height(60.dp)` 或 `h-14`），并采用居中对齐（`CenterVertically`）。
2. **切断纵向干扰**：
   - 局部按钮的显隐必须使用**纯水平伸缩（`expandHorizontally`）**或**纯透明度渐变（`fadeIn`/`fadeOut`）**。
   - 严禁使用默认的双向尺寸伸缩动画（`expandIn`/`shrinkOut`）作用于行内单项。
3. **锚点恒定原则**：在多种模式切换时，相同功能的按钮在屏幕上的物理坐标必须保持 100% 恒定，利用用户的肌肉记忆减少认知负担。

---

## ⚡ 原则二：平台底层强制约束的提前避坑法则 (Platform Invariants)

### 现象与痛点
AI 在重命名包名、模块路径或目录时，往往容易使用单单词或非标准路径，触发底层编译工具链（如 Android Manifest Merger、iOS Bundle Identifier、Docker Image Tag）的隐式硬性校验。

### 核心方法论
1. **命名空间段落规范**：在任何需要标识符的地方（如 Android `applicationId`、Java Package、Domain Reverse），必须确保**至少包含一个英文点号（`.`）**（如 `com.appname`）。
2. **目录深度适度原则**：在满足平台标准的前提下，尽量扁平化目录结构（如从 4 层包名精简为 2 层 `com/appname/`），既满足平台编译器对包名的硬性要求，又使项目目录清晰可读。

---

## 🔄 原则三：零延迟、单向数据流与持久化法则 (State Flow & Persistence)

### 核心方法论
1. **单一真实数据源 (SSOT)**：所有 UI 呈现严格由单一 ViewModel / Store 中的 StateFlow 驱动。
2. **内存直读 + 异步落盘**：
   - 内存状态即时响应（保证 0 延迟，如点击敲击立即触发音频与形变）。
   - 磁盘持久化（SharedPreferences / DataStore / LocalStorage）跟随状态变更自动同步，不在 UI 主线程阻塞任何 IO 操作。
3. **音频引擎分级策略**：
   - 高频并发短音效（点击/敲击）：使用内存驻留的低延迟音频池（如 Android `SoundPool` / Web Audio API）。
   - 长音频/背景音乐（BGM）：使用媒体播放器（如 `MediaPlayer` / `HTML5 Audio`），支持音量衰减与平滑循环。

---

## 🚀 原则四：零摩擦自动化发布流水线法则 (Zero-Friction Release Pipeline)

### 现象与痛点
手动编译打包、下载、重命名、创建 Release 上传是一项繁琐且易出错的过程，且 GitHub 默认的 Release 经常生成杂乱的 git diff 比较文本。

### 核心方法论
1. **每次推送皆可发布**：配置 CI/CD 流水线，使得无论推送代码还是打 Tag，均自动执行编译、重命名规整安装包（如 `appname-x.y.z.apk`）并自动创建/更新 GitHub Release。
2. **结构化发布日志 (Release Notes)**：在工作流中接入专门的 `RELEASE_NOTES.md`，彻底屏蔽默认的 `Full Changelog: [v0.1...v0.2]` 泛化无用信息，为用户呈现结构化的更新说明。
3. **权限预置**：确保 CI/CD 具有 `contents: write` 权限，让自动化流程自闭环。

---

## 📝 原则五：文档与代码强一致同步守则 (Doc-Code Sync)

### 核心方法论
- **代码变，文档动**：任何涉及功能变更、按键重排、版本升级的操作，必须在**同一个 Commit 中同步更新 `README.md` 与相关的 Release 说明**。
- **永久约束机制**：将关键开发准则写入本地配置文件或 Agent 指南，确保 AI Agent 长期保持行为一致性。
