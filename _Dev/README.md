# 🛠️ _Dev: Vibe Coding 方法论与跨项目 Agent 复用工具包

欢迎使用 `_Dev` 工具包！本项目沉淀了一套**高度可复用、跨技术栈通用**的 AI Agent 结对编程（Vibe Coding）方法论、开发规范与提示工程 SOP。

---

## 📁 目录内容导航

1. **[`README.md`](README.md)**（本文件）：工具包使用方案与开发者进阶指南。
2. **[`agent_sop_prompt.md`](agent_sop_prompt.md)**：**开箱即用的 Agent 引导提示词 SOP**（在新项目中直接复制该文本发送给 Agent 即可完成项目对齐与工程约束）。
3. **[`vibe_coding_methodology.md`](vibe_coding_methodology.md)**：**通用 Vibe Coding 核心方法论与策略**（包含零跳动 UI 布局、状态单向流、平台底层约束、CI/CD 自动化等可复用模式）。
4. **[`woofish_case_study.md`](woofish_case_study.md)**：**经典案例复盘**（记录了从 Web 重构到 Android 原生、解决清单合并报错、布局颠簸优化、自动构建发布的实战经验）。

---

## 🚀 跨项目快速迁移与使用方案（3步走）

当你准备开启一个新的项目（无论是 Android、Flutter、Web、后端还是全栈）时，请按以下步骤操作：

### 第一步：复制工具包
将本 `_Dev` 目录直接整体复制到新项目的根目录下：
```
your-new-project/
├── _Dev/                     <-- 复制到此处
│   ├── README.md
│   ├── agent_sop_prompt.md
│   ├── vibe_coding_methodology.md
│   └── woofish_case_study.md
└── ... (新项目的其他代码)
```

### 第二步：唤醒与初始化 Agent
打开你的 AI 编程助手（如 Antigravity / Cursor / Claude 等），将 [`agent_sop_prompt.md`](agent_sop_prompt.md) 中的【**新项目初始化提示词**】直接复制并发送给 Agent。

### 第三步：持续复盘与资产累加
在每一轮功能开发完成后，让 Agent 总结本轮新增的通用经验并追加到 `vibe_coding_methodology.md` 中，让你的知识资产随项目迭代持续复利增值！
